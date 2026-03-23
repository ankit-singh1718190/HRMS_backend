package com.example.hrmsclient.config;

import com.example.hrmsclient.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC — No authentication required
    // ─────────────────────────────────────────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
        "/api/auth/login",            // Login for all roles
        "/api/auth/forgot-password",  // Password reset request
        "/api/auth/reset-password",   // Password reset confirm
        "/actuator/health",           // Health check
        "/actuator/health/**",
        "/actuator/info"
    };

    private static final String[] ADMIN_ONLY_URLS = {
        "/api/admin/**",
        "/api/dashboard/admin-stats",
        "/api/emails/**"
    };

 

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT is stateless — CSRF not needed
            .csrf(csrf -> csrf.disable())

            // Wire CORS bean defined below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // No HTTP sessions — every request carries JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── 1. PUBLIC ──────────────────────────────────────────────
                .requestMatchers(PUBLIC_URLS).permitAll()

                // ── 2. ADMIN ONLY ──────────────────────────────────────────
                .requestMatchers(ADMIN_ONLY_URLS)
                    .hasRole("ADMIN")

                // ── 3. ADMIN + HR: Employee CRUD ───────────────────────────
                .requestMatchers(
                    HttpMethod.POST,   "/api/employee/register")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "HR")

                .requestMatchers(HttpMethod.GET,
                    "/api/employee",
                    "/api/employee/search",
                    "/api/employee/filter/**",
                    "/api/employee/active",
                    "/api/employee/exited",
                    "/api/employee/departments",
                    "/api/employee/dashboard")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "HR", "MANAGER")

                .requestMatchers(
                    HttpMethod.PUT,    "/api/employee/{id}")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.PUT,    "/api/employee/{id}/exit")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.DELETE, "/api/employee/{id}")
                    .hasAnyRole("ADMIN", "HR")

                // ── 4. ADMIN + HR: Payroll management ──────────────────────
                .requestMatchers(HttpMethod.POST,
                    "/api/payroll/save",
                    "/api/payroll/lock",
                    "/api/payroll/generate",
                    "/api/payroll/process-all")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.PUT,
                    "/api/payroll/approve-all")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.GET,
                    "/api/payroll/locked",
                    "/api/payroll/report",
                    "/api/payroll/summary")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.PUT,
                    "/api/payroll/{id}/approve",
                    "/api/payroll/{id}/hold")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.POST,
                    "/api/payroll/{id}/process-payment",
                    "/api/payroll/{id}/retry")
                    .hasAnyRole("ADMIN", "HR")

                // ── 5. ADMIN + HR: Dashboard payroll & employee delete ──────
                .requestMatchers(HttpMethod.POST,
                    "/api/dashboard/payroll/generate",
                    "/api/dashboard/payroll/{id}/pay",
                    "/api/dashboard/payroll/{id}/retry")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.PUT,
                    "/api/dashboard/payroll/{id}/approve",
                    "/api/dashboard/payroll/{id}/hold")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.DELETE,
                    "/api/dashboard/employees/{id}")
                    .hasAnyRole("ADMIN", "HR")

                // ── 6. ADMIN + HR: Manual attendance & exports ──────────────
                .requestMatchers(
                    HttpMethod.POST, "/api/attendance/manual")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(
                    HttpMethod.GET, "/api/attendance/export/**")
                    .hasAnyRole("ADMIN", "HR")

                // ── 7. ADMIN + HR: Leave balance report ────────────────────
                .requestMatchers(
                    HttpMethod.GET, "/api/leaves/report/balance")
                    .hasAnyRole("ADMIN", "HR")
                    
                    .requestMatchers(HttpMethod.GET, "/api/attendance/edit-history")
                    .hasAnyRole("ADMIN", "HR")

                // ── 8. ADMIN + HR + MANAGER: Dashboard views ───────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/dashboard/overview",
                    "/api/dashboard/employees",
                    "/api/dashboard/attendance",
                    "/api/dashboard/departments",
                    "/api/dashboard/payroll")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 9. ADMIN + HR + MANAGER: Leave approval / rejection ─────
                .requestMatchers(HttpMethod.PATCH,
                    "/api/leaves/{id}/approve",
                    "/api/leaves/{id}/reject")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                .requestMatchers(HttpMethod.GET,
                    "/api/leaves/pending",
                    "/api/leaves/pending/manager/{managerEmployeeId}")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "HR", "MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/leaves/employee/{empId}")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 10. ADMIN + HR + MANAGER: Attendance reports ────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/attendance/report/daily",
                    "/api/attendance/report/monthly")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 11. ADMIN + HR + MANAGER: Admin calendar view ───────────
                .requestMatchers(HttpMethod.GET, "/api/calendar/admin")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 11a. Holiday CRUD: view = all roles, manage = ADMIN + HR ─
                .requestMatchers(HttpMethod.POST,   "/api/calendar/holidays")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.PUT,    "/api/calendar/holidays/{id}")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.DELETE, "/api/calendar/holidays/{id}")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.GET,    "/api/calendar/holidays")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 12. ALL AUTHENTICATED: Own attendance ────────────────────
                .requestMatchers(HttpMethod.POST,
                    "/api/attendance/checkin",
                    "/api/attendance/checkout")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                .requestMatchers(HttpMethod.GET,
                    "/api/attendance/today",
                    "/api/attendance/date")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                    
                    .requestMatchers(HttpMethod.PUT, "/api/attendance/{id}/edit")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 13. ALL AUTHENTICATED: Own leave ────────────────────────
                .requestMatchers(
                    HttpMethod.POST, "/api/leaves/apply")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 14. ALL AUTHENTICATED: Own profile ──────────────────────
                .requestMatchers(
                    HttpMethod.GET, "/api/employee/{id}")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 16. ALL AUTHENTICATED: Own payslip ──────────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/payroll/month",
                    "/api/payroll/employee/{employeeId}")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 17. ALL AUTHENTICATED: Personal calendar ────────────────
                .requestMatchers(
                    HttpMethod.GET, "/api/calendar/employee/{employeeId}")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 18. ALL AUTHENTICATED: File uploads ──────────────────────
                .requestMatchers("/api/upload/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                    
                .requestMatchers(HttpMethod.PUT, "/api/auth/update-password").authenticated()

                // ── 19. Deny anything else not matched above ─────────────────
                .anyRequest().authenticated()
            )

            // JWT filter runs before Spring's username/password filter
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

   
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}