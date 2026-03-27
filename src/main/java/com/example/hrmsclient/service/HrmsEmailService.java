package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.LeaveRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class HrmsEmailService {

    private static final Logger log = Logger.getLogger(HrmsEmailService.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final String HR_EMAIL = "hr@yourcompany.com";

    private final EmailService emailService;

    @Value("${app.mail.base-url}")
    private String baseUrl;

    @Value("${app.mail.from-name}")
    private String companyName;
    
    public HrmsEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    public void sendWelcomeEmail(Employee employee, String plainPassword) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName", employee.getFullName());
            vars.put("employeeId",   employee.getEmployeeId());
            vars.put("department",   employee.getDepartment());
            vars.put("designation",  employee.getDesignation());
            vars.put("joiningDate",  employee.getJoiningDate() != null
                ? employee.getJoiningDate().format(DATE_FMT) : "N/A");
            vars.put("loginEmail",   employee.getWorkEmail() != null
                ? employee.getWorkEmail() : employee.getEmailId());
            vars.put("tempPassword", plainPassword);
            vars.put("loginUrl",     baseUrl + "/login");
            vars.put("companyName",  companyName);

            emailService.sendTemplatedEmailAsync(
                employee.getEmailId(),
                "Welcome to " + companyName + " — Your Account is Ready",
                "email/welcome",
                vars
            );

            // HR notification via simple email
            emailService.sendSimpleEmail(
                HR_EMAIL,
                "[HRMS] New Employee Onboarded: " + employee.getFullName(),
                employee.getFullName() + " (" + employee.getEmployeeId() + ") has been " +
                "registered in " + employee.getDepartment() +
                " as " + employee.getDesignation() + "."
            );
        } catch (Exception e) {
            log.severe("Failed to send welcome email: " + e.getMessage());
        }
    }
   
    //  2. LEAVE MANAGEMENT
    @Async
    public void sendLeaveAppliedEmail(LeaveRequest leave) {
        try {
            Employee emp = leave.getEmployee();
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName", emp.getFullName());
            vars.put("leaveType",    leave.getLeaveType());
            vars.put("startDate",    leave.getStartDate().format(DATE_FMT));
            vars.put("endDate",      leave.getEndDate().format(DATE_FMT));
            vars.put("totalDays",    leave.getLeaveDays());
            vars.put("reason",       leave.getReason());
            vars.put("status",       "PENDING");
            vars.put("companyName",  companyName);

            // Confirm to employee
            emailService.sendTemplatedEmailAsync(
                emp.getEmailId(),
                "Leave Application Received — " + leave.getLeaveType(),
                "email/leave-applied",
                vars
            );

            // Notify HR
            emailService.sendSimpleEmail(
                HR_EMAIL,
                "[HRMS] Leave Request: " + emp.getFullName() + " (" + leave.getLeaveType() + ")",
                emp.getFullName() + " applied for " + leave.getLeaveType() +
                " from " + leave.getStartDate().format(DATE_FMT) +
                " to "   + leave.getEndDate().format(DATE_FMT) +
                " ("     + leave.getLeaveDays() + " days). Please review."
            );
        } catch (Exception e) {
            log.severe("Failed to send leave applied email: " + e.getMessage());
        }
    }

    @Async
    public void sendLeaveApprovedEmailAsync(
            String email,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String leaveType,
            String approvedBy
    ) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName", name);
            vars.put("leaveType", leaveType);
            vars.put("startDate", startDate.format(DATE_FMT));
            vars.put("endDate", endDate.format(DATE_FMT));
            vars.put("approvedBy", approvedBy);
            vars.put("companyName", companyName);

            emailService.sendTemplatedEmailAsync(
                email,
                "Leave Approved — " + leaveType,
                "email/leave-approved",
                vars
            );

        } catch (Exception e) {
            log.severe("Failed to send leave approved email: " + e.getMessage());
        }
    }

    @Async
    public void sendLeaveRejectedEmail(LeaveRequest leave) {
        try {
            Employee emp = leave.getEmployee();
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName",    emp.getFullName());
            vars.put("leaveType",       leave.getLeaveType());
            vars.put("startDate",       leave.getStartDate().format(DATE_FMT));
            vars.put("endDate",         leave.getEndDate().format(DATE_FMT));
            vars.put("rejectionReason", leave.getRejectionReason());
            vars.put("companyName",     companyName);

            emailService.sendTemplatedEmailAsync(
                emp.getEmailId(),
                "Leave Request Rejected — " + leave.getLeaveType(),
                "email/leave-rejected",
                vars
            );
        } catch (Exception e) {
            log.severe("Failed to send leave rejected email: " + e.getMessage());
        }
    }
