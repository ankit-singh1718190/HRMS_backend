package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.AttendanceRequestDTO;
import com.example.hrmsclient.dto.CheckInRequestDTO;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.AttendanceRepository;
import com.example.hrmsclient.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final AttendanceEmailService attendanceEmailService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             AttendanceEmailService attendanceEmailService) {
        this.attendanceRepository  = attendanceRepository;
        this.employeeRepository    = employeeRepository;
        this.attendanceEmailService = attendanceEmailService;
    }

    // ── Employee Self Check-In 
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

    // ── Employee Self Check-Out
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

    // ── Get Today's Attendance 
    public Attendance getTodayAttendance(String email) {
        Employee employee = getEmployeeByEmail(email);
        return attendanceRepository
            .findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now())
            .orElse(null);
    }

    // ── Admin Manual Entry 
    @Transactional
    public Attendance manualEntry(AttendanceRequestDTO request) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(request.getEmployeeId())
            .orElseThrow(() -> new RuntimeException(
                "Employee not found: " + request.getEmployeeId()));

        // Upsert: update if record exists, else create new
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
        log.info("Manual attendance: {} | {} | {}", employee.getEmployeeId(),
                request.getDate(), request.getStatus());
        return saved;
    }

    // ── Get Attendance by Date (Admin view) 
    public List<Map<String, Object>> getAttendanceByDate(LocalDate date) {
        List<Employee> activeEmployees = employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();

        List<Attendance> records = attendanceRepository.findByAttendanceDate(date);
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
                row.put("status",       att.getStatus().name());
                row.put("checkIn",      att.getCheckIn()  != null ? att.getCheckIn().toString()  : "");
                row.put("checkOut",     att.getCheckOut() != null ? att.getCheckOut().toString() : "");
                row.put("workingHours", att.getWorkingHours());
                row.put("remarks",      att.getRemarks()  != null ? att.getRemarks() : "");
            } else {
                row.put("status",       "NOT_MARKED");
                row.put("checkIn",      "");
                row.put("checkOut",     "");
                row.put("workingHours", "");
                row.put("remarks",      "");
            }
            result.add(row);
        }
        return result;
    }

    // ── Daily Report 
    public List<Map<String, Object>> getDailyReport(LocalDate date) {
        return getAttendanceByDate(date);
    }

    // ── Monthly Report
    public List<Map<String, Object>> getMonthlyReport(LocalDate month) {
        YearMonth ym    = YearMonth.of(month.getYear(), month.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        List<Employee> employees = employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();

        List<Attendance> allRecords =
            attendanceRepository.findByAttendanceDateBetween(start, end,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        Map<Long, List<Attendance>> byEmp = allRecords.stream()
            .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : employees) {
            List<Attendance> empRecs = byEmp.getOrDefault(emp.getId(), Collections.emptyList());
            long present = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                                 || a.getStatus() == AttendanceStatus.WORK_FROM_HOME).count();
            long halfDay = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
            long onLeave = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();
            long absent  = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();

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
            result.add(row);
        }
        return result;
    }

    // ── Helper
    private Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmailIdAndDeletedFalse(email)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + email));
    }
}