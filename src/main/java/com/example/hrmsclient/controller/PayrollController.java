package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.PayrollRequestDTO;
import com.example.hrmsclient.entity.Payroll;
import com.example.hrmsclient.entity.PayrollStatus;
import com.example.hrmsclient.repository.PayrollRepository;
import com.example.hrmsclient.service.PayrollService;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService    payrollService;
    private final PayrollRepository payrollRepository;

    public PayrollController(PayrollService payrollService,
                             PayrollRepository payrollRepository) {
        this.payrollService    = payrollService;
        this.payrollRepository = payrollRepository;
    }

    // ── NEW: Save / Update Payroll for Active Employee 
    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> savePayroll(
            @Valid @RequestBody PayrollRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        Payroll payroll = payrollService.saveOrUpdatePayroll(dto, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll saved successfully",
            "data",    buildPayrollSummary(payroll)
        ));
    }

    // ── NEW: Lock Month 
    @PostMapping("/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> lockMonth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @AuthenticationPrincipal UserDetails userDetails) {

        int count = payrollService.lockMonth(month, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Locked " + count + " payroll records for "
                        + month.getMonth() + " " + month.getYear(),
            "locked",  count
        ));
    }

    // ── NEW: Check if Month is Locked
    @GetMapping("/locked")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<?> isMonthLocked(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        boolean locked = payrollService.isMonthLocked(month);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "month",  month.toString(),
            "locked", locked
        ));
    }

    // ── NEW: Monthly Payroll Report 
    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<?> getPayrollReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String employeeType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<Map<String, Object>> report =
            payrollService.getPayrollReport(month, name, employeeId, department, employeeType, page, size);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data", report.getContent(),
            "totalRecords", report.getTotalElements(),
            "totalPages", report.getTotalPages(),
            "currentPage", report.getNumber()
        ));
    }

    // ── STEP 1: Generate
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> generatePayroll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @AuthenticationPrincipal UserDetails userDetails) {

        payrollService.generatePayrollForAllEmployees(month);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll generated for " + month.getMonth() + " " + month.getYear()
        ));
    }

    // ── STEP 2A: Approve Single 
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approvePayroll(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Payroll payroll = payrollService.approvePayroll(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll approved successfully",
            "data",    buildPayrollSummary(payroll)
        ));
    }

    // ── STEP 2B: Approve All 
    @PutMapping("/approve-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @AuthenticationPrincipal UserDetails userDetails) {

        int count = payrollService.approveAllPendingPayrolls(month, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",   "success",
            "message",  "Bulk approved " + count + " payrolls for "
                         + month.getMonth() + " " + month.getYear(),
            "approved", count
        ));
    }

    // ── STEP 3A: Process Single
    @PostMapping("/{id}/process-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Payroll payroll = payrollService.manualProcessPayment(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Salary of Rs." + payroll.getNetSalary() + " transferred successfully",
            "data",    buildPayrollSummary(payroll)
        ));
    }

    // ── STEP 3B: Process All
    @PostMapping("/process-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        PayrollService.BulkPaymentResult result =
            payrollService.processAllApprovedPayrolls(month);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Bulk payment completed for " + month.getMonth() + " " + month.getYear(),
            "total",   result.total(),
            "success", result.success(),
            "failed",  result.failed()
        ));
    }

    // ── Hold & Retry 
    @PutMapping("/{id}/hold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> holdPayroll(@PathVariable Long id, @RequestParam String reason) {
        Payroll payroll = payrollService.holdPayroll(id, reason);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll put on hold",
            "data",    buildPayrollSummary(payroll)
        ));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> retryPayment(@PathVariable Long id) {
        Payroll payroll = payrollService.retryFailedPayment(id);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payment retried successfully",
            "data",    buildPayrollSummary(payroll)
        ));
    }

    // ── GET by Month 
    @GetMapping("/month")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPayrollByMonth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("employee.firstName"));
        Page<Payroll> payrolls  = payrollRepository.findByPayrollMonth(month, pageRequest);
        return ResponseEntity.ok(Map.of(
            "status",       "success",
            "data",         payrolls.getContent().stream().map(this::buildPayrollSummary).toList(),
            "totalRecords", payrolls.getTotalElements(),
            "totalPages",   payrolls.getTotalPages()
        ));
    }

    // ── GET Employee History (used by My Documents for employees)
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getEmployeePayrollHistory(@PathVariable Long employeeId) {
        List<Payroll> payrolls = payrollRepository.findByEmployeeId(employeeId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data",   payrolls.stream().map(this::buildPayrollSummary).toList()
        ));
    }

    // ── GET Summary
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<?> getPayrollSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data", Map.of(
                "month",         month.getMonth() + " " + month.getYear(),
                "totalPaid",     payrollRepository.countByMonthAndStatus(month, PayrollStatus.PAID),
                "totalPending",  payrollRepository.countByMonthAndStatus(month, PayrollStatus.PENDING),
                "totalApproved", payrollRepository.countByMonthAndStatus(month, PayrollStatus.APPROVED),
                "totalLocked",   payrollRepository.countByMonthAndStatus(month, PayrollStatus.LOCKED),
                "totalFailed",   payrollRepository.countByMonthAndStatus(month, PayrollStatus.FAILED),
                "totalAmount",   payrollRepository.getTotalNetSalaryByMonth(month) != null
                                     ? payrollRepository.getTotalNetSalaryByMonth(month) : 0,
                "isLocked",      payrollService.isMonthLocked(month)
            )
        ));
    }
    // payslip
    @GetMapping("/{id}/payslip")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long id) throws Exception {

        Payroll payroll = payrollRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document();
        PdfWriter.getInstance(document, out);

        document.open();
        document.add(new Paragraph("Payslip"));
        document.add(new Paragraph("Employee: " + payroll.getEmployee().getFullName()));
        document.add(new Paragraph("Month: " + payroll.getPayrollMonth()));
        document.add(new Paragraph("Net Salary: ₹" + payroll.getNetSalary()));
        document.close();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=payslip_" + id + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    // ── Helper 
    private Map<String, Object> buildPayrollSummary(Payroll p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("payrollId",        p.getId());
        map.put("employeeId",       p.getEmployee().getEmployeeId());
        map.put("employeeName",     p.getEmployee().getFullName());
        map.put("month",            p.getPayrollMonth().toString());
        // Gross components
        map.put("basicSalary",      p.getBasicSalary());
        map.put("hra",              p.getHra());
        map.put("specialAllowance", p.getSpecialAllowance());
        map.put("arrears",          p.getArrears());
        map.put("perfPay",          p.getPerfPay());
        map.put("weekendWorkDays",  p.getWeekendWorkDays());
        map.put("weekendWorkAmount",p.getWeekendWorkAmount());
        map.put("reimbursement",    p.getReimbursement());
        map.put("fbp",              p.getFbp());
        map.put("grossSalary",      p.getGrossSalary());
        // Deductions
        map.put("pfEmployee",       p.getPfEmployee());
        map.put("professionalTax",  p.getProfessionalTax());
        map.put("tds",              p.getTds());
        map.put("salaryAdvance",    p.getSalaryAdvance());
        map.put("otherDeduction",   p.getOtherDeduction());
        map.put("totalDeductions",  p.getTotalDeductions());
        map.put("netSalary",        p.getNetSalary());
        map.put("status",           p.getStatus().toString());
        map.put("remarks",          p.getRemarks() != null ? p.getRemarks() : "");
        map.put("paymentDate",      p.getPaymentDate() != null ? p.getPaymentDate().toString() : "Not Paid");
        map.put("paymentRef",       p.getPaymentReference() != null ? p.getPaymentReference() : "N/A");
        map.put("payslipUrl",       p.getPayslipUrl() != null ? p.getPayslipUrl() : "");
        return map;
    }
}