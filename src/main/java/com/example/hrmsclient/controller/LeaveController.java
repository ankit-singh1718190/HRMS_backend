package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.repository.EmployeeRepository;
import com.example.hrmsclient.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;
    private final EmployeeRepository employeeRepository;

    public LeaveController(LeaveService leaveService, EmployeeRepository employeeRepository) {
        this.leaveService = leaveService;
        this.employeeRepository = employeeRepository;
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
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        if (auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))) {
            employeeRepository.findByEmailId(auth.getName())
                    .filter(emp -> emp.getId().equals(empId))
                    .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("You can only view your own leaves"));
        }
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.getLeavesByEmployee(empId, page, size), "Success"));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','HR','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponseDTO<LeaveResponseDTO>>> getPending(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.getPendingLeaves(page, size), "Success"));
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

    @GetMapping("/report/balance")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> getLeaveBalanceReport() {
        List<Map<String, Object>> report = leaveService.getLeaveBalanceReport();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data",   report,
            "total",  report.size()
        ));
    }
}