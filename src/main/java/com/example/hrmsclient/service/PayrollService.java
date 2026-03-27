package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.PayrollRequestDTO;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.io.File;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository          payrollRepository;
    private final EmployeeRepository         employeeRepository;
    private final AttendanceRepository       attendanceRepository;
    private final PayrollCalculationService  calculationService;
    private final EmailService               emailService;
    private final PayslipPdfService          payslipPdfService;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeRepository employeeRepository,
                          AttendanceRepository attendanceRepository,
                          PayrollCalculationService calculationService,
                          EmailService emailService,
                          PayslipPdfService payslipPdfService) {
        this.payrollRepository   = payrollRepository;
        this.employeeRepository  = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.calculationService  = calculationService;
        this.emailService        = emailService;
        this.payslipPdfService   = payslipPdfService;
    }

    // ── NEW: Save or Update Payroll for an Active Employee ─────────────────────
   
    @Transactional
    public Payroll saveOrUpdatePayroll(PayrollRequestDTO dto, String processedBy) {
        // Guard: reject edits to locked months
        if (isMonthLocked(dto.getPayrollMonth())) {
            throw new IllegalStateException(
                "Payroll for " + dto.getPayrollMonth().getMonth()
                + " " + dto.getPayrollMonth().getYear()
                + " is LOCKED and cannot be edited.");
        }

        Employee employee = employeeRepository.findByIdAndDeletedFalse(dto.getEmployeeId())
            .orElseThrow(() -> new RuntimeException(
                "Employee not found: " + dto.getEmployeeId()));

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new IllegalStateException(
                "Payroll can only be updated for ACTIVE employees.");
        }

        // Upsert
        Payroll payroll = payrollRepository
            .findByEmployeeIdAndPayrollMonth(employee.getId(), dto.getPayrollMonth())
            .orElse(new Payroll());

        payroll.setEmployee(employee);
        payroll.setPayrollMonth(dto.getPayrollMonth());
        payroll.setProcessedBy(processedBy);

        // Set admin-entered salary components
        payroll.setBasicSalary(safe(dto.getBasicSalary()));
        payroll.setHra(safe(dto.getHra()));
        payroll.setSpecialAllowance(safe(dto.getSpecialAllowance()));
        payroll.setArrears(safe(dto.getArrears()));
        payroll.setPerfPay(safe(dto.getPerfPay()));
        payroll.setWeekendWorkDays(dto.getWeekendWorkDays() != null ? dto.getWeekendWorkDays() : 0);
        payroll.setReimbursement(safe(dto.getReimbursement()));
        payroll.setFbp(safe(dto.getFbp()));
        payroll.setTds(safe(dto.getTds()));
        payroll.setSalaryAdvance(safe(dto.getSalaryAdvance()));
        payroll.setOtherDeduction(safe(dto.getOtherDeduction()));
        if (dto.getRemarks() != null) payroll.setRemarks(dto.getRemarks());

        // Set attendance data for the month
        attachAttendanceSummary(payroll, employee, dto.getPayrollMonth());

        // Auto-calculate: weekendWorkAmount, PF, PT, gross, deductions, net
        calculationService.calculate(employee, payroll);

        if (payroll.getStatus() == null || payroll.getStatus() == PayrollStatus.DRAFT) {
            payroll.setStatus(PayrollStatus.PENDING);
        }

        Payroll saved = payrollRepository.save(payroll);
        log.info("Payroll saved: {} | {} | Net: {}",
            employee.getEmployeeId(), dto.getPayrollMonth(), saved.getNetSalary());
        return saved;
    }

    // ── NEW: Lock Month 
   
    @Transactional
    public int lockMonth(LocalDate month, String lockedBy) {
    	List<Payroll> payrolls =
    		    payrollRepository.findByPayrollMonthWithEmployee(month);

        for (Payroll p : payrolls) {
            p.setStatus(PayrollStatus.LOCKED);
            p.setApprovedBy(lockedBy);
        }
        payrollRepository.saveAll(payrolls);
        log.info("Locked {} payroll records for {}", payrolls.size(), month);
        return payrolls.size();
    }

    public boolean isMonthLocked(LocalDate month) {
        return payrollRepository.existsByPayrollMonthAndStatus(month, PayrollStatus.LOCKED);
    }

    // ── NEW: Monthly Payroll Report
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPayrollReport(LocalDate month) {
    	List<Payroll> payrolls =
    		    payrollRepository.findByPayrollMonthWithEmployee(month);

        List<Map<String, Object>> report = new ArrayList<>();
        for (Payroll p : payrolls) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId",       p.getEmployee().getEmployeeId());
            row.put("employeeName",     p.getEmployee().getFullName());
            row.put("department",       p.getEmployee().getDepartment());
            row.put("employeeType",     p.getEmployee().getEmployeeType() != null
                                            ? p.getEmployee().getEmployeeType().name() : "");
            row.put("month",            p.getPayrollMonth().toString());
            row.put("basicSalary",      p.getBasicSalary());
            row.put("hra",              p.getHra());
            row.put("specialAllowance", p.getSpecialAllowance());
            row.put("arrears",          p.getArrears());
            row.put("perfPay",          p.getPerfPay());
            row.put("weekendWorkDays",  p.getWeekendWorkDays());
            row.put("weekendWorkAmount",p.getWeekendWorkAmount());
            row.put("reimbursement",    p.getReimbursement());
            row.put("fbp",              p.getFbp());
            row.put("grossSalary",      p.getGrossSalary());
            row.put("pfEmployee",       p.getPfEmployee());
            row.put("professionalTax",  p.getProfessionalTax());
            row.put("tds",              p.getTds());
            row.put("salaryAdvance",    p.getSalaryAdvance());
            row.put("otherDeduction",   p.getOtherDeduction());
            row.put("totalDeductions",  p.getTotalDeductions());
            row.put("netSalary",        p.getNetSalary());
            row.put("status",           p.getStatus().name());
            row.put("remarks",          p.getRemarks() != null ? p.getRemarks() : "");
            report.add(row);
        }
        return report;
    }

    @Transactional
    public void generatePayrollForAllEmployees(LocalDate month) {
        if (isMonthLocked(month)) {
            throw new IllegalStateException("Payroll for this month is already locked.");
        }
        Page<Employee> activePage = employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.ACTIVE,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by("firstName")));

        for (Employee employee : activePage.getContent()) {
            try {
                generatePayrollForEmployee(employee, month);
            } catch (Exception e) {
                log.error("Payroll generation failed for {}: {}",
                    employee.getEmployeeId(), e.getMessage());
            }
        }
        log.info("Payroll generation completed for month: {}", month);
    }

    @Transactional
    public Payroll generatePayrollForEmployee(Employee employee, LocalDate month) {
        if (payrollRepository.existsByEmployeeIdAndPayrollMonth(employee.getId(), month)) {
            return payrollRepository
                .findByEmployeeIdAndPayrollMonth(employee.getId(), month)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));
        }

        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setPayrollMonth(month);
        payroll.setBankAccount(employee.getAccountNo());
        payroll.setIfscCode(employee.getIfscCode());
        payroll.setStatus(PayrollStatus.PENDING);
        payroll.setProcessedBy("ADMIN");

        attachAttendanceSummary(payroll, employee, month);
        calculationService.calculate(employee, payroll);
        return payrollRepository.save(payroll);
    }

    // ── STEP 2: Approve
    @Transactional
    public Payroll approvePayroll(Long payrollId, String approvedBy) {
        Payroll payroll = getPayrollById(payrollId);
        if (payroll.getStatus() == PayrollStatus.LOCKED) {
            throw new IllegalStateException("Cannot modify a LOCKED payroll.");
        }
        if (payroll.getStatus() != PayrollStatus.PENDING) {
            throw new IllegalStateException(
                "Only PENDING payroll can be approved. Current: " + payroll.getStatus());
        }
        payroll.setStatus(PayrollStatus.APPROVED);
        payroll.setApprovedBy(approvedBy);
        return payrollRepository.save(payroll);
    }

    @Transactional
    public int approveAllPendingPayrolls(LocalDate month, String approvedBy) {
        List<Payroll> pending = payrollRepository
            .findByPayrollMonthAndStatus(month, PayrollStatus.PENDING);
        pending.forEach(p -> { p.setStatus(PayrollStatus.APPROVED); p.setApprovedBy(approvedBy); });
        payrollRepository.saveAll(pending);
        return pending.size();
    }

    @Transactional
    public Payroll manualProcessPayment(Long payrollId, String processedBy) {
        Payroll payroll = getPayrollById(payrollId);
        if (payroll.getStatus() == PayrollStatus.LOCKED) {
            throw new IllegalStateException("Cannot modify a LOCKED payroll.");
        }
        if (payroll.getStatus() != PayrollStatus.APPROVED) {
            throw new IllegalStateException(
                "Only APPROVED payroll can be processed. Current: " + payroll.getStatus());
        }
        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());
        payroll.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payroll.setPaymentMode("NEFT");
        payroll.setProcessedBy(processedBy);
        try {
            // Generate payslip PDF and store a web-accessible URL
            File pdf = payslipPdfService.generatePayslip(payroll);
            String fileName = pdf.getName();
            payroll.setPayslipUrl("/uploads/payslips/" + fileName);
        } catch (Exception e) {
            log.warn("Payslip PDF generation failed for payroll {}: {}", payrollId, e.getMessage());
        }
        return payrollRepository.save(payroll);
    }

    public record BulkPaymentResult(int total, int success, int failed) {}

    @Transactional
    public BulkPaymentResult processAllApprovedPayrolls(LocalDate month) {
        List<Payroll> approved = payrollRepository
            .findByPayrollMonthAndStatus(month, PayrollStatus.APPROVED);
        int success = 0, failed = 0;
        for (Payroll p : approved) {
            try {
                p.setStatus(PayrollStatus.PAID);
                p.setPaymentDate(LocalDate.now());
                p.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                p.setPaymentMode("NEFT");
                payrollRepository.save(p);
                success++;
            } catch (Exception e) {
                log.error("Payment failed for payroll {}: {}", p.getId(), e.getMessage());
                p.setStatus(PayrollStatus.FAILED);
                payrollRepository.save(p);
                failed++;
            }
        }
        return new BulkPaymentResult(approved.size(), success, failed);
    }

    @Transactional
    public Payroll holdPayroll(Long id, String reason) {
        Payroll payroll = getPayrollById(id);
        if (payroll.getStatus() == PayrollStatus.LOCKED) {
            throw new IllegalStateException("Cannot modify a LOCKED payroll.");
        }
        payroll.setStatus(PayrollStatus.ON_HOLD);
        payroll.setRemarks(reason);
        return payrollRepository.save(payroll);
    }

    @Transactional
    public Payroll retryFailedPayment(Long id) {
        Payroll payroll = getPayrollById(id);
        if (payroll.getStatus() != PayrollStatus.FAILED) {
            throw new IllegalStateException("Only FAILED payroll can be retried.");
        }
        payroll.setStatus(PayrollStatus.APPROVED);
        return payrollRepository.save(payroll);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void attachAttendanceSummary(Payroll payroll, Employee employee, LocalDate month) {
        YearMonth ym       = YearMonth.of(month.getYear(), month.getMonth());
        LocalDate start    = ym.atDay(1);
        LocalDate end      = ym.atEndOfMonth();
        int workingDays    = calculateWorkingDays(start, end);

        List<Attendance> attendances = attendanceRepository
            .findByEmployeeIdAndAttendanceDateBetween(employee.getId(), start, end);

        long present = attendances.stream()
            .filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                      || a.getStatus() == AttendanceStatus.WORK_FROM_HOME
                      || a.getStatus() == AttendanceStatus.HALF_DAY).count();
        long onLeave = attendances.stream()
            .filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();
        long absent  = Math.max(workingDays - present - onLeave, 0);

        payroll.setWorkingDays(workingDays);
        payroll.setPresentDays((int) present);
        payroll.setLeaveDays((int) onLeave);
        payroll.setAbsentDays((int) absent);
    }

    private int calculateWorkingDays(LocalDate start, LocalDate end) {
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            int dow = d.getDayOfWeek().getValue();
            if (dow != 6 && dow != 7) count++;
        }
        return count;
    }

    private Payroll getPayrollById(Long id) {
        return payrollRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Payroll not found: " + id));
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
    public Page<Map<String, Object>> getPayrollReport(
            LocalDate month,
            String name,
            String employeeId,
            String department,
            String employeeType,
            int page,
            int size
    ) {
        List<Map<String, Object>> fullList = getPayrollReport(month); // existing

        List<Map<String, Object>> filtered = fullList.stream()
            .filter(row ->
                (name == null || name.isEmpty() ||
                    row.get("employeeName").toString().toLowerCase().contains(name.toLowerCase()))
            )
            .filter(row ->
                (employeeId == null || employeeId.isEmpty() ||
                    row.get("employeeId").toString().toLowerCase().contains(employeeId.toLowerCase()))
            )
            .filter(row ->
                (department == null || department.isEmpty() ||
                    row.get("department").toString().toLowerCase().contains(department.toLowerCase()))
            )
            .filter(row ->
                (employeeType == null || employeeType.isEmpty() ||
                    row.get("employeeType").toString().equalsIgnoreCase(employeeType))
            )
            .toList();

        int start = page * size;
        int end = Math.min(start + size, filtered.size());

        List<Map<String, Object>> pageContent =
            start > filtered.size() ? List.of() : filtered.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());
    }
}