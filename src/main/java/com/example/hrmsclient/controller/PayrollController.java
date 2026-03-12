package com.example.hrmsclient.controller;

import com.example.hrmsclient.entity.Payroll;
import com.example.hrmsclient.entity.PayrollStatus;
import com.example.hrmsclient.repository.PayrollRepository;
import com.example.hrmsclient.service.PayrollService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PayrollController
 * ─────────────────────────────────────────────────────────────────────────────
 * All endpoints are ADMIN/HR triggered manually.
 * No auto-scheduling — admin has full control over payroll flow.
 *
 * Manual Flow:
 *   STEP 1 → POST /api/payroll/generate?month=2025-03-01        Generate all
 *   STEP 2 → PUT  /api/payroll/{id}/approve                     Approve one
 *          → PUT  /api/payroll/approve-all?month=2025-03-01     Approve all
 *   STEP 3 → POST /api/payroll/{id}/process-payment             Pay one
 *          → POST /api/payroll/process-all?month=2025-03-01     Pay all
 */
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

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — GENERATE
    // POST /api/payroll/generate?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/generate")
    public ResponseEntity<?> generatePayroll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @AuthenticationPrincipal UserDetails userDetails) {

        payrollService.generatePayrollForAllEmployees(month);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll generated for " + month.getMonth() + " " + month.getYear()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2A — APPROVE SINGLE
    // PUT /api/payroll/{id}/approve
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/approve")
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

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2B — APPROVE ALL PENDING FOR A MONTH (Bulk)
    // PUT /api/payroll/approve-all?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/approve-all")
    public ResponseEntity<?> approveAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            @AuthenticationPrincipal UserDetails userDetails) {

        int count = payrollService.approveAllPendingPayrolls(month, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Bulk approved " + count + " payrolls for "
                        + month.getMonth() + " " + month.getYear(),
            "approved", count
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3A — PROCESS PAYMENT (Single)
    // POST /api/payroll/{id}/process-payment
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/process-payment")
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

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3B — PROCESS ALL APPROVED PAYMENTS FOR A MONTH (Bulk Pay)
    // POST /api/payroll/process-all?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/process-all")
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

    // ─────────────────────────────────────────────────────────────────────────
    // PUT ON HOLD
    // PUT /api/payroll/{id}/hold
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/hold")
    public ResponseEntity<?> holdPayroll(
            @PathVariable Long id,
            @RequestParam String reason) {

        Payroll payroll = payrollService.holdPayroll(id, reason);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll put on hold",
            "data",    buildPayrollSummary(payroll)
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RETRY FAILED PAYMENT
    // POST /api/payroll/{id}/retry
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retryPayment(@PathVariable Long id) {
        Payroll payroll = payrollService.retryFailedPayment(id);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payment retried successfully",
            "data",    buildPayrollSummary(payroll)
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET PAYROLL BY MONTH
    // GET /api/payroll/month?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/month")
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

    // ─────────────────────────────────────────────────────────────────────────
    // GET EMPLOYEE PAYROLL HISTORY
    // GET /api/payroll/employee/{employeeId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeePayrollHistory(
            @PathVariable Long employeeId) {

        List<Payroll> payrolls = payrollRepository.findByEmployeeId(employeeId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data",   payrolls.stream().map(this::buildPayrollSummary).toList()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET PAYROLL SUMMARY FOR A MONTH
    // GET /api/payroll/summary?month=2025-03-01
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<?> getPayrollSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data", Map.of(
                "month",        month.getMonth() + " " + month.getYear(),
                "totalPaid",    payrollRepository.countByMonthAndStatus(month, PayrollStatus.PAID),
                "totalPending", payrollRepository.countByMonthAndStatus(month, PayrollStatus.PENDING),
                "totalApproved",payrollRepository.countByMonthAndStatus(month, PayrollStatus.APPROVED),
                "totalFailed",  payrollRepository.countByMonthAndStatus(month, PayrollStatus.FAILED),
                "totalAmount",  payrollRepository.getTotalNetSalaryByMonth(month) != null
                                ? payrollRepository.getTotalNetSalaryByMonth(month) : 0
            )
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> buildPayrollSummary(Payroll p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("payrollId",       p.getId());
        map.put("employeeId",      p.getEmployee().getEmployeeId());
        map.put("employeeName",    p.getEmployee().getFullName());
        map.put("month",           p.getPayrollMonth().toString());
        map.put("basicSalary",     p.getBasicSalary());
        map.put("grossSalary",     p.getGrossSalary());
        map.put("totalDeductions", p.getTotalDeductions());
        map.put("netSalary",       p.getNetSalary());
        map.put("status",          p.getStatus().toString());
        map.put("paymentDate",     p.getPaymentDate() != null
                                        ? p.getPaymentDate().toString() : "Not Paid");
        map.put("paymentRef",      p.getPaymentReference() != null
                                        ? p.getPaymentReference() : "N/A");
        return map;
    }
}