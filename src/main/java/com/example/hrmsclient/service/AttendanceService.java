package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.AttendanceRequestDTO;
import com.example.hrmsclient.dto.CheckInRequestDTO;
import com.example.hrmsclient.dto.EditAttendanceRequestDTO;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.AdminRepository;
import com.example.hrmsclient.repository.AttendanceRepository;
import com.example.hrmsclient.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository   attendanceRepository;
    private final EmployeeRepository     employeeRepository;
    private final AdminRepository        adminRepository;
    private final AttendanceEmailService attendanceEmailService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             AdminRepository adminRepository,
                             AttendanceEmailService attendanceEmailService) {
        this.attendanceRepository   = attendanceRepository;
        this.employeeRepository     = employeeRepository;
        this.adminRepository        = adminRepository;
        this.attendanceEmailService = attendanceEmailService;
    }

    // ── Helper: resolve role safely ───────────────────────────────────────────
    private boolean isManager(Employee user) {
        return user != null && "MANAGER".equalsIgnoreCase(user.getRole());
    }

    // ── Employee Self Check-In ────────────────────────────────────────────────
    @Transactional
    public Attendance checkIn(String email, CheckInRequestDTO request) {
        Employee employee = getEmployeeByEmail(email);
        if (attendanceRepository.existsByEmployeeIdAndAttendanceDate(
                employee.getId(), LocalDate.now())) {
            throw new IllegalStateException("Already checked in today");
        }
        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(LocalDate.now());
        attendance.setCheckIn(LocalDateTime.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setCheckInLatitude(request.getLatitude());
        attendance.setCheckInLongitude(request.getLongitude());
        attendance.setCheckInAddress(request.getAddress());
        attendance.setLoginPhotoUrl(request.getLoginPhotoUrl());
        Attendance saved = attendanceRepository.save(attendance);
        attendanceEmailService.sendCheckInEmail(employee, saved);
        return saved;
    }

    // ── Employee Self Check-Out ───────────────────────────────────────────────
    @Transactional
    public Attendance checkOut(String email) {
        Employee employee = getEmployeeByEmail(email);
        Attendance attendance = attendanceRepository
            .findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now())
            .orElseThrow(() -> new IllegalStateException("No check-in found for today"));
        if (attendance.getCheckOut() != null) {
            throw new IllegalStateException("Already checked out today");
        }
        attendance.setCheckOut(LocalDateTime.now());
        Attendance saved = attendanceRepository.save(attendance);
        attendanceEmailService.sendCheckOutEmail(employee, saved);
        return saved;
    }

    // ── Get Today's Attendance ────────────────────────────────────────────────
    public Attendance getTodayAttendance(String email) {
        Employee employee = getEmployeeByEmail(email);
        return attendanceRepository
            .findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now())
            .orElse(null);
    }

    // ── Admin Manual Entry ────────────────────────────────────────────────────
    @Transactional
    public Attendance manualEntry(AttendanceRequestDTO request) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(request.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Employee not found: " + request.getEmployeeId()));
        Attendance attendance = attendanceRepository
            .findByEmployeeIdAndAttendanceDate(employee.getId(), request.getDate())
            .orElse(new Attendance());
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(request.getDate());
        attendance.setStatus(request.getStatus());
        if (request.getInTime()  != null) attendance.setCheckIn(request.getInTime());
        if (request.getOutTime() != null) attendance.setCheckOut(request.getOutTime());
        if (request.getRemarks() != null) attendance.setRemarks(request.getRemarks());
        Attendance saved = attendanceRepository.save(attendance);
        log.info("Manual attendance: {} | {} | {}", employee.getEmployeeId(), request.getDate(), request.getStatus());
        return saved;
    }

    // ── Edit Attendance with Full Audit Trail ─────────────────────────────────
    @Transactional
    public Attendance editAttendance(Long attendanceId,
                                     EditAttendanceRequestDTO request,
                                     String editorEmail) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
            .orElseThrow(() -> new RuntimeException("Attendance record not found: " + attendanceId));

        // Resolve editor name and role
        String editorName = "Unknown";
        String editorRole = "UNKNOWN";
        var adminOpt = adminRepository.findByEmailId(editorEmail);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            editorName = admin.getFirstName() + " " + admin.getLastName();
            editorRole = admin.getRole();
        } else {
            var empOpt = employeeRepository.findByEmailIdAndDeletedFalse(editorEmail);
            if (empOpt.isPresent()) {
                Employee emp = empOpt.get();
                editorName = emp.getFullName();
                editorRole = emp.getRole();
            }
        }

        // Save original status only on first edit
        if (attendance.getOriginalStatus() == null) {
            attendance.setOriginalStatus(
                attendance.getStatus() != null ? attendance.getStatus().name() : "NOT_MARKED"
            );
        }

        // Apply edits
        attendance.setStatus(request.getStatus());
        if (request.getCheckIn()  != null) attendance.setCheckIn(request.getCheckIn());
        if (request.getCheckOut() != null) attendance.setCheckOut(request.getCheckOut());
        if (request.getRemarks()  != null) attendance.setRemarks(request.getRemarks());

        // Set audit fields
        attendance.setLastEditedBy(editorEmail);
        attendance.setLastEditedByName(editorName);
        attendance.setLastEditedByRole(editorRole);
        attendance.setLastEditedAt(LocalDateTime.now());
        attendance.setEditReason(request.getReason());
        attendance.setEditCount(attendance.getEditCount() == null ? 1 : attendance.getEditCount() + 1);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Attendance EDITED | ID:{} | Emp:{} | Date:{} | Status:{} | By:{} ({}) | Reason:{}",
                attendanceId, attendance.getEmployee().getEmployeeId(),
                attendance.getAttendanceDate(), request.getStatus(),
                editorEmail, editorRole, request.getReason());
        return saved;
    }

    // ── Get ALL edited records — scoped by role ───────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllEditHistory(LocalDate from, LocalDate to, Employee user) {
        return attendanceRepository
            .findEditedAttendanceWithEmployee(from, to)
            .stream()
            .filter(a -> a.getLastEditedAt() != null)
            // MANAGER: show only edits belonging to their team
            .filter(a -> !isManager(user) ||
                    (a.getEmployee().getManager() != null &&
                     a.getEmployee().getManager().getId().equals(user.getId())))
            .sorted(Comparator.comparing(Attendance::getLastEditedAt).reversed())
            .map(a -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("attendanceId",   a.getId());
                row.put("employeeId",     a.getEmployee().getEmployeeId());
                row.put("employeeName",   a.getEmployee().getFullName());
                row.put("department",     a.getEmployee().getDepartment());
                row.put("date",           a.getAttendanceDate().toString());
                row.put("currentStatus",  a.getStatus().name());
                row.put("originalStatus", a.getOriginalStatus() != null ? a.getOriginalStatus() : "N/A");
                row.put("editedBy",       a.getLastEditedBy());
                row.put("editedByName",   a.getLastEditedByName());
                row.put("editedByRole",   a.getLastEditedByRole());
                row.put("editedAt",       a.getLastEditedAt().toString());
                row.put("reason",         a.getEditReason());
                row.put("editCount",      a.getEditCount());
                row.put("checkIn",        a.getCheckIn()  != null ? a.getCheckIn().toString()  : "");
                row.put("checkOut",       a.getCheckOut() != null ? a.getCheckOut().toString() : "");
                return row;
            })
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAttendanceByDate(LocalDate date, Employee user) {

        List<Employee> activeEmployees;
        if (isManager(user)) {
            activeEmployees = employeeRepository
                .findByManagerIdAndEmploymentStatusAndDeletedFalse(
                        user.getId(), EmploymentStatus.ACTIVE,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
        } else {
            activeEmployees = employeeRepository
                .findByEmploymentStatusAndDeletedFalse(EmploymentStatus.ACTIVE,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
        }

        List<Attendance> records = attendanceRepository.findByAttendanceDateWithEmployee(date);
        Map<Long, Attendance> recordMap = records.stream()
            .collect(Collectors.toMap(a -> a.getEmployee().getId(), a -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : activeEmployees) {
            Attendance att = recordMap.get(emp.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId",   emp.getEmployeeId());
            row.put("employeeName", emp.getFullName());
            row.put("department",   emp.getDepartment());
            row.put("employeeType", emp.getEmployeeType() != null ? emp.getEmployeeType().name() : "");
            row.put("date",         date.toString());
            if (att != null) {
                row.put("attendanceId",  att.getId());
                row.put("status",        att.getStatus().name());
                row.put("checkIn",       att.getCheckIn()  != null ? att.getCheckIn().toString()  : "");
                row.put("checkOut",      att.getCheckOut() != null ? att.getCheckOut().toString() : "");
                row.put("workingHours",  att.getWorkingHours());
                row.put("remarks",       att.getRemarks()  != null ? att.getRemarks() : "");
                row.put("isEdited",      att.getLastEditedAt() != null);
                row.put("editedByName",  att.getLastEditedByName() != null ? att.getLastEditedByName() : "");
                row.put("editedByRole",  att.getLastEditedByRole() != null ? att.getLastEditedByRole() : "");
                row.put("editedAt",      att.getLastEditedAt() != null ? att.getLastEditedAt().toString() : "");
                row.put("editCount",     att.getEditCount() != null ? att.getEditCount() : 0);
            } else {
                row.put("attendanceId",  null);
                row.put("status",        "NOT_MARKED");
                row.put("checkIn",       "");
                row.put("checkOut",      "");
                row.put("workingHours",  "");
                row.put("remarks",       "");
                row.put("isEdited",      false);
                row.put("editedByName",  "");
                row.put("editedByRole",  "");
                row.put("editedAt",      "");
                row.put("editCount",     0);
            }
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getDailyReport(
            LocalDate date,
            String name,
            String employeeId,
            String department,
            String employeeType,
            Employee user          
    ) {
        // getAttendanceByDate already applies the manager filter
        List<Map<String, Object>> data = getAttendanceByDate(date, user);

        return data.stream()
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
            .collect(Collectors.toList());
    }

   
    public List<Map<String, Object>> getMonthlyReport(LocalDate month, Employee user) {
        YearMonth ym    = YearMonth.of(month.getYear(), month.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        // MANAGER: only their assigned active employees
        // ADMIN/HR: all active employees
        List<Employee> employees;
        if (isManager(user)) {
            employees = employeeRepository
                .findByManagerIdAndEmploymentStatusAndDeletedFalse(
                        user.getId(), EmploymentStatus.ACTIVE,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
        } else {
            employees = employeeRepository
                .findByEmploymentStatusAndDeletedFalse(EmploymentStatus.ACTIVE,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
        }

        List<Attendance> allRecords = attendanceRepository
            .findByAttendanceDateBetween(start, end, Pageable.unpaged())
            .getContent();

        Map<Long, List<Attendance>> byEmp = allRecords.stream()
            .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : employees) {
            List<Attendance> empRecs = byEmp.getOrDefault(emp.getId(), Collections.emptyList());
            long present = empRecs.stream().filter(a ->
                a.getStatus() == AttendanceStatus.PRESENT ||
                a.getStatus() == AttendanceStatus.WORK_FROM_HOME).count();
            long halfDay = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
            long onLeave = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();
            long absent  = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
            long edited  = empRecs.stream().filter(a -> a.getLastEditedAt() != null).count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId",   emp.getEmployeeId());
            row.put("employeeName", emp.getFullName());
            row.put("department",   emp.getDepartment());
            row.put("employeeType", emp.getEmployeeType() != null ? emp.getEmployeeType().name() : "");
            row.put("month",        ym.toString());
            row.put("present",      present);
            row.put("halfDay",      halfDay);
            row.put("onLeave",      onLeave);
            row.put("absent",       absent);
            row.put("totalMarked",  empRecs.size());
            row.put("totalEdited",  edited);
            result.add(row);
        }
        return result;
    }

   
    private Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmailIdAndDeletedFalse(email)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + email));
    }
}