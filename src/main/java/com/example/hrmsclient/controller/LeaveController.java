package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.LeaveStatus;
import com.example.hrmsclient.repository.EmployeeRepository;
import com.example.hrmsclient.service.LeaveBalanceService;
import com.example.hrmsclient.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService leaveBalanceService;

    public LeaveController(LeaveService leaveService,
                           EmployeeRepository employeeRepository,
                           LeaveBalanceService leaveBalanceService) {
        this.leaveService = leaveService;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceService = leaveBalanceService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> apply(
            @Valid @RequestBody LeaveRequestDTO dto) {
        return ResponseEntity.status(201)
            .body(ApiResponse.success(leaveService.applyLeave(dto), "Leave applied"));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.approveLeave(id), "Leave approved"));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> reject(
            @PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.rejectLeave(id, reason), "Leave rejected"));
    }

    @GetMapping("/employee/{empId}")
    public ResponseEntity<ApiResponse<PageResponseDTO<LeaveResponseDTO>>> getByEmployee(
            @PathVariable Long empId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        if (auth != null && auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))) {

            Employee employee = employeeRepository
                    .findByEmailIdAndDeletedFalse(auth.getName())
                    .orElseThrow(() ->
                            new org.springframework.security.access.AccessDeniedException("User not found"));

            if (!employee.getId().equals(empId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only view your own leaves");
            }
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        leaveService.getLeavesByEmployee(empId, page, size),
                        "Success"));
    }

    // ── Pending leaves — MANAGER sees only their team, ADMIN/HR see all ───────
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponseDTO<LeaveResponseDTO>>> getPending(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Employee user) {                          // ← added
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.getPendingLeaves(page, size, user), "Success")); // ← added user
    }

    @GetMapping("/pending/manager/{managerEmployeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponseDTO<LeaveResponseDTO>>> getPendingForManager(
            @PathVariable String managerEmployeeId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            ApiResponse.success(
                leaveService.getPendingLeavesForManager(managerEmployeeId, page, size),
                "Success"));
    }

    @GetMapping("/my-balance/{empId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getMyBalance(
            @PathVariable Long empId,
            @AuthenticationPrincipal Employee user) {

        if ("EMPLOYEE".equalsIgnoreCase(user.getRole()) && !user.getId().equals(empId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view your own leave balance");
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        leaveService.getMyLeaveBalance(empId),
                        "Leave balance fetched"));
    }

    @GetMapping("/report/balance")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> getLeaveBalanceReport(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String employeeType
    ) {
        List<Map<String, Object>> report =
                leaveService.getLeaveBalanceReport(name, employeeId, department, employeeType);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data", report,
            "total", report.size()
        ));
    }

    @GetMapping("/balance/summary/{empId}")
    public ResponseEntity<ApiResponse<LeaveBalanceSummaryDTO>> getMyBalanceSummary(
            @PathVariable Long empId,
            @RequestParam(defaultValue = "0") int year,
            @AuthenticationPrincipal Employee user) {

        int resolvedYear = (year == 0) ? LocalDate.now().getYear() : year;

        if ("EMPLOYEE".equalsIgnoreCase(user.getRole()) && !user.getId().equals(empId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view your own leave balance");
        }

        LeaveBalanceSummaryDTO summary = leaveBalanceService.getSummary(empId, resolvedYear);

        return ResponseEntity.ok(
                ApiResponse.success(summary, "Leave balance summary fetched"));
    }

    // ── All leaves — MANAGER sees only their team, ADMIN/HR see all ──────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponseDTO<LeaveResponseDTO>>> getAllLeaves(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) String employeeName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal Employee user) {                          // ← added
        return ResponseEntity.ok(
            ApiResponse.success(
                leaveService.getAllLeaves(status, leaveType, employeeName, page, size, user), // ← added user
                "Success"
            )
        );
    }
}