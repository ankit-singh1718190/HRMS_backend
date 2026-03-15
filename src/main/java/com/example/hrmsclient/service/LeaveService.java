package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

@Service
public class LeaveService {

    private final LeaveRequestRepository  leaveRequestRepository;
    private final LeaveBalanceRepository  leaveBalanceRepository;
    private final EmployeeRepository      employeeRepository;
    private final HrmsEmailService        hrmsEmailService;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        EmployeeRepository employeeRepository,
                        HrmsEmailService hrmsEmailService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository     = employeeRepository;
        this.hrmsEmailService       = hrmsEmailService;
    }

    // ── Apply Leave
    @Transactional
    public LeaveResponseDTO applyLeave(LeaveRequestDTO dto) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(dto.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Employee not found"));

        validateLeaveEligibility(employee, dto.getLeaveType(), dto.getStartDate(), dto.getEndDate());

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setLeaveType(dto.getLeaveType());
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setReason(dto.getReason());
        request.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(request);
        hrmsEmailService.sendLeaveAppliedEmail(saved);
        return toDTO(saved);
    }

    @Transactional
    public LeaveResponseDTO approveLeave(Long id) {
        LeaveRequest request = getById(id);
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING leave can be approved");
        }
        request.setStatus(LeaveStatus.APPROVED);
        request.setApprovedAt(java.time.LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        hrmsEmailService.sendLeaveApprovedEmail(saved);
        return toDTO(saved);
    }

    @Transactional
    public LeaveResponseDTO rejectLeave(Long id, String reason) {
        LeaveRequest request = getById(id);
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING leave can be rejected");
        }
        request.setStatus(LeaveStatus.REJECTED);
        request.setRejectionReason(reason);
        LeaveRequest saved = leaveRequestRepository.save(request);
        hrmsEmailService.sendLeaveRejectedEmail(saved);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<LeaveResponseDTO> getLeavesByEmployee(Long empId, int page, int size) {
        Page<LeaveRequest> result = leaveRequestRepository
            .findByEmployeeId(empId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PageResponseDTO.from(result.map(this::toDTO));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<LeaveResponseDTO> getPendingLeaves(int page, int size) {
        Page<LeaveRequest> result = leaveRequestRepository
            .findByStatus(LeaveStatus.PENDING,
                PageRequest.of(page, size, Sort.by("createdAt").ascending()));
        return PageResponseDTO.from(result.map(this::toDTO));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<LeaveResponseDTO> getPendingLeavesForManager(
            String managerEmployeeId, int page, int size) {

        // Find all employees whose reporting manager is this manager
        List<Employee> reportees = employeeRepository
            .findByReportingManagerAndDeletedFalse(managerEmployeeId);

        List<Long> reporteeIds = reportees.stream()
            .map(Employee::getId)
            .toList();

        if (reporteeIds.isEmpty()) {
        	return PageResponseDTO.from(Page.empty());
        }

        Page<LeaveRequest> result = leaveRequestRepository
            .findByStatusAndEmployeeIdIn(
                LeaveStatus.PENDING,
                reporteeIds,
                PageRequest.of(page, size, Sort.by("createdAt").ascending()));
        return PageResponseDTO.from(result.map(this::toDTO));
    }

    public List<Map<String, Object>> getLeaveBalanceReport() {
        List<Employee> employees = employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.ACTIVE,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by("firstName")))
            .getContent();

        List<Map<String, Object>> report = new ArrayList<>();

        for (Employee emp : employees) {
            int fyYear = getCurrentFYYear();

            // Get entitlements based on employee type
            int plannedEntitlement = getEntitlement(emp.getEmployeeType(), "Planned");
            int sickEntitlement    = getEntitlement(emp.getEmployeeType(), "Sick");

            // Count approved leaves taken this FY
            LocalDate fyStart = LocalDate.of(fyYear, 4, 1);
            LocalDate fyEnd   = LocalDate.of(fyYear + 1, 3, 31);

            long plannedAvailed = leaveRequestRepository.countApprovedLeavesByType(
                emp.getId(), "Planned", fyStart, fyEnd);
            long sickAvailed = leaveRequestRepository.countApprovedLeavesByType(
                emp.getId(), "Sick", fyStart, fyEnd);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId",          emp.getEmployeeId());
            row.put("employeeName",         emp.getFullName());
            row.put("department",           emp.getDepartment());
            row.put("employeeType",         emp.getEmployeeType() != null ? emp.getEmployeeType().name() : "");
            row.put("plannedOpeningBalance",plannedEntitlement);
            row.put("plannedAvailed",       plannedAvailed);
            row.put("plannedClosingBalance",Math.max(plannedEntitlement - plannedAvailed, 0));
            row.put("sickOpeningBalance",   sickEntitlement);
            row.put("sickAvailed",          sickAvailed);
            row.put("sickClosingBalance",   Math.max(sickEntitlement - sickAvailed, 0));
            report.add(row);
        }
        return report;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void validateLeaveEligibility(Employee emp, String leaveType,
                                           LocalDate start, LocalDate end) {
        EmployeeType type = emp.getEmployeeType();
        if (type == null) return; // skip for legacy records

        int entitlement = getEntitlement(type, leaveType);
        if (entitlement == 0) {
            throw new IllegalArgumentException(
                emp.getEmployeeType().name() + " employees are not entitled to " + leaveType + " leave");
        }
        // Could add balance check here — left as extension point
    }

    private int getEntitlement(EmployeeType type, String leaveType) {
        if (type == null) return 0;
        return switch (type) {
            case FULL_TIME -> leaveType.equalsIgnoreCase("Planned") ? 15 : 10;
            case CONTRACT  -> leaveType.equalsIgnoreCase("Planned") ? 10 :  6;
            case TEMPORARY -> leaveType.equalsIgnoreCase("Planned") ?  6 :  4;
            case INTERN    -> leaveType.equalsIgnoreCase("Planned") ?  5 :  3;
        };
    }

    private int getCurrentFYYear() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
    }

    private LeaveRequest getById(Long id) {
        return leaveRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Leave request not found: " + id));
    }
    private LeaveResponseDTO toDTO(LeaveRequest r) {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setId(r.getId());
        dto.setEmployeeId(r.getEmployee().getEmployeeId());   
        dto.setEmployeeName(r.getEmployee().getFullName());
        dto.setLeaveType(r.getLeaveType());
        dto.setStartDate(r.getStartDate());
        dto.setEndDate(r.getEndDate());
        dto.setTotalDays(r.getLeaveDays());                   
        dto.setPaidDays(r.getPaidDays());
        dto.setUnpaidDays(r.getUnpaidDays());
        dto.setStatus(r.getStatus());                         
        dto.setReason(r.getReason());
        dto.setRejectionReason(r.getRejectionReason());
        dto.setApprovedBy(r.getApprovedBy());
        dto.setCreatedAt(r.getCreatedAt());                   
        return dto;
    }
}