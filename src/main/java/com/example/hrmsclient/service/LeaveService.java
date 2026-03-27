package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;
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
    private final LeavePolicyService      leavePolicyService;

    private static final Logger log = Logger.getLogger(LeaveService.class.getName());

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        EmployeeRepository employeeRepository,
                        HrmsEmailService hrmsEmailService,
                        LeavePolicyService leavePolicyService) {

        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository     = employeeRepository;
        this.hrmsEmailService       = hrmsEmailService;
        this.leavePolicyService     = leavePolicyService;
    }

    // ── Helper: check if logged-in user is a Manager ──────────────────────────
    private boolean isManager(Employee user) {
        return user != null && "MANAGER".equalsIgnoreCase(user.getRole());
    }

    // ── Apply Leave ───────────────────────────────────────────────────────────
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

    // ── Approve Leave ─────────────────────────────────────────────────────────
    @Transactional
    public LeaveResponseDTO approveLeave(Long id) {

        LeaveRequest request = getById(id);

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING leave can be approved");
        }

        request.setStatus(LeaveStatus.APPROVED);
        request.setApprovedAt(java.time.LocalDateTime.now());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String approvedBy = (auth != null) ? auth.getName() : "SYSTEM";
        request.setApprovedBy(approvedBy);

        LeaveRequest saved = leaveRequestRepository.save(request);

        try {
            hrmsEmailService.sendLeaveApprovedEmailAsync(
                saved.getEmployee().getEmailId(),
                saved.getEmployee().getFullName(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getLeaveType(),
                approvedBy
            );
        } catch (Exception ex) {
            log.severe("Email failed: " + ex.getMessage());
        }

        return toDTO(saved);
    }

    // ── Reject Leave ──────────────────────────────────────────────────────────
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

    // ── Get My Leave Balance ──────────────────────────────────────────────────
    public Map<String, Object> getMyLeaveBalance(Long empDbId) {

        Employee emp = employeeRepository.findByIdAndDeletedFalse(empDbId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        int fyYear = getCurrentFYYear();
        LocalDate fyStart = LocalDate.of(fyYear, 4, 1);
        LocalDate fyEnd   = LocalDate.of(fyYear + 1, 3, 31);

        int plannedTotal = leavePolicyService.getLeaveDays(emp.getEmployeeType(), "Planned");
        int sickTotal    = leavePolicyService.getLeaveDays(emp.getEmployeeType(), "Sick");

        long plannedUsed = leaveRequestRepository.countApprovedLeavesByType(emp.getId(), "Planned", fyStart, fyEnd);
        long sickUsed    = leaveRequestRepository.countApprovedLeavesByType(emp.getId(), "Sick", fyStart, fyEnd);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", emp.getEmployeeId());
        result.put("employeeName", emp.getFullName());
        result.put("employeeType", emp.getEmployeeType().name());
        result.put("fyYear", fyYear);

        result.put("plannedTotal", plannedTotal);
        result.put("plannedUsed", plannedUsed);
        result.put("plannedRemaining", Math.max(plannedTotal - plannedUsed, 0));

        result.put("sickTotal", sickTotal);
        result.put("sickUsed", sickUsed);
        result.put("sickRemaining", Math.max(sickTotal - sickUsed, 0));

        return result;
    }

    // ── Leave Balance Report ──────────────────────────────────────────────────
    public List<Map<String, Object>> getLeaveBalanceReport() {

        List<Employee> employees = employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.ACTIVE,
                PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();

        List<Map<String, Object>> report = new ArrayList<>();

        for (Employee emp : employees) {

            int fyYear = getCurrentFYYear();
            LocalDate fyStart = LocalDate.of(fyYear, 4, 1);
            LocalDate fyEnd   = LocalDate.of(fyYear + 1, 3, 31);

            int plannedEntitlement = leavePolicyService.getLeaveDays(emp.getEmployeeType(), "Planned");
            int sickEntitlement    = leavePolicyService.getLeaveDays(emp.getEmployeeType(), "Sick");

            long plannedUsed = leaveRequestRepository.countApprovedLeavesByType(emp.getId(), "Planned", fyStart, fyEnd);
            long sickUsed    = leaveRequestRepository.countApprovedLeavesByType(emp.getId(), "Sick", fyStart, fyEnd);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId",   emp.getEmployeeId());
            row.put("employeeName", emp.getFullName());
            row.put("department",   emp.getDepartment());
            row.put("employeeType", emp.getEmployeeType().name());

            row.put("plannedOpeningBalance", plannedEntitlement);
            row.put("plannedAvailed",        plannedUsed);
            row.put("plannedClosingBalance", Math.max(plannedEntitlement - plannedUsed, 0));

            row.put("sickOpeningBalance", sickEntitlement);
            row.put("sickAvailed",        sickUsed);
            row.put("sickClosingBalance", Math.max(sickEntitlement - sickUsed, 0));

            report.add(row);
        }

        return report;
    }

    public List<Map<String, Object>> getLeaveBalanceReport(
            String name,
            String employeeId,
            String department,
            String employeeType
    ) {
        return getLeaveBalanceReport().stream()
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
    }

    // ── Get Leaves by Employee ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponseDTO<LeaveResponseDTO> getLeavesByEmployee(Long empId, int page, int size) {

        Page<LeaveRequest> result = leaveRequestRepository
            .findByEmployeeId(empId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return PageResponseDTO.from(result.map(this::toDTO));
    }

    // ── Pending Leaves — scoped by role ───────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponseDTO<LeaveResponseDTO> getPendingLeaves(int page, int size, Employee user) {

        // MANAGER: delegate to the existing manager-scoped method
        if (isManager(user)) {
            return getPendingLeavesForManager(user.getEmployeeId(), page, size);
        }

        // ADMIN / HR: return all pending leaves
        Page<LeaveRequest> result = leaveRequestRepository
            .findByStatus(LeaveStatus.PENDING,
                PageRequest.of(page, size, Sort.by("createdAt").ascending()));

        return PageResponseDTO.from(result.map(this::toDTO));
    }

    // ── Pending Leaves for a specific Manager (used by path-variable endpoint) ─
    @Transactional(readOnly = true)
    public PageResponseDTO<LeaveResponseDTO> getPendingLeavesForManager(
            String managerEmployeeId, int page, int size) {

        Employee manager = employeeRepository
            .findByEmployeeIdAndDeletedFalse(managerEmployeeId)
            .orElseThrow(() -> new RuntimeException("Manager not found"));

        List<Employee> reportees = employeeRepository
            .findByManagerIdAndDeletedFalse(manager.getId(), Pageable.unpaged())
            .getContent();

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

    // ── All Leaves — scoped by role ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponseDTO<LeaveResponseDTO> getAllLeaves(
            LeaveStatus status,
            String leaveType,
            String employeeName,
            int page,
            int size,
            Employee user) {                                                    // ← added user

        // MANAGER: fetch only their team's reportee IDs first, then filter
        if (isManager(user)) {
            List<Long> reporteeIds = employeeRepository
                .findByManagerIdAndDeletedFalse(user.getId(), Pageable.unpaged())
                .getContent()
                .stream()
                .map(Employee::getId)
                .toList();

            if (reporteeIds.isEmpty()) {
                return PageResponseDTO.from(Page.empty());
            }

            Page<LeaveRequest> result = leaveRequestRepository.findWithFiltersAndEmployeeIds(
                    status,
                    leaveType,
                    employeeName,
                    reporteeIds,
                    PageRequest.of(page, size, Sort.by("createdAt").descending())
            );

            return PageResponseDTO.from(result.map(this::toDTO));
        }

        // ADMIN / HR: no employee restriction — use existing filter query
        Page<LeaveRequest> result = leaveRequestRepository.findWithFilters(
                status,
                leaveType,
                employeeName,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return PageResponseDTO.from(result.map(this::toDTO));
    }

    // ── Validation ────────────────────────────────────────────────────────────
    private void validateLeaveEligibility(Employee emp, String leaveType,
                                           LocalDate start, LocalDate end) {

        EmployeeType type = emp.getEmployeeType();
        if (type == null) return;

        int entitlement = leavePolicyService.getLeaveDays(type, leaveType);

        if (entitlement <= 0) {
            throw new IllegalArgumentException(
                "Leave policy not configured for " + type + " and " + leaveType);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
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