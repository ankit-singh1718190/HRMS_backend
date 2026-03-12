package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.AdminCalendarDTO;
import com.example.hrmsclient.dto.CalendarDayDTO;
import com.example.hrmsclient.dto.HolidayDTO;
import com.example.hrmsclient.dto.LeaveResponseDTO;
import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.entity.Holiday;
import com.example.hrmsclient.entity.LeaveRequest;
import com.example.hrmsclient.repository.AttendanceRepository;
import com.example.hrmsclient.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private final AttendanceRepository attendanceRepo;
    private final LeaveRequestRepository leaveRepo;
    private final HolidayService holidayService;

    public CalendarService(AttendanceRepository attendanceRepo,
                           LeaveRequestRepository leaveRepo,
                           HolidayService holidayService) {
        this.attendanceRepo = attendanceRepo;
        this.leaveRepo      = leaveRepo;
        this.holidayService = holidayService;
    }

    // ─── Employee Calendar ────────────────────────────────────────────────────
    public List<CalendarDayDTO> getEmployeeCalendar(Long employeeId, int year, int month) {

        YearMonth ym    = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        Map<LocalDate, Attendance> attendanceMap =
            attendanceRepo.findByEmployeeIdAndAttendanceDateBetween(employeeId, start, end)
                .stream()
                .collect(Collectors.toMap(Attendance::getAttendanceDate, a -> a));

        List<LeaveRequest> leaves = leaveRepo.findByEmployeeIdAndDateRange(employeeId, start, end);
        Map<LocalDate, LeaveRequest> leaveMap = leaves.stream()
            .flatMap(l -> l.getStartDate()
                .datesUntil(l.getEndDate().plusDays(1))
                .filter(d -> !d.isBefore(start) && !d.isAfter(end))
                .map(d -> Map.entry(d, l)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        Map<LocalDate, Holiday> holidayMap = holidayService.getHolidayMapForRange(start, end);

        List<CalendarDayDTO> calendar = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            DayOfWeek dow      = date.getDayOfWeek();
            boolean isWeekend  = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            Holiday holiday    = holidayMap.get(date);
            LeaveRequest leave = leaveMap.get(date);
            Attendance att     = attendanceMap.get(date);

            if (isWeekend) {
                calendar.add(new CalendarDayDTO(date, "WEEKEND", null, null, null, null));

            } else if (holiday != null) {
                CalendarDayDTO day = new CalendarDayDTO(date, "HOLIDAY", null, null, null, null);
                day.setHolidayName(holiday.getName());
                day.setHolidayType(holiday.getHolidayType().name());
                calendar.add(day);

            } else if (leave != null) {
                calendar.add(new CalendarDayDTO(
                    date, "LEAVE",
                    leave.getLeaveType(),
                    leave.getStatus().name(),
                    null, null));

            } else if (att != null) {
                String checkIn  = att.getCheckIn()  != null ? att.getCheckIn().format(timeFmt)  : null;
                String checkOut = att.getCheckOut() != null ? att.getCheckOut().format(timeFmt) : null;
                calendar.add(new CalendarDayDTO(date, "PRESENT", null, null, checkIn, checkOut));

            } else if (date.isBefore(LocalDate.now())) {
                calendar.add(new CalendarDayDTO(date, "ABSENT", null, null, null, null));

            } else {
                calendar.add(new CalendarDayDTO(date, "UPCOMING", null, null, null, null));
            }
        }
        return calendar;
    }

    public AdminCalendarDTO getAdminCalendar(int year, int month) {
        YearMonth ym    = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();
        List<LeaveResponseDTO> leaves = leaveRepo.findAllByDateRange(start, end)
            .stream()
            .map(this::toLeaveDto)
            .collect(Collectors.toList());

        List<HolidayDTO> holidays = holidayService.getHolidaysByDateRange(start, end);

        return new AdminCalendarDTO(leaves, holidays);
    }

 // ─── Helpers 
    private LeaveResponseDTO toLeaveDto(LeaveRequest l) {

        String employeeId = null;
        String employeeName = null;

        if (l.getEmployee() != null) {
            employeeId = l.getEmployee().getEmployeeId();
            employeeName = l.getEmployee().getFullName();
        }

        return new LeaveResponseDTO(
                l.getId(),
                employeeId,
                employeeName,
                l.getLeaveType(),
                l.getStartDate(),
                l.getEndDate(),
                l.getLeaveDays(),
                l.getPaidDays(),
                l.getUnpaidDays(),
                l.getStatus(),
                l.getReason(),
                l.getRejectionReason(),
                l.getApprovedBy(),
                l.getApprovedAt(),
                l.getCreatedAt()
        );
    }
}