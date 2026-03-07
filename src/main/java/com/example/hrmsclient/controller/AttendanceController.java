package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.CheckInRequestDTO;
import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckInRequestDTO request) {

        Attendance attendance = attendanceService.checkIn(
                userDetails.getUsername(), request);

        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Check-in successful",
            "data", Map.of(
                "attendanceId", attendance.getId(),
                "checkInTime",  attendance.getCheckIn().toString(),
                "photoUrl",     attendance.getLoginPhotoUrl(),
                "latitude",     attendance.getCheckInLatitude(),
                "longitude",    attendance.getCheckInLongitude(),
                "address",      attendance.getCheckInAddress() != null
                                    ? attendance.getCheckInAddress() : "N/A"
            )
        ));
    }
    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(
            @AuthenticationPrincipal UserDetails userDetails) {

        Attendance attendance = attendanceService.checkOut(
                userDetails.getUsername());

        String workingHours = attendance.getWorkingHours();

        return ResponseEntity.ok(Map.of(
            "status",  "success",
            "message", "Check-out successful",
            "data", Map.of(
                "attendanceId", attendance.getId(),
                "checkInTime",  attendance.getCheckIn().toString(),
                "checkOutTime", attendance.getCheckOut().toString(),
                "workingHours", workingHours
            )
        ));
    }
    
    @GetMapping("/today")
    public ResponseEntity<?> getTodayAttendance(
            @AuthenticationPrincipal UserDetails userDetails) {

        Attendance attendance = attendanceService.getTodayAttendance(
                userDetails.getUsername());

        Map<String, Object> response = new HashMap<>();

        if (attendance == null) {
            response.put("status", "success");
            response.put("message", "No attendance for today");
            response.put("data", null);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("attendanceId", attendance.getId());
        data.put("checkInTime", attendance.getCheckIn());
        data.put("checkOutTime", attendance.getCheckOut());
        data.put("workingHours", attendance.getWorkingHours());
        data.put("latitude", attendance.getCheckInLatitude());
        data.put("longitude", attendance.getCheckInLongitude());
        data.put("address", attendance.getCheckInAddress() != null ? attendance.getCheckInAddress() : "");
        response.put("status", "success");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}