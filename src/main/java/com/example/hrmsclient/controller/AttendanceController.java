package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.AttendanceRequestDTO;
import com.example.hrmsclient.dto.CheckInRequestDTO;
import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.service.AttendanceExcelService;
import com.example.hrmsclient.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService      attendanceService;
    private final AttendanceExcelService attendanceExcelService;

    public AttendanceController(AttendanceService attendanceService,
                                AttendanceExcelService attendanceExcelService) {
        this.attendanceService      = attendanceService;
        this.attendanceExcelService = attendanceExcelService;
    }

    // ── Employee Self-Service ─────────────────────────────────────────────────

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckInRequestDTO request) {

        Attendance attendance = attendanceService.checkIn(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Check-in successful",
            "data", Map.of(
                "attendanceId", attendance.getId(),
                "checkInTime",  attendance.getCheckIn().toString(),
                "photoUrl",     attendance.getLoginPhotoUrl()      != null ? attendance.getLoginPhotoUrl()      : "",
                "latitude",     attendance.getCheckInLatitude()    != null ? attendance.getCheckInLatitude()    : 0,
                "longitude",    attendance.getCheckInLongitude()   != null ? attendance.getCheckInLongitude()   : 0,
                "address",      attendance.getCheckInAddress()     != null ? attendance.getCheckInAddress()     : "N/A"
            )
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(@AuthenticationPrincipal UserDetails userDetails) {
        Attendance attendance = attendanceService.checkOut(userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Check-out successful",
            "data", Map.of(
                "attendanceId", attendance.getId(),
                "checkInTime",  attendance.getCheckIn().toString(),
                "checkOutTime", attendance.getCheckOut().toString(),
                "workingHours", attendance.getWorkingHours()
            )
        ));
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayAttendance(
            @AuthenticationPrincipal UserDetails userDetails) {

        Attendance attendance = attendanceService.getTodayAttendance(userDetails.getUsername());
        Map<String, Object> response = new HashMap<>();
        if (attendance == null) {
            response.put("status",  "success");
            response.put("message", "No attendance for today");
            response.put("data",    null);
            return ResponseEntity.ok(response);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("attendanceId",  attendance.getId());
        data.put("checkInTime",   attendance.getCheckIn());
        data.put("checkOutTime",  attendance.getCheckOut());
        data.put("workingHours",  attendance.getWorkingHours());
        data.put("latitude",      attendance.getCheckInLatitude());
        data.put("longitude",     attendance.getCheckInLongitude());
        data.put("address",       attendance.getCheckInAddress() != null ? attendance.getCheckInAddress() : "");
        response.put("status", "success");
        response.put("data",   data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<?> getAttendanceByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Map<String, Object>> records = attendanceService.getAttendanceByDate(date);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "date",   date.toString(),
            "data",   records,
            "total",  records.size()
        ));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> manualEntry(@Valid @RequestBody AttendanceRequestDTO request) {
        Attendance attendance = attendanceService.manualEntry(request);
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Attendance marked manually",
            "data", Map.of(
                "attendanceId", attendance.getId(),
                "employeeId",   attendance.getEmployee().getEmployeeId(),
                "date",         attendance.getAttendanceDate().toString(),
                "status",       attendance.getStatus().toString(),
                "workingHours", attendance.getWorkingHours()
            )
        ));
    }

    @GetMapping("/report/daily")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> dailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate reportDate = date != null ? date : LocalDate.now();
        List<Map<String, Object>> report = attendanceService.getDailyReport(reportDate);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "date",   reportDate.toString(),
            "data",   report
        ));
    }

    @GetMapping("/report/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> monthlyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        List<Map<String, Object>> report = attendanceService.getMonthlyReport(month);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "month",  month.toString(),
            "data",   report
        ));
    }

    @GetMapping("/export/daily")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<byte[]> exportDaily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws Exception {

        LocalDate exportDate = date != null ? date : LocalDate.now();
        byte[] bytes = attendanceExcelService.exportDaily(exportDate);
        String filename = "attendance_daily_"
            + exportDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".xlsx";
        return excelResponse(bytes, filename);
    }

    @GetMapping("/export/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month)
            throws Exception {

        byte[] bytes = attendanceExcelService.exportMonthly(month);
        String filename = "attendance_monthly_"
            + month.format(DateTimeFormatter.ofPattern("MM-yyyy")) + ".xlsx";
        return excelResponse(bytes, filename);
    }

    /**
     * GET /api/attendance/export/yearly?year=2025
     * Downloads yearly attendance as Excel (13 sheets: summary + one per month)
     */
    @GetMapping("/export/yearly")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<byte[]> exportYearly(@RequestParam int year) throws Exception {
        byte[] bytes = attendanceExcelService.exportYearly(year);
        String filename = "attendance_yearly_" + year + ".xlsx";
        return excelResponse(bytes, filename);
    }

    // ── Helper 
    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .contentLength(bytes.length)
            .body(bytes);
    }
}