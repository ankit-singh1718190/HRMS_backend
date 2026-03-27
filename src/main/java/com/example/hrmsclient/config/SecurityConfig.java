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

    // ── Public — no auth needed ───────────────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
        "/api/auth/login",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info"
    };

    // ── ADMIN-only (user management, email logs) ──────────────────────────────
    private static final String[] ADMIN_ONLY_URLS = {
        "/api/admin/**",          // Admin user CRUD — ADMIN only
        "/api/dashboard/admin-stats",
        "/api/emails/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ── 1. PUBLIC ─────────────────────────────────────────────────
                .requestMatchers(PUBLIC_URLS).permitAll()

                // ── 2. ADMIN ONLY (admin user management & email logs) ─────────
                .requestMatchers(ADMIN_ONLY_URLS).hasRole("ADMIN")

                // ── 3. Employee CRUD: create ───────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/employee/register")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "HR")

                // ── 4. Employee READ: ADMIN + HR + MANAGER ─────────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/employee",
                    "/api/employee/search",
                    "/api/employee/filter/**",
                    "/api/employee/active",
                    "/api/employee/exited",
                    "/api/employee/departments",
                    "/api/employee/dashboard")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "HR", "MANAGER")

                // ── 5. Employee UPDATE / DELETE: ADMIN + HR ────────────────────
                .requestMatchers(HttpMethod.PUT,    "/api/employee/{id}")
                    .hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.PUT,    "/api/employee/{id}/exit")
                    .hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.DELETE, "/api/employee/{id}")
                    .hasAnyRole("ADMIN", "HR")

                // ── 6. Payroll WRITE: ADMIN + HR only ─────────────────────────
                .requestMatchers(HttpMethod.POST,
                    "/api/payroll/save",
                    "/api/payroll/generate",
                    "/api/payroll/process-all")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.POST, "/api/payroll/lock")
                    .hasRole("ADMIN")

                .requestMatchers(HttpMethod.PUT,
                    "/api/payroll/approve-all",
                    "/api/payroll/{id}/approve",
                    "/api/payroll/{id}/hold")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.POST,
                    "/api/payroll/{id}/process-payment",
                    "/api/payroll/{id}/retry")
                    .hasAnyRole("ADMIN", "HR")

                // ── 7. Payroll READ: ADMIN + HR + MANAGER ─────────────────────
                //       (MANAGER sees only their team's payroll via service layer)
                .requestMatchers(HttpMethod.GET,
                    "/api/payroll/locked",
                    "/api/payroll/report",
                    "/api/payroll/summary",
                    "/api/payroll/month")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 8. Form 16 WRITE: ADMIN + HR only ─────────────────────────
                .requestMatchers(HttpMethod.POST,
                    "/api/admin/form16/upload-bulk",
                    "/api/admin/form16/upload/**")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.DELETE, "/api/admin/form16/**")
                    .hasAnyRole("ADMIN", "HR")

                // ── 9. Form 16 READ: ADMIN + HR + MANAGER ─────────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/admin/form16/list",
                    "/api/admin/form16/status")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 10. Attendance WRITE: ADMIN + HR only ─────────────────────
                .requestMatchers(HttpMethod.POST, "/api/attendance/manual")
                    .hasAnyRole("ADMIN", "HR")

                // ── 11. Attendance EXPORT: ADMIN + HR + MANAGER ───────────────
                .requestMatchers(HttpMethod.GET, "/api/attendance/export/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 12. Attendance REPORTS + EDIT HISTORY: ADMIN + HR + MANAGER
                .requestMatchers(HttpMethod.GET,
                    "/api/attendance/edit-history",
                    "/api/attendance/report/daily",
                    "/api/attendance/report/monthly")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 13. Leave balance report: ADMIN + HR + MANAGER ────────────
                .requestMatchers(HttpMethod.GET, "/api/leaves/report/balance")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 14. Leave approval / rejection: ADMIN + HR + MANAGER ───────
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

                // ── 15. Dashboard views: ADMIN + HR + MANAGER ─────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/dashboard/overview",
                    "/api/dashboard/employees",
                    "/api/dashboard/attendance",
                    "/api/dashboard/departments",
                    "/api/dashboard/payroll")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 16. Dashboard payroll actions: ADMIN + HR ──────────────────
                .requestMatchers(HttpMethod.POST,
                    "/api/dashboard/payroll/generate",
                    "/api/dashboard/payroll/{id}/pay",
                    "/api/dashboard/payroll/{id}/retry")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.PUT,
                    "/api/dashboard/payroll/{id}/approve",
                    "/api/dashboard/payroll/{id}/hold")
                    .hasAnyRole("ADMIN", "HR")

                .requestMatchers(HttpMethod.DELETE, "/api/dashboard/employees/{id}")
                    .hasAnyRole("ADMIN", "HR")

                // ── 17. Calendar: holiday manage = ADMIN + HR; view = all ──────
                .requestMatchers(HttpMethod.POST,   "/api/calendar/holidays")
                    .hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.PUT,    "/api/calendar/holidays/{id}")
                    .hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.DELETE, "/api/calendar/holidays/{id}")
                    .hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.GET,    "/api/calendar/holidays")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                .requestMatchers(HttpMethod.GET,    "/api/calendar/admin")
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

                // ── 18. ALL AUTHENTICATED: own attendance check-in / check-out ─
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

                // ── 19. ALL AUTHENTICATED: own payslip, leave balance ──────────
                .requestMatchers(HttpMethod.GET, "/api/payroll/{id}/payslip")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                .requestMatchers(HttpMethod.GET, "/api/leaves/balance/summary/{empId}")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 20. ALL AUTHENTICATED: own leave ──────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/leaves/apply")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 21. ALL AUTHENTICATED: own profile ────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/employee/{id}")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 22. ALL AUTHENTICATED: own payroll history ────────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/payroll/employee/{employeeId}")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 23. ALL AUTHENTICATED: personal calendar ──────────────────
                .requestMatchers(HttpMethod.GET, "/api/calendar/employee/{employeeId}")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 24. ALL AUTHENTICATED: own Form 16 ────────────────────────
                .requestMatchers("/api/employee/form16/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                // ── 25. ALL AUTHENTICATED: file uploads ───────────────────────
                .requestMatchers("/api/upload/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                .requestMatchers(HttpMethod.PUT, "/api/auth/update-password").authenticated()

                // ── 26. Deny anything else ────────────────────────────────────
                .anyRequest().authenticated()
            )

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