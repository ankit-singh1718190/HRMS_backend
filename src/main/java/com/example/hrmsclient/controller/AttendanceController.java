package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.AttendanceRequestDTO;
import com.example.hrmsclient.dto.CheckInRequestDTO;
import com.example.hrmsclient.dto.EditAttendanceRequestDTO;
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
                "photoUrl",     attendance.getLoginPhotoUrl()    != null ? attendance.getLoginPhotoUrl()    : "",
                "latitude",     attendance.getCheckInLatitude()  != null ? attendance.getCheckInLatitude()  : 0,
                "longitude",    attendance.getCheckInLongitude() != null ? attendance.getCheckInLongitude() : 0,
                "address",      attendance.getCheckInAddress()   != null ? attendance.getCheckInAddress()   : "N/A"
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
    public ResponseEntity<?> getTodayAttendance(@AuthenticationPrincipal UserDetails userDetails) {
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
        data.put("isEdited",      attendance.getLastEditedAt() != null);
        data.put("editedByName",  attendance.getLastEditedByName() != null ? attendance.getLastEditedByName() : "");
        data.put("editedAt",      attendance.getLastEditedAt() != null ? attendance.getLastEditedAt().toString() : "");
        response.put("status", "success");
        response.put("data",   data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date")
    public ResponseEntity<?> getAttendanceByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Map<String, Object>> records = attendanceService.getAttendanceByDate(date);
        return ResponseEntity.ok(Map.of("status", "success", "date", date.toString(), "data", records, "total", records.size()));
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

    @PutMapping("/{id}/edit")
    public ResponseEntity<?> editAttendance(
            @PathVariable Long id,
            @Valid @RequestBody EditAttendanceRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Attendance attendance = attendanceService.editAttendance(id, request, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Attendance updated successfully",
            "data", Map.of(
                "attendanceId",   attendance.getId(),
                "date",           attendance.getAttendanceDate().toString(),
                "newStatus",      attendance.getStatus().name(),
                "originalStatus", attendance.getOriginalStatus() != null ? attendance.getOriginalStatus() : "N/A",
                "editedBy",       attendance.getLastEditedByName(),
                "editedByRole",   attendance.getLastEditedByRole(),
                "editedAt",       attendance.getLastEditedAt().toString(),
                "reason",         attendance.getEditReason(),
                "editCount",      attendance.getEditCount()
            )
        ));
    }

    // ── EDIT HISTORY — Admin/HR only ──────────────────────────────────────────
    // GET /api/attendance/edit-history?from=2026-03-01&to=2026-03-31
    @GetMapping("/edit-history")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> getEditHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<Map<String, Object>> history = attendanceService.getAllEditHistory(from, to);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "from",   from.toString(),
            "to",     to.toString(),
            "total",  history.size(),
            "data",   history
        ));
    }

    @GetMapping("/report/daily")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> dailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(Map.of("status", "success", "date", reportDate.toString(),
            "data", attendanceService.getDailyReport(reportDate)));
    }

    @GetMapping("/report/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> monthlyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return ResponseEntity.ok(Map.of("status", "success", "month", month.toString(),
            "data", attendanceService.getMonthlyReport(month)));
    }

    @GetMapping("/export/daily")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<byte[]> exportDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            throws Exception {
        LocalDate exportDate = date != null ? date : LocalDate.now();
        byte[] bytes = attendanceExcelService.exportDaily(exportDate);
        return excelResponse(bytes, "attendance_daily_" + exportDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".xlsx");
    }

    @GetMapping("/export/monthly")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) throws Exception {
        byte[] bytes = attendanceExcelService.exportMonthly(month);
        return excelResponse(bytes, "attendance_monthly_" + month.format(DateTimeFormatter.ofPattern("MM-yyyy")) + ".xlsx");
    }

    @GetMapping("/export/yearly")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<byte[]> exportYearly(@RequestParam int year) throws Exception {
        return excelResponse(attendanceExcelService.exportYearly(year), "attendance_yearly_" + year + ".xlsx");
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .contentLength(bytes.length)
            .body(bytes);
    }
}