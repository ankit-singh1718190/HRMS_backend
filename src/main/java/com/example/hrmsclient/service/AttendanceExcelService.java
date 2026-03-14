package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.entity.AttendanceStatus;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.EmploymentStatus;
import com.example.hrmsclient.repository.AttendanceRepository;
import com.example.hrmsclient.repository.EmployeeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceExcelService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository   employeeRepository;

    public AttendanceExcelService(AttendanceRepository attendanceRepository,
                                  EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository   = employeeRepository;
    }

    // ── DAILY EXPORT ─────────────────────────────────────────────────────────
    public byte[] exportDaily(LocalDate date) throws IOException {
        List<Employee> employees = getActiveEmployees();
        List<Attendance> records = attendanceRepository.findByAttendanceDate(date);
        Map<Long, Attendance> recordMap = records.stream()
            .collect(Collectors.toMap(a -> a.getEmployee().getId(), a -> a));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Daily Attendance");

            // ── Styles
            CellStyle titleStyle   = titleStyle(wb);
            CellStyle headerStyle  = headerStyle(wb);
            CellStyle presentStyle = statusStyle(wb, new byte[]{(byte)198,(byte)239,(byte)206});
            CellStyle absentStyle  = statusStyle(wb, new byte[]{(byte)255,(byte)199,(byte)206});
            CellStyle leaveStyle   = statusStyle(wb, new byte[]{(byte)255,(byte)235,(byte)156});
            CellStyle dataStyle    = dataStyle(wb);

            // ── Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Daily Attendance Report — "
                + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            // ── Header Row
            String[] headers = { "Emp ID", "Employee Name", "Department",
                                  "Emp Type", "Date", "Status",
                                  "Check In", "Check Out", "Working Hours" };
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // ── Data Rows
            int rowIdx = 2;
            for (Employee emp : employees) {
                Attendance att = recordMap.get(emp.getId());
                Row row = sheet.createRow(rowIdx++);

                setCell(row, 0, emp.getEmployeeId(),   dataStyle);
                setCell(row, 1, emp.getFullName(),     dataStyle);
                setCell(row, 2, emp.getDepartment(),   dataStyle);
                setCell(row, 3, emp.getEmployeeType() != null
                    ? emp.getEmployeeType().name() : "", dataStyle);
                setCell(row, 4, date.toString(),       dataStyle);

                if (att != null) {
                    CellStyle st = resolveStatusStyle(att.getStatus(),
                        presentStyle, absentStyle, leaveStyle, dataStyle);
                    setCell(row, 5, att.getStatus().name(), st);
                    setCell(row, 6, att.getCheckIn()  != null ? att.getCheckIn().toLocalTime().toString()  : "", dataStyle);
                    setCell(row, 7, att.getCheckOut() != null ? att.getCheckOut().toLocalTime().toString() : "", dataStyle);
                    setCell(row, 8, att.getWorkingHours() != null ? att.getWorkingHours() : "", dataStyle);
                } else {
                    setCell(row, 5, "NOT MARKED", absentStyle);
                    setCell(row, 6, "", dataStyle);
                    setCell(row, 7, "", dataStyle);
                    setCell(row, 8, "", dataStyle);
                }
            }

            // ── Summary Row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            long present  = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                || a.getStatus() == AttendanceStatus.WORK_FROM_HOME).count();
            long absent   = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
            long onLeave  = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();
            long halfDay  = records.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
            long notMarked = employees.size() - records.size();

            CellStyle summaryStyle = summaryStyle(wb);
            setCell(summaryRow, 0, "SUMMARY", summaryStyle);
            setCell(summaryRow, 1, "Present: "   + present,  summaryStyle);
            setCell(summaryRow, 2, "Absent: "    + absent,   summaryStyle);
            setCell(summaryRow, 3, "On Leave: "  + onLeave,  summaryStyle);
            setCell(summaryRow, 4, "Half Day: "  + halfDay,  summaryStyle);
            setCell(summaryRow, 5, "Not Marked: "+ notMarked,summaryStyle);

            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // ── MONTHLY EXPORT ────────────────────────────────────────────────────────
    public byte[] exportMonthly(LocalDate month) throws IOException {
        YearMonth ym    = YearMonth.of(month.getYear(), month.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        List<Employee> employees = getActiveEmployees();
        List<Attendance> allRecords = attendanceRepository
            .findByAttendanceDateBetween(start, end,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        Map<Long, List<Attendance>> byEmp = allRecords.stream()
            .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Monthly Attendance");

            CellStyle titleStyle  = titleStyle(wb);
            CellStyle headerStyle = headerStyle(wb);
            CellStyle dataStyle   = dataStyle(wb);
            CellStyle summaryStyle= summaryStyle(wb);

            // ── Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Monthly Attendance Report — "
                + ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            // ── Header
            String[] headers = { "Emp ID", "Employee Name", "Department", "Emp Type",
                                  "Month", "Present", "Half Day", "On Leave",
                                  "Absent", "Total Marked" };
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // ── Data
            int rowIdx = 2;
            long totalPresent = 0, totalAbsent = 0, totalLeave = 0, totalHalfDay = 0;

            for (Employee emp : employees) {
                List<Attendance> empRecs = byEmp.getOrDefault(emp.getId(), Collections.emptyList());
                long present = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                    || a.getStatus() == AttendanceStatus.WORK_FROM_HOME).count();
                long halfDay = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
                long onLeave = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();
                long absent  = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();

                totalPresent += present;
                totalAbsent  += absent;
                totalLeave   += onLeave;
                totalHalfDay += halfDay;

                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, emp.getEmployeeId(),   dataStyle);
                setCell(row, 1, emp.getFullName(),     dataStyle);
                setCell(row, 2, emp.getDepartment(),   dataStyle);
                setCell(row, 3, emp.getEmployeeType() != null ? emp.getEmployeeType().name() : "", dataStyle);
                setCell(row, 4, ym.toString(),         dataStyle);
                setCellNum(row, 5, present,  dataStyle);
                setCellNum(row, 6, halfDay,  dataStyle);
                setCellNum(row, 7, onLeave,  dataStyle);
                setCellNum(row, 8, absent,   dataStyle);
                setCellNum(row, 9, empRecs.size(), dataStyle);
            }

            // ── Summary
            Row summaryRow = sheet.createRow(rowIdx + 1);
            setCell(summaryRow, 0, "TOTAL", summaryStyle);
            setCell(summaryRow, 1, "",      summaryStyle);
            setCell(summaryRow, 2, "",      summaryStyle);
            setCell(summaryRow, 3, "",      summaryStyle);
            setCell(summaryRow, 4, "",      summaryStyle);
            setCellNum(summaryRow, 5, totalPresent,  summaryStyle);
            setCellNum(summaryRow, 6, totalHalfDay,  summaryStyle);
            setCellNum(summaryRow, 7, totalLeave,    summaryStyle);
            setCellNum(summaryRow, 8, totalAbsent,   summaryStyle);
            setCellNum(summaryRow, 9, (long) employees.size() * ym.lengthOfMonth(), summaryStyle);

            autoSizeColumns(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // ── YEARLY EXPORT ─────────────────────────────────────────────────────────
    public byte[] exportYearly(int year) throws IOException {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);

        List<Employee> employees = getActiveEmployees();
        List<Attendance> allRecords = attendanceRepository
            .findByAttendanceDateBetween(start, end,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        Map<Long, List<Attendance>> byEmp = allRecords.stream()
            .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        try (Workbook wb = new XSSFWorkbook()) {

            // ── Sheet 1: Yearly Summary ───────────────────────────────────────
            Sheet summarySheet = wb.createSheet("Yearly Summary");
            CellStyle titleStyle  = titleStyle(wb);
            CellStyle headerStyle = headerStyle(wb);
            CellStyle dataStyle   = dataStyle(wb);
            CellStyle summaryStyle= summaryStyle(wb);

            Row titleRow = summarySheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Yearly Attendance Report — " + year);
            titleCell.setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            String[] headers = { "Emp ID", "Employee Name", "Department",
                                  "Present", "Half Day", "On Leave",
                                  "Absent", "Total Marked", "Attendance %" };
            Row headerRow = summarySheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 2;
            for (Employee emp : employees) {
                List<Attendance> empRecs = byEmp.getOrDefault(emp.getId(), Collections.emptyList());
                long present = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                    || a.getStatus() == AttendanceStatus.WORK_FROM_HOME).count();
                long halfDay = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
                long onLeave = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();
                long absent  = empRecs.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
                int  total   = empRecs.size();
                double pct   = total > 0 ? Math.round((present * 100.0 / total) * 10.0) / 10.0 : 0.0;

                Row row = summarySheet.createRow(rowIdx++);
                setCell(row, 0, emp.getEmployeeId(), dataStyle);
                setCell(row, 1, emp.getFullName(),   dataStyle);
                setCell(row, 2, emp.getDepartment(), dataStyle);
                setCellNum(row, 3, present,  dataStyle);
                setCellNum(row, 4, halfDay,  dataStyle);
                setCellNum(row, 5, onLeave,  dataStyle);
                setCellNum(row, 6, absent,   dataStyle);
                setCellNum(row, 7, total,    dataStyle);
                setCell(row, 8, pct + "%",   dataStyle);
            }
            autoSizeColumns(summarySheet, headers.length);

            // ── Sheet 2–13: One sheet per month ──────────────────────────────
            for (int m = 1; m <= 12; m++) {
                YearMonth ym       = YearMonth.of(year, m);
                LocalDate mStart   = ym.atDay(1);
                LocalDate mEnd     = ym.atEndOfMonth();
                String sheetName   = ym.format(DateTimeFormatter.ofPattern("MMM yyyy"));

                Sheet mSheet = wb.createSheet(sheetName);

                // Build date columns for all days in month
                List<LocalDate> days = mStart.datesUntil(mEnd.plusDays(1))
                    .collect(Collectors.toList());

                // Header: fixed columns + one column per day
                Row mHeader = mSheet.createRow(0);
                setCell(mHeader, 0, "Emp ID",       headerStyle);
                setCell(mHeader, 1, "Employee Name",headerStyle);
                setCell(mHeader, 2, "Department",   headerStyle);
                for (int d = 0; d < days.size(); d++) {
                    setCell(mHeader, 3 + d,
                        days.get(d).format(DateTimeFormatter.ofPattern("dd")), headerStyle);
                }
                setCell(mHeader, 3 + days.size(),     "Present",  headerStyle);
                setCell(mHeader, 3 + days.size() + 1, "Absent",   headerStyle);
                setCell(mHeader, 3 + days.size() + 2, "Leave",    headerStyle);
                setCell(mHeader, 3 + days.size() + 3, "Half Day", headerStyle);

                // Filter records for this month
                final int fm = m;
                Map<Long, Map<LocalDate, Attendance>> empDayMap = allRecords.stream()
                    .filter(a -> a.getAttendanceDate().getMonthValue() == fm
                              && a.getAttendanceDate().getYear() == year)
                    .collect(Collectors.groupingBy(
                        a -> a.getEmployee().getId(),
                        Collectors.toMap(Attendance::getAttendanceDate, a -> a)));

                int mRowIdx = 1;
                for (Employee emp : employees) {
                    Map<LocalDate, Attendance> dayMap =
                        empDayMap.getOrDefault(emp.getId(), Collections.emptyMap());
                    Row row = mSheet.createRow(mRowIdx++);
                    setCell(row, 0, emp.getEmployeeId(), dataStyle);
                    setCell(row, 1, emp.getFullName(),   dataStyle);
                    setCell(row, 2, emp.getDepartment(), dataStyle);

                    long present = 0, absent = 0, leave = 0, halfDay = 0;
                    for (int d = 0; d < days.size(); d++) {
                        Attendance att = dayMap.get(days.get(d));
                        String val = att != null ? statusShortCode(att.getStatus()) : "-";
                        setCell(row, 3 + d, val, dataStyle);
                        if (att != null) {
                            switch (att.getStatus()) {
                                case PRESENT, WORK_FROM_HOME -> present++;
                                case ABSENT                  -> absent++;
                                case ON_LEAVE                -> leave++;
                                case HALF_DAY                -> halfDay++;
                                default -> {}
                            }
                        }
                    }
                    setCellNum(row, 3 + days.size(),     present,  dataStyle);
                    setCellNum(row, 3 + days.size() + 1, absent,   dataStyle);
                    setCellNum(row, 3 + days.size() + 2, leave,    dataStyle);
                    setCellNum(row, 3 + days.size() + 3, halfDay,  dataStyle);
                }
                autoSizeColumns(mSheet, 3 + days.size() + 4);
            }

            return toBytes(wb);
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private List<Employee> getActiveEmployees() {
        return employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();
    }

    private String statusShortCode(AttendanceStatus s) {
        return switch (s) {
            case PRESENT          -> "P";
            case ABSENT           -> "A";
            case ON_LEAVE         -> "L";
            case HALF_DAY         -> "HD";
            case WORK_FROM_HOME   -> "WFH";
            default               -> "-";
        };
    }

    private CellStyle resolveStatusStyle(AttendanceStatus status,
            CellStyle present, CellStyle absent, CellStyle leave, CellStyle def) {
        return switch (status) {
            case PRESENT, WORK_FROM_HOME -> present;
            case ABSENT                  -> absent;
            case ON_LEAVE, HALF_DAY      -> leave;
            default                      -> def;
        };
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void setCellNum(Row row, int col, long value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) sheet.autoSizeColumn(i);
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    // ── Cell Styles ───────────────────────────────────────────────────────────

    private CellStyle titleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font wf = wb.createFont();
        wf.setBold(true);
        wf.setFontHeightInPoints((short) 14);
        wf.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(wf);
        return s;
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle dataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle statusStyle(Workbook wb, byte[] rgb) {
        CellStyle s = dataStyle(wb);
        // Use a built-in color approximation
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private CellStyle summaryStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBorderTop(BorderStyle.MEDIUM);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }
}