//  3. ATTENDANCE
@Async
public void sendAttendanceMarkedEmail(Attendance attendance) {
    try {
        Employee emp = attendance.getEmployee();
        Map<String, Object> vars = new HashMap<>();

        vars.put("employeeName", emp.getFullName());
        vars.put("date", attendance.getAttendanceDate().format(DATE_FMT));
        vars.put("status", attendance.getStatus().name());

        vars.put("checkIn", attendance.getCheckIn() != null
                ? attendance.getCheckIn().toLocalTime().toString()
                : "—");

        vars.put("checkOut", attendance.getCheckOut() != null
                ? attendance.getCheckOut().toLocalTime().toString()
                : "—");

        String workingHours = attendance.getCheckIn() != null
                ? attendance.getWorkingHours()
                : "—";

        vars.put("workingHours", workingHours);
        vars.put("companyName", companyName);

        emailService.sendTemplatedEmailAsync(
                emp.getEmailId(),
                "Attendance Marked — " + attendance.getAttendanceDate().format(DATE_FMT),
                "email/attendance-marked",
                vars
        );

    } catch (Exception e) {
        log.severe("Failed to send attendance email: " + e.getMessage());
    }
}

    @Async
    public void sendAbsenteeAlert(Employee employee, String date) {
        try {
            emailService.sendSimpleEmail(
                HR_EMAIL,
                "[HRMS] Absentee Alert: " + employee.getFullName(),
                employee.getFullName() + " (" + employee.getEmployeeId() + ") from " +
                employee.getDepartment() + " is absent on " + date +
                " with no leave applied."
            );
        } catch (Exception e) {
            log.severe("Failed to send absentee alert: " + e.getMessage());
        }
    }

      //  4. PASSWORD MANAGEMENT
    @Async
    public void sendPasswordResetEmail(Employee employee, String resetToken) {
        try {
            String resetLink = baseUrl + "/reset-password?token=" + resetToken;
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName",  employee.getFullName());
            vars.put("resetLink",     resetLink);
            vars.put("expiryMinutes", 30);
            vars.put("companyName",   companyName);

            emailService.sendTemplatedEmailAsync(
                employee.getEmailId(),
                "Password Reset Request — " + companyName,
                "email/password-reset",
                vars
            );
        } catch (Exception e) {
            log.severe("Failed to send password reset email: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangedEmail(Employee employee) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName", employee.getFullName());
            vars.put("companyName",  companyName);
            vars.put("supportEmail", "support@yourcompany.com");
            vars.put("changedAt",    LocalDateTime.now().format(DT_FMT));

            emailService.sendTemplatedEmailAsync(
                employee.getEmailId(),
                "Your Password Was Changed — " + companyName,
                "email/password-changed",
                vars
            );
        } catch (Exception e) {
            log.severe("Failed to send password changed email: " + e.getMessage());
        }
    }

    //  5. PROFILE & ACCOUNT EVENTS
    @Async
    public void sendProfileUpdatedEmail(Employee employee) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName", employee.getFullName());
            vars.put("updatedAt",    LocalDateTime.now().format(DT_FMT));
            vars.put("companyName",  companyName);

            emailService.sendTemplatedEmailAsync(
                employee.getEmailId(),
                "Your Profile Was Updated — " + companyName,
                "email/profile-updated",
                vars
            );
        } catch (Exception e) {
            log.severe("Failed to send profile updated email: " + e.getMessage());
        }
    }

    @Async
    public void sendResignationAcknowledgementEmail(Employee employee, String lastWorkingDay) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName",  employee.getFullName());
            vars.put("employeeId",    employee.getEmployeeId());
            vars.put("lastWorkingDay", lastWorkingDay);
            vars.put("companyName",   companyName);

            emailService.sendTemplatedEmailAsync(
                employee.getEmailId(),
                "Resignation Acknowledged — " + companyName,
                "email/resignation-ack",
                vars
            );

            emailService.sendSimpleEmail(
                HR_EMAIL,
                "[HRMS] Resignation: " + employee.getFullName(),
                employee.getFullName() + " (" + employee.getEmployeeId() + ") from " +
                employee.getDepartment() + " has submitted resignation. " +
                "Last working day: " + lastWorkingDay
            );
        } catch (Exception e) {
            log.severe("Failed to send resignation email: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  6. PAYROLL / SALARY SLIP
    // ══════════════════════════════════════════════════════════════

    @Async
    public void sendSalarySlipEmail(Employee employee, File salarySlipPdf, String month) {
        try {
            String html = buildSalarySlipEmailHtml(employee, month);
            emailService.sendEmailWithAttachment(
                employee.getEmailId(),
                "Salary Slip for " + month + " — " + companyName,
                html,
                salarySlipPdf
            );
        } catch (Exception e) {
            log.severe("Failed to send salary slip email: " + e.getMessage());
        }
    }

    private String buildSalarySlipEmailHtml(Employee employee, String month) {
        return "<html><body style=\"font-family: Arial, sans-serif; color: #333;\">"
             + "<div style=\"max-width:600px;margin:auto;padding:20px;"
             + "border:1px solid #eee;border-radius:8px;\">"
             + "<h2 style=\"color:#1a73e8;\">Salary Slip — " + month + "</h2>"
             + "<p>Dear <strong>" + employee.getFullName() + "</strong>,</p>"
             + "<p>Please find attached your salary slip for <strong>"
             + month + "</strong>.</p>"
             + "<p>If you have any questions, contact HR.</p>"
             + "<br><p>Regards,<br><strong>" + companyName + " HR Team</strong></p>"
             + "</div></body></html>";
    }

    //  7. BIRTHDAY / WORK ANNIVERSARY

    @Async
    public void sendBirthdayEmail(Employee employee) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName", employee.getFirstName());
            vars.put("companyName",  companyName);

            emailService.sendTemplatedEmailAsync(
                employee.getEmailId(),
                "Happy Birthday, " + employee.getFirstName() + "!",
                "email/birthday",
                vars
            );
        } catch (Exception e) {
            log.severe("Failed to send birthday email: " + e.getMessage());
        }
    }

    @Async
    public void sendWorkAnniversaryEmail(Employee employee, long years) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("employeeName", employee.getFirstName());
            vars.put("years",        years);
            vars.put("joiningDate",  employee.getJoiningDate().format(DATE_FMT));
            vars.put("companyName",  companyName);

            emailService.sendTemplatedEmailAsync(
                employee.getEmailId(),
                "Happy " + years + "-Year Work Anniversary, " + employee.getFirstName() + "!",
                "email/work-anniversary",
                vars
            );
        } catch (Exception e) {
            log.severe("Failed to send anniversary email: " + e.getMessage());
        }
    }
}