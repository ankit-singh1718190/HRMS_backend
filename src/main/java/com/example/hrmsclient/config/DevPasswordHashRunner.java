package com.example.hrmsclient.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Runs only when spring.profiles.active=dev.
 * Logs a BCrypt hash you can use to fix "Encoded password does not look like BCrypt" by
 * updating an admin/employee row in the DB:
 *
 *   UPDATE admin SET password = '<logged hash>' WHERE email_id = 'your@email.com';
 *   -- or for employees table:
 *   UPDATE employees SET password = '<logged hash>' WHERE email_id = 'your@email.com';
 */
@Configuration
@Profile("dev")
public class DevPasswordHashRunner {

    private static final Logger log = LoggerFactory.getLogger(DevPasswordHashRunner.class);

    private static final String SAMPLE_PASSWORD = "Password@123";

    @Bean
    public CommandLineRunner logBcryptHash(PasswordEncoder passwordEncoder) {
        return args -> {
            String hash = passwordEncoder.encode(SAMPLE_PASSWORD);
            log.info("--- DEV: BCrypt hash for '{}' (use to fix plain-text passwords in DB) ---", SAMPLE_PASSWORD);
            log.info("Hash: {}", hash);
            log.info("SQL example: UPDATE admin SET password = '{}' WHERE email_id = 'your@email.com';", hash);
            log.info("---");
        };
    }
}
