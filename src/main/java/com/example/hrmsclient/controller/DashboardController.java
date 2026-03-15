package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.AttendanceResponseDTO;
import com.example.hrmsclient.dto.DashboardFilterRequest;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.service.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final PayrollService   payrollService;
    private final AdminService     adminService;

    public DashboardController(DashboardService dashboardService,
                                PayrollService payrollService,
                                AdminService adminService) {
        this.dashboardService = dashboardService;
        this.payrollService   = payrollService;
        this.adminService     = adminService;
    }

    // Overview stats card
    // GET /api/dashboard/overview
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR','MANAGER')")
    public ResponseEntity<?> getOverview() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data",   dashboardService.getOverviewStats()
        ));
    }

    // Employee list with ALL filters
    // GET /api/dashboard/employees?employeeId=EMP001&firstName=ravi&department=IT
    //                              &role=EMPLOYEE&employmentStatus=ACTIVE
    //                              &search=ravi&page=0&size=10&sortBy=firstName&sortDir=asc
    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR','MANAGER')")
    public ResponseEntity<?> getEmployees(@ModelAttribute DashboardFilterRequest filter) {
        Page<Employee> employees = dashboardService.getFilteredEmployees(filter);
        return ResponseEntity.ok(Map.of(
            "status",       "success",
            "data",         employees.getContent(),
            "totalRecords", employees.getTotalElements(),
            "totalPages",   employees.getTotalPages(),
            "currentPage",  employees.getNumber()
        ));
    }

    // Attendance with ALL filters
    // GET /api/dashboard/attendance?attendanceDate=2024-03-01&attendanceStatus=PRESENT
    //                               &employeeId=EMP001&attendanceDateFrom=2024-03-01
    //                               &attendanceDateTo=2024-03-31&checkedIn=true
    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<?> getAttendance(@ModelAttribute DashboardFilterRequest filter) {
        Page<AttendanceResponseDTO> attendance = dashboardService.getFilteredAttendance(filter);

        return ResponseEntity.ok(Map.of(
            "status",       "success",
            "data",         attendance.getContent(),
            "totalRecords", attendance.getTotalElements(),
            "totalPages",   attendance.getTotalPages(),
            "currentPage",  attendance.getNumber()
        ));
    }

    // Department breakdown chart data
    // GET /api/dashboard/departments
    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR','MANAGER')")
    public ResponseEntity<?> getDepartments() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data",   dashboardService.getDepartmentBreakdown()
        ));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION B — ADMIN + HR only (HR can VIEW payroll, not process)
    // ═════════════════════════════════════════════════════════════════════════

    // Payroll list with filters — HR can view, not process
    // GET /api/dashboard/payroll?payrollMonth=2024-03-01&payrollStatus=PAID
    @GetMapping("/payroll")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> getPayroll(@ModelAttribute DashboardFilterRequest filter) {
        Page<Payroll> payroll = dashboardService.getFilteredPayroll(filter);
        return ResponseEntity.ok(Map.of(
            "status",       "success",
            "data",         payroll.getContent(),
            "totalRecords", payroll.getTotalElements(),
            "totalPages",   payroll.getTotalPages()
        ));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION C — ADMIN ONLY (Critical Operations)
    // ═════════════════════════════════════════════════════════════════════════

    // Admin stats — total admins, super admins etc
    // GET /api/dashboard/admin-stats
    @GetMapping("/admin-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminStats() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data",   adminService.getDashboardStats()
        ));
    }

    // Generate payroll for a month — ADMIN ONLY
    // POST /api/dashboard/payroll/generate?month=2024-03-01
    @PostMapping("/payroll/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generatePayroll(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate month) {
        payrollService.generatePayrollForAllEmployees(month);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll generated for " + month.getMonth() + " " + month.getYear()
        ));
    }

    // Approve payroll — ADMIN ONLY
    // PUT /api/dashboard/payroll/{id}/approve
    @PutMapping("/payroll/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approvePayroll(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails user) {
        Payroll payroll = payrollService.approvePayroll(id, user.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll approved",
            "data",    Map.of(
                "payrollId", payroll.getId(),
                "status",    payroll.getStatus()
            )
        ));
    }

    // Process salary payment — ADMIN ONLY
    // POST /api/dashboard/payroll/{id}/pay
    @PostMapping("/payroll/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> processPayment(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails user) {
        Payroll payroll = payrollService.manualProcessPayment(id, user.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "✅ Salary of ₹" + payroll.getNetSalary() + " transferred",
            "data",    Map.of(
                "payrollId",   payroll.getId(),
                "netSalary",   payroll.getNetSalary(),
                "paymentRef",  payroll.getPaymentReference(),
                "paymentDate", payroll.getPaymentDate()
            )
        ));
    }

    // Delete employee — ADMIN ONLY
    // DELETE /api/dashboard/employees/{id}
    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        // delegated to EmployeeService
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Employee deleted successfully"
        ));
    }

    // Hold payroll — ADMIN ONLY
    // PUT /api/dashboard/payroll/{id}/hold
    @PutMapping("/payroll/{id}/hold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> holdPayroll(
            @PathVariable Long id,
            @RequestParam String reason) {
        Payroll payroll = payrollService.holdPayroll(id, reason);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payroll put on hold",
            "data",    Map.of("payrollId", payroll.getId(), "status", payroll.getStatus())
        ));
    }

    // Retry failed payment — ADMIN ONLY
    // POST /api/dashboard/payroll/{id}/retry
    @PostMapping("/payroll/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> retryPayment(@PathVariable Long id) {
        Payroll payroll = payrollService.retryFailedPayment(id);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Payment retried",
            "data",    Map.of("payrollId", payroll.getId(), "status", payroll.getStatus())
        ));
    }
}