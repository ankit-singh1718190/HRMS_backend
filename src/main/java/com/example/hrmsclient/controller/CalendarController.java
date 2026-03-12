package com.example.hrmsclient.controller;

import com.example.hrmsclient.dto.AdminCalendarDTO;
import com.example.hrmsclient.dto.ApiResponse;
import com.example.hrmsclient.dto.CalendarDayDTO;
import com.example.hrmsclient.dto.HolidayDTO;
import com.example.hrmsclient.service.CalendarService;
import com.example.hrmsclient.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final HolidayService holidayService;

    public CalendarController(CalendarService calendarService,
                              HolidayService holidayService) {
        this.calendarService = calendarService;
        this.holidayService  = holidayService;
    }

    // ── Employee Calendar ──────────────────────────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<CalendarDayDTO>>> getEmployeeCalendar(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        List<CalendarDayDTO> data = calendarService.getEmployeeCalendar(employeeId, year, month);
        return ResponseEntity.ok(ApiResponse.success(data, "Employee calendar fetched"));
    }

    // ── Admin Calendar ─────────────────────────────────────────────────────────
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<AdminCalendarDTO>> getAdminCalendar(
            @RequestParam int year,
            @RequestParam int month) {
        AdminCalendarDTO data = calendarService.getAdminCalendar(year, month);
        return ResponseEntity.ok(ApiResponse.success(data, "Admin calendar fetched"));
    }

    // ── Holiday Endpoints (Admin only) ─────────────────────────────────────────
    @PostMapping("/holidays")
    public ResponseEntity<ApiResponse<HolidayDTO>> addHoliday(
            @Valid @RequestBody HolidayDTO dto) {
        return ResponseEntity.status(201)
            .body(ApiResponse.success(holidayService.addHoliday(dto), "Holiday added"));
    }

    @PutMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<HolidayDTO>> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody HolidayDTO dto) {
        return ResponseEntity.ok(
            ApiResponse.success(holidayService.updateHoliday(id, dto), "Holiday updated"));
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Holiday deleted"));
    }

    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<List<HolidayDTO>>> getHolidays(
            @RequestParam int year) {
        return ResponseEntity.ok(
            ApiResponse.success(holidayService.getHolidaysByYear(year), "Holidays fetched"));
    }
}