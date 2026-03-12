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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    private static final String[] PUBLIC_URLS = {
        "/api/auth/**",             
        "/api/employee/register",
        "/api/attendance/**",
        "/api/admin/register",
        "/api/attendance/today",
        "/api/leaves/apply",
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info",
        "/api/employee/form16/**"
    };

    private static final String[] ADMIN_ONLY_URLS = {
        "/api/admin/**",
        "/api/employees/delete/**",
        "/api/dashboard/overview",
        "/api/reports/**",
        "/api/dashboard/employees",
        "/api/dashboard/attendance",
        "/api/dashboard/departments",
        "/api/dashboard/payroll/**",
        "/api/leaves/*/approve",
        "/api/leaves/*/pending",
        "/api/leaves/*/reject",
        "/api/leaves/employee/**",
        "/api/admin/form16/**",
        "/api/payroll/**"
    };


    private static final String[] HR_MANAGER_URLS = {
        "/api/employees/**",
        "/api/attendance/manage/**",
        "/api/leave/approve/**",
        "/api/leave/reject/**",
        "/api/attendance/**",
        "/api/dashboard/overview",
        "/api/dashboard/employees",
        "/api/dashboard/attendance",
        "/api/leaves/*/approve",
        "/api/leaves/*/pending",
        "/api/leaves/*/reject",
        "/api/leaves/employee/**",
        "/api/admin/form16/**",
        "/api/payroll/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless JWT — no sessions)
            .csrf(csrf -> csrf.disable())

            // Enable CORS (for frontend calls)
            .cors(cors -> {})

            // Stateless session — no server-side sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                		PUBLIC_URLS
                		).permitAll()

             
                .requestMatchers(ADMIN_ONLY_URLS)
                    .hasRole("ADMIN")

          
                .requestMatchers(HR_MANAGER_URLS)
                    .hasAnyRole("ADMIN", "HR", "MANAGER")

           
                .requestMatchers(HttpMethod.GET, "/api/employee/profile/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                .requestMatchers("/api/leave/apply/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")

                .requestMatchers("/api/attendance/checkin/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
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