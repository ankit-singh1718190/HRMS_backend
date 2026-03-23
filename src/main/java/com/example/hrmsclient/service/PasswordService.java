package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.ForgotPasswordRequestDTO;
import com.example.hrmsclient.dto.ResetPasswordRequestDTO;
import com.example.hrmsclient.dto.UpdatePasswordRequestDTO;
import com.example.hrmsclient.entity.Admin;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.PasswordResetToken;
import com.example.hrmsclient.repository.AdminRepository;
import com.example.hrmsclient.repository.EmployeeRepository;
import com.example.hrmsclient.repository.PasswordResetTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PasswordService {

    private static final Logger log = Logger.getLogger(PasswordService.class.getName());
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    private final AdminRepository adminRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordService(AdminRepository adminRepository,
                           EmployeeRepository employeeRepository,
                           PasswordResetTokenRepository tokenRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.adminRepository = adminRepository;
        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        boolean exists = adminRepository.existsByEmailId(email)
                || employeeRepository.existsByEmailIdAndDeletedFalse(email);

        if (!exists) return;

        tokenRepository.deleteAllByEmail(email);

        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(email);
        resetToken.setToken(rawToken);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        tokenRepository.save(resetToken);

        String resetLink = "http://localhost:3000/reset-password?token=" + rawToken;
        String subject = "HRMS - Password Reset Request";
        String body = buildForgotPasswordEmail(email, resetLink);
        try {
            emailService.sendHtmlEmail(email, subject, body);
            log.info("Password reset email sent to: " + email);
        } catch (Exception e) {
            log.severe("Failed to send reset email to " + email + ": " + e.getMessage());
            throw new RuntimeException("Failed to send reset email: " + e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Reset token has expired. Please request a new one");
        }

        String email = resetToken.getEmail();
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        Optional<Admin> adminOpt = adminRepository.findByEmailId(email);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            admin.setPassword(encodedPassword);
            adminRepository.save(admin);
        } else {
            Employee employee = employeeRepository.findByEmailIdAndDeletedFalse(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            employee.setPassword(encodedPassword);
            employeeRepository.save(employee);
        }

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE PASSWORD
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void updatePassword(String email, UpdatePasswordRequestDTO request) {
        log.info("updatePassword called for email: [" + email + "]");

        // Basic validation
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Current password is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        // ── Try ADMIN ─────────────────────────────────────────────────────────
        Optional<Admin> adminOpt = adminRepository.findByEmailId(email);

        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            log.info("Admin found: " + admin.getEmailId());

            String storedPassword = admin.getPassword();

            // Null-safe check — NoOpPasswordEncoder stores plain text
            if (storedPassword == null) {
                throw new IllegalArgumentException("No password set for this account. Please contact admin.");
            }

            // With NoOpPasswordEncoder: matches() = currentPassword.equals(storedPassword)
            if (!storedPassword.equals(request.getCurrentPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }

            admin.setPassword(request.getNewPassword()); // NoOpPasswordEncoder: store as-is
            adminRepository.save(admin);
            log.info("Admin password updated successfully for: " + email);
            return;
        }

        // ── Try EMPLOYEE ──────────────────────────────────────────────────────
        log.info("Not found as admin, trying employee: " + email);

        Employee employee = employeeRepository.findByEmailIdAndDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for: " + email));

        log.info("Employee found: " + employee.getEmailId());

        String storedPassword = employee.getPassword();

        if (storedPassword == null) {
            throw new IllegalArgumentException("No password set for this account. Please contact admin.");
        }

        // With NoOpPasswordEncoder: direct string comparison
        if (!storedPassword.equals(request.getCurrentPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        employee.setPassword(request.getNewPassword()); // NoOpPasswordEncoder: store as-is
        employeeRepository.save(employee);
        log.info("Employee password updated successfully for: " + email);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email template
    // ─────────────────────────────────────────────────────────────────────────
    private String buildForgotPasswordEmail(String email, String resetLink) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #2c3e50;">Password Reset Request</h2>
                <p>Hello,</p>
                <p>We received a request to reset the password for your HRMS account associated with <strong>%s</strong>.</p>
                <p>Click the button below to reset your password. This link is valid for <strong>%d minutes</strong>.</p>
                <div style="text-align: center; margin: 32px 0;">
                    <a href="%s" style="background-color: #3498db; color: white; padding: 12px 28px; border-radius: 6px; text-decoration: none; font-size: 16px;">
                        Reset Password
                    </a>
                </div>
                <p style="color: #888; font-size: 13px;">If you did not request a password reset, please ignore this email. Your account is safe.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;" />
                <p style="color: #aaa; font-size: 12px;">HRMS System &mdash; Do not reply to this email.</p>
            </div>
            """.formatted(email, TOKEN_EXPIRY_MINUTES, resetLink);
    }
}