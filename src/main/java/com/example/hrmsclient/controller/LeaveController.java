package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    
    public LeaveController(LeaveService leaveService) {
    	this.leaveService=leaveService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> apply(
            @Valid @RequestBody LeaveRequestDTO dto) {
        return ResponseEntity.status(201)
            .body(ApiResponse.success(leaveService.applyLeave(dto), "Leave applied"));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.approveLeave(id), "Leave approved"));
    }

    @PatchMapping("/{id}/reject")
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
    public ResponseEntity<ApiResponse<PageResponseDTO<LeaveResponseDTO>>> getPending(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            ApiResponse.success(leaveService.getPendingLeaves(page, size), "Success"));
    }
}