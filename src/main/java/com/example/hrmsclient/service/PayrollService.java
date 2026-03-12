package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PayrollService
 * ─────────────────────────────────────────────────────────────────────────────
 * ALL operations are ADMIN/HR triggered manually via PayrollController.
 * No @Scheduled cron jobs — admin has full control.
 *
 * Manual Flow (Admin Dashboard):
 *   STEP 1 → POST /api/payroll/generate?month=2025-03-01     (Generate)
 *   STEP 2 → PUT  /api/payroll/{id}/approve                  (Approve)
 *   STEP 3 → POST /api/payroll/{id}/process-payment          (Pay)
 */
@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository         payrollRepository;
    private final EmployeeRepository        employeeRepository;
    private final AttendanceRepository      attendanceRepository;
    private final PayrollCalculationService calculationService;
    private final EmailService              emailService;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeRepository employeeRepository,
                          AttendanceRepository attendanceRepository,
                          PayrollCalculationService calculationService,
                          EmailService emailService) {
        this.payrollRepository    = payrollRepository;
        this.employeeRepository   = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.calculationService   = calculationService;
        this.emailService         = emailService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — ADMIN MANUALLY GENERATES PAYROLL
    // Triggered by: POST /api/payroll/generate?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void generatePayrollForAllEmployees(LocalDate month) {

        Page<Employee> activePage = employeeRepository
                .findByEmploymentStatusAndDeletedFalse(
                        EmploymentStatus.ACTIVE,
                        PageRequest.of(0, Integer.MAX_VALUE, Sort.by("firstName")));

        List<Employee> activeEmployees = activePage.getContent();
        log.info("Admin triggered payroll generation for {} employees | Month: {}",
                activeEmployees.size(), month);

        for (Employee employee : activeEmployees) {
            try {
                generatePayrollForEmployee(employee, month);
            } catch (Exception e) {
                log.error("Payroll generation failed for {}: {}",
                        employee.getEmployeeId(), e.getMessage());
            }
        }

        log.info("Payroll generation completed for month: {}", month);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERATE PAYROLL FOR A SINGLE EMPLOYEE
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Payroll generatePayrollForEmployee(Employee employee, LocalDate month) {

        // Skip if already generated for this month
        if (payrollRepository.existsByEmployeeIdAndPayrollMonth(employee.getId(), month)) {
            log.info("Payroll already exists for {} - {}", employee.getEmployeeId(), month);
            return payrollRepository
                    .findByEmployeeIdAndPayrollMonth(employee.getId(), month)
                    .orElseThrow(() -> new RuntimeException("Payroll not found"));
        }

        // ── Attendance for the month ──────────────────────────────────────────
        YearMonth ym         = YearMonth.of(month.getYear(), month.getMonth());
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd   = ym.atEndOfMonth();
        int workingDays      = calculateWorkingDays(monthStart, monthEnd);

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(
                        employee.getId(), monthStart, monthEnd);

        long presentDays = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                          || a.getStatus() == AttendanceStatus.WORK_FROM_HOME
                          || a.getStatus() == AttendanceStatus.HALF_DAY)
                .count();

        long leaveDays = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE)
                .count();

        long absentDays = Math.max(workingDays - presentDays - leaveDays, 0);

        // ── Build Payroll object ──────────────────────────────────────────────
        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setPayrollMonth(month);
        payroll.setWorkingDays(workingDays);
        payroll.setPresentDays((int) presentDays);
        payroll.setLeaveDays((int) leaveDays);
        payroll.setAbsentDays((int) absentDays);
        payroll.setBankAccount(employee.getAccountNo());
        payroll.setIfscCode(employee.getIfscCode());
        payroll.setStatus(PayrollStatus.PENDING);
        payroll.setProcessedBy("ADMIN");

        // ── Calculate all salary components ──────────────────────────────────
        calculationService.calculate(employee, payroll);

        log.info("Payroll generated for {} | Month: {} | Net: {}",
                employee.getEmployeeId(), month, payroll.getNetSalary());

        return payrollRepository.save(payroll);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — ADMIN APPROVES PAYROLL
    // Triggered by: PUT /api/payroll/{id}/approve
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Payroll approvePayroll(Long payrollId, String approvedBy) {
        Payroll payroll = getPayrollById(payrollId);

        if (payroll.getStatus() != PayrollStatus.PENDING) {
            throw new RuntimeException(
                "Only PENDING payroll can be approved. Current status: " + payroll.getStatus());
        }

        payroll.setStatus(PayrollStatus.APPROVED);
        payroll.setApprovedBy(approvedBy);

        log.info("Payroll approved | ID: {} | Employee: {} | ApprovedBy: {}",
                payrollId, payroll.getEmployee().getEmployeeId(), approvedBy);

        return payrollRepository.save(payroll);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVE ALL PENDING PAYROLLS FOR A MONTH (Bulk Approve)
    // Triggered by: PUT /api/payroll/approve-all?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public int approveAllPendingPayrolls(LocalDate month, String approvedBy) {
        List<Payroll> pendingPayrolls = payrollRepository
                .findByPayrollMonthAndStatus(month, PayrollStatus.PENDING);

        for (Payroll payroll : pendingPayrolls) {
            payroll.setStatus(PayrollStatus.APPROVED);
            payroll.setApprovedBy(approvedBy);
            payrollRepository.save(payroll);
        }

        log.info("Bulk approved {} payrolls for month: {} | By: {}",
                pendingPayrolls.size(), month, approvedBy);

        return pendingPayrolls.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3 — ADMIN PROCESSES PAYMENT (Single)
    // Triggered by: POST /api/payroll/{id}/process-payment
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Payroll manualProcessPayment(Long payrollId, String approvedBy) {
        Payroll payroll = getPayrollById(payrollId);

        if (payroll.getStatus() != PayrollStatus.APPROVED) {
            throw new RuntimeException(
                "Payroll must be APPROVED before processing. Current status: "
                        + payroll.getStatus());
        }

        payroll.setApprovedBy(approvedBy);
        processSinglePayment(payroll);
        return payroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROCESS ALL APPROVED PAYROLLS FOR A MONTH (Bulk Pay)
    // Triggered by: POST /api/payroll/process-all?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public BulkPaymentResult processAllApprovedPayrolls(LocalDate month) {
        List<Payroll> approvedPayrolls = payrollRepository
                .findByPayrollMonthAndStatus(month, PayrollStatus.APPROVED);

        int success = 0, failed = 0;

        log.info("Admin triggered bulk payment for {} payrolls | Month: {}",
                approvedPayrolls.size(), month);

        for (Payroll payroll : approvedPayrolls) {
            try {
                processSinglePayment(payroll);
                success++;
            } catch (Exception e) {
                log.error("Payment failed for {}: {}",
                        payroll.getEmployee().getEmployeeId(), e.getMessage());
                payroll.setStatus(PayrollStatus.FAILED);
                payroll.setRemarks("Payment failed: " + e.getMessage());
                payrollRepository.save(payroll);
                failed++;
            }
        }

        log.info("Bulk payment completed | Success: {} | Failed: {}", success, failed);
        return new BulkPaymentResult(approvedPayrolls.size(), success, failed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE: Process single employee payment
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void processSinglePayment(Payroll payroll) {

        // Mark as PROCESSING
        payroll.setStatus(PayrollStatus.PROCESSING);
        payrollRepository.save(payroll);

        // ── Initiate Bank Transfer ────────────────────────────────────────────
        // In production: replace with RazorpayX / Cashfree / HDFC / ICICI API
        String transactionRef = initiateBankTransfer(
                payroll.getEmployee(),
                payroll.getNetSalary(),
                payroll.getBankAccount(),
                payroll.getIfscCode()
        );

        // ── Mark as PAID ──────────────────────────────────────────────────────
        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());
        payroll.setPaymentReference(transactionRef);
        payroll.setPaymentMode("NEFT");
        payrollRepository.save(payroll);

        log.info("Salary paid | Employee: {} | Amount: {} | Ref: {}",
                payroll.getEmployee().getEmployeeId(),
                payroll.getNetSalary(),
                transactionRef);

        // ── Send Payslip Email with PDF (@Async — non-blocking) ──────────────
        try {
            emailService.sendPayslipEmail(payroll);
            payroll.setPayslipSent(true);
            payrollRepository.save(payroll);
        } catch (Exception e) {
            log.warn("Payslip email failed for {} - payment still succeeded. Error: {}",
                    payroll.getEmployee().getEmailId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT ON HOLD
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Payroll holdPayroll(Long payrollId, String reason) {
        Payroll payroll = getPayrollById(payrollId);

        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new RuntimeException("Cannot hold a payroll that is already PAID");
        }

        payroll.setStatus(PayrollStatus.ON_HOLD);
        payroll.setRemarks(reason);

        log.info("Payroll put on hold | ID: {} | Reason: {}", payrollId, reason);
        return payrollRepository.save(payroll);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RETRY FAILED PAYMENT
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Payroll retryFailedPayment(Long payrollId) {
        Payroll payroll = getPayrollById(payrollId);

        if (payroll.getStatus() != PayrollStatus.FAILED) {
            throw new RuntimeException("Only FAILED payroll can be retried");
        }

        payroll.setStatus(PayrollStatus.APPROVED);
        payroll.setRemarks(null);
        payrollRepository.save(payroll);

        log.info("Retrying failed payment | ID: {}", payrollId);
        processSinglePayment(payroll);
        return payroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY METHODS
    // ─────────────────────────────────────────────────────────────────────────

    public Payroll getPayrollById(Long payrollId) {
        return payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));
    }

    public List<Payroll> getEmployeePayrollHistory(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    public Page<Payroll> getPayrollByMonth(LocalDate month, int page, int size) {
        return payrollRepository.findByPayrollMonth(
                month, PageRequest.of(page, size, Sort.by("employee.firstName")));
    }

    public Optional<Payroll> getPayroll(Long employeeId, LocalDate month) {
        return payrollRepository.findByEmployeeIdAndPayrollMonth(employeeId, month);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BANK TRANSFER — Replace with real bank API in production
    // ─────────────────────────────────────────────────────────────────────────
    private String initiateBankTransfer(Employee employee, BigDecimal amount,
                                         String accountNo, String ifscCode) {
        log.info("Bank transfer initiated | Employee: {} | Account: {} | Amount: {} | IFSC: {}",
                employee.getEmployeeId(), maskAccount(accountNo), amount, ifscCode);

        // Simulated transaction reference
        // PRODUCTION: Replace with RazorpayX / Cashfree / HDFC / ICICI API call
        return "TXN" + UUID.randomUUID().toString()
                           .replace("-", "")
                           .substring(0, 12)
                           .toUpperCase();
    }

    public record BulkPaymentResult(int total, int success, int failed) {}

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
 
    // Count working days (Mon-Sat, skip Sundays)
    private int calculateWorkingDays(LocalDate start, LocalDate end) {
        int count = 0;
        LocalDate date = start;
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek().getValue() != 7) count++;
            date = date.plusDays(1);
        }
        return count;
    }

    // Mask account number for logs
    private String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 4) return "****";
        return "****" + accountNo.substring(accountNo.length() - 4);
    }
}