package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
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
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.getLeavesByEmployee(empId, page, size), "Success"));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
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