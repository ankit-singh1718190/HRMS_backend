package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.EmploymentStatus;
import com.example.hrmsclient.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ── CREATE
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> create(
            @Valid @RequestBody EmployeeRequestDTO dto) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success(employeeService.createEmployee(dto), "Employee created"));
    }

    // ── READ
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id), "Success"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDTO<EmployeeResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String dir,
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        employeeService.getAll(page, size, sortBy, dir, user),
                        "Success"
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponseDTO<EmployeeResponseDTO>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        employeeService.search(q, page, size, user),
                        "Success"
                )
        );
    }

    @GetMapping("/filter/department")
    public ResponseEntity<ApiResponse<PageResponseDTO<EmployeeResponseDTO>>> filterByDept(
            @RequestParam String dept,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        employeeService.filterByDepartment(dept, page, size, user),
                        "Success"
                )
        );
    }

    @GetMapping("/filter/status")
    public ResponseEntity<ApiResponse<PageResponseDTO<EmployeeResponseDTO>>> filterByStatus(
            @RequestParam EmploymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        employeeService.filterByStatus(status, page, size, user),
                        "Success"
                )
        );
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getActiveEmployees(
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(employeeService.getActiveEmployees(user), "Success")
        );
    }

    // Role filter (no restriction)
    @GetMapping("/filter/role")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> filterByRole(
            @RequestParam String role) {

        return ResponseEntity.ok(
                ApiResponse.success(employeeService.getByRole(role), "Success")
        );
    }

    @GetMapping("/exited")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDTO>>> getExitedEmployees(
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(employeeService.getExitEmployees(user), "Success")
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO dto) {

        return ResponseEntity.ok(
                ApiResponse.success(employeeService.updateEmployee(id, dto), "Employee updated")
        );
    }

    @PutMapping("/{id}/exit")
    public ResponseEntity<ApiResponse<EmployeeResponseDTO>> exitEmployee(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exitDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        employeeService.exitEmployee(id, exitDate),
                        "Employee exit updated"
                )
        );
    }

    // ── DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Employee deleted"));
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<String>>> getDepartments(
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(employeeService.getAllDepartments(user), "Success")
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard(
            @AuthenticationPrincipal Employee user) {

        return ResponseEntity.ok(
                ApiResponse.success(employeeService.getDashboardSummary(user), "Success")
        );
    }
}