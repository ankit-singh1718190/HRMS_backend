package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.repository.AttendanceRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class AttendanceEmailService {

    private static final Logger log = Logger.getLogger(AttendanceEmailService.class.getName());

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AttendanceRepository attendanceRepository;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.admin}")
    private String adminEmail;

    public AttendanceEmailService(JavaMailSender mailSender,
                                  TemplateEngine templateEngine,
                                  AttendanceRepository attendanceRepository) {
        this.mailSender           = mailSender;
        this.templateEngine       = templateEngine;
        this.attendanceRepository = attendanceRepository;
    }

    // ── MORNING REPORT – runs every weekday at 10:00 AM 
    @Scheduled(cron = "0 0 10 * * MON-FRI")
    public void sendMorningCheckinReport() {
        LocalDate today = LocalDate.now();
        log.info("Sending morning check-in report for: " + today);

        List<Attendance> checkedIn = attendanceRepository
                .findByAttendanceDateAndCheckInIsNotNull(today);

        String subject = "Morning Check-In Report – " +
                today.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        Map<String, Object> vars = Map.of(
                "reportTitle",    "Morning Check-In Report",
                "reportDate",     today.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                "generatedAt",    LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")),
                "attendanceList", checkedIn,
                "totalCount",     checkedIn.size(),
                "reportType",     "CHECK-IN"
        );

        sendAttendanceEmail(adminEmail, subject, "email/checkin-report", vars);
    }

    // ── EVENING REPORT – runs every weekday at 07:00 PM 
    @Scheduled(cron = "0 0 19 * * MON-FRI")
    public void sendEveningCheckoutReport() {
        LocalDate today = LocalDate.now();
        log.info("Sending evening check-out report for: " + today);

        List<Attendance> checkedOut =
                attendanceRepository.findByAttendanceDateAndCheckOutIsNotNull(today);

        String subject = "Evening Check-Out Report – " +
                today.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        Map<String, Object> vars = Map.of(
                "reportTitle",    "Evening Check-Out Report",
                "reportDate",     today.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                "generatedAt",    LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")),
                "attendanceList", checkedOut,
                "totalCount",     checkedOut.size(),
                "reportType",     "CHECK-OUT"
        );

        sendAttendanceEmail(adminEmail, subject, "email/checkout-report", vars);
    }

    // ── MANUAL TRIGGERS 
    public void triggerCheckinReport()  { sendMorningCheckinReport();  }
    public void triggerCheckoutReport() { sendEveningCheckoutReport(); }

    // ── EMPLOYEE CHECK-IN CONFIRMATION EMAIL 
    public void sendCheckInEmail(Employee employee, Attendance attendance) {
        try {
            Context context = new Context();
            context.setVariables(Map.of(
                    "employeeName",   employee.getFirstName() + " " + employee.getLastName(),
                    "attendanceDate", attendance.getAttendanceDate()
                                         .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                    "checkInTime",    attendance.getCheckIn()
                                         .format(DateTimeFormatter.ofPattern("hh:mm a")),
                    "checkInAddress", attendance.getCheckInAddress() != null
                                         ? attendance.getCheckInAddress() : "N/A"
            ));

            String htmlContent = templateEngine.process("email/employee-checkin", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(employee.getEmailId());
            helper.setSubject("Check-In Confirmation – " +
                    attendance.getAttendanceDate()
                              .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Check-in confirmation email sent to: " + employee.getEmailId());

        } catch (Exception e) {
            log.severe("Failed to send check-in email: " + e.getMessage());
            throw new RuntimeException("Check-in email failed: " + e.getMessage(), e);
        }
    }

    // ── EMPLOYEE CHECK-OUT CONFIRMATION EMAIL
    public void sendCheckOutEmail(Employee employee, Attendance attendance) {
        try {
            Context context = new Context();
            context.setVariables(Map.of(
                    "employeeName",   employee.getFirstName() + " " + employee.getLastName(),
                    "attendanceDate", attendance.getAttendanceDate()
                                         .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
                    "checkInTime",    attendance.getCheckIn()
                                         .format(DateTimeFormatter.ofPattern("hh:mm a")),
                    "checkOutTime",   attendance.getCheckOut()
                                         .format(DateTimeFormatter.ofPattern("hh:mm a")),
                    "workingHours",   attendance.getWorkingHours() != null
                                         ? attendance.getWorkingHours() : "N/A"
            ));

            String htmlContent = templateEngine.process("email/employee-checkout", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(employee.getEmailId());
            helper.setSubject("Check-Out Confirmation – " +
                    attendance.getAttendanceDate()
                              .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Check-out confirmation email sent to: " + employee.getEmailId());

        } catch (Exception e) {
            log.severe("Failed to send check-out email: " + e.getMessage());
            throw new RuntimeException("Check-out email failed: " + e.getMessage(), e);
        }
    }

    // ── INTERNAL HELPER
    private void sendAttendanceEmail(String to, String subject,
                                     String templateName,
                                     Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Attendance report sent to admin: " + to + " | Subject: " + subject);

        } catch (Exception e) {
            log.severe("Failed to send attendance report: " + e.getMessage());
            throw new RuntimeException("Attendance email failed: " + e.getMessage(), e);
        }
    }
}