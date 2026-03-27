package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.AttendanceResponseDTO;
import com.example.hrmsclient.dto.DashboardFilterRequest;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * UPDATED DashboardService
 *
 * Changes vs original:
 *   1. getOverviewStats: now includes presentToday (was missing/not working).
 *      Uses attendanceRepository.countByAttendanceDateAndStatus for today.
 *
 *   2. Removed pendingLeaves from admin overview stats (per requirements:
 *      "Need to remove Pending Leaves from admin dashboard").
 *
 *   3. Added managerPendingLeaveCount in getOverviewStats —
 *      "Manager Pending Leave Approval" tile on dashboard.
 */
@Service
public class DashboardService {

    private final EmployeeRepository    employeeRepository;
    private final AttendanceRepository  attendanceRepository;
    private final PayrollRepository     payrollRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public DashboardService(EmployeeRepository employeeRepository,
                             AttendanceRepository attendanceRepository,
                             PayrollRepository payrollRepository,
                             LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository    = employeeRepository;
        this.attendanceRepository  = attendanceRepository;
        this.payrollRepository     = payrollRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }


    public Map<String, Object> getOverviewStats() {
        LocalDate today = LocalDate.now();

        long totalEmployees  = employeeRepository.countByDeletedFalse();
        long activeEmployees = employeeRepository
            .countByEmploymentStatusAndDeletedFalse(EmploymentStatus.ACTIVE);

        // FIX: presentToday — count PRESENT + WFH records for today
        long presentToday = attendanceRepository
            .countByAttendanceDateAndStatus(today, AttendanceStatus.PRESENT)
            + attendanceRepository
            .countByAttendanceDateAndStatus(today, AttendanceStatus.WORK_FROM_HOME);

        // leaveToday — employees on approved leave today
        long leaveToday = attendanceRepository
            .countByAttendanceDateAndStatus(today, AttendanceStatus.ON_LEAVE);

        // notPresentToday = active - present - onLeave
        long notPresentToday = Math.max(activeEmployees - presentToday - leaveToday, 0);

        // managerPendingLeaveCount — total pending leaves where employee has a reporting manager
        long managerPendingLeaveCount = leaveRequestRepository
            .countPendingLeavesWithReportingManager();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEmployees",          totalEmployees);
        stats.put("activeEmployees",         activeEmployees);
        stats.put("presentToday",            presentToday);       // FIX: was missing
        stats.put("notPresentToday",         notPresentToday);
        stats.put("leaveToday",              leaveToday);
        // pendingLeaves REMOVED from admin dashboard per requirements
        stats.put("managerPendingLeaveCount",managerPendingLeaveCount); // NEW
        return stats;
    }

    // ── Employees with Filters
    public Page<Employee> getFilteredEmployees(DashboardFilterRequest f) {
        Pageable pg = pageable(f);
        if (hasValue(f.getSearch()))           return employeeRepository.searchEmployees(f.getSearch(), pg);
        if (hasValue(f.getEmployeeId()))       return employeeRepository.searchEmployees(f.getEmployeeId(), pg);
        if (hasValue(f.getFirstName()))        return employeeRepository.searchEmployees(f.getFirstName(), pg);
        if (hasValue(f.getDepartment()))       return employeeRepository.findByDepartmentIgnoreCaseAndDeletedFalse(f.getDepartment(), pg);
        if (hasValue(f.getRole()))             return employeeRepository.findByRoleIgnoreCaseAndDeletedFalse(f.getRole(), pg);
        if (hasValue(f.getEmploymentStatus())) {
            EmploymentStatus s = EmploymentStatus.valueOf(f.getEmploymentStatus().toUpperCase());
            return employeeRepository.findByEmploymentStatusAndDeletedFalse(s, pg);
        }
        return employeeRepository.findAllByDeletedFalse(pg);
    }

    // ── Attendance with Filters 
    @Transactional(readOnly = true)
    public Page<AttendanceResponseDTO> getFilteredAttendance(DashboardFilterRequest f) {
        Pageable pg   = pageable(f);
        LocalDate date = f.getAttendanceDate() != null ? f.getAttendanceDate() : LocalDate.now();

        Page<Attendance> attendancePage;
        if (f.getAttendanceDateFrom() != null && f.getAttendanceDateTo() != null) {
            attendancePage = attendanceRepository.findByAttendanceDateBetween(
                f.getAttendanceDateFrom(), f.getAttendanceDateTo(), pg);
        } else {
            attendancePage = attendanceRepository.findByAttendanceDate(date, pg);
        }
        return attendancePage.map(this::toAttendanceDTO);
    }

    // ── Department Breakdown 
    public Map<String, Long> getDepartmentBreakdown() {

        List<Employee> all = employeeRepository
                .findAllByDeletedFalse(Pageable.unpaged())
                .getContent();

        Map<String, Long> breakdown = new LinkedHashMap<>();

        for (Employee e : all) {
            breakdown.merge(e.getDepartment(), 1L, Long::sum);
        }

        return breakdown;
    }

    // ── Payroll with Filters
    public Page<Payroll> getFilteredPayroll(DashboardFilterRequest f) {
        Pageable pg = pageable(f);
        if (f.getPayrollMonth() != null && hasValue(f.getPayrollStatus())) {
            PayrollStatus s = PayrollStatus.valueOf(f.getPayrollStatus().toUpperCase());
            return payrollRepository.findByPayrollMonthAndStatus(f.getPayrollMonth(), s, pg);
        }
        if (f.getPayrollMonth() != null) {
            return payrollRepository.findByPayrollMonth(f.getPayrollMonth(), pg);
        }
        return payrollRepository.findAll(pg);
    }

    // ── Helpers
    private AttendanceResponseDTO toAttendanceDTO(Attendance a) {
        return AttendanceResponseDTO.from(a);
    }

    private Pageable pageable(DashboardFilterRequest f) {
        Sort sort = Sort.by(
            hasValue(f.getSortDir()) && f.getSortDir().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC,
            hasValue(f.getSortBy()) ? f.getSortBy() : "createdAt"
        );
        return PageRequest.of(f.getPage(), f.getSize(), sort);
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }
}