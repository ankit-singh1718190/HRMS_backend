package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // ✅ Fix 1: Check if employee already checked in today
    // Uses employee.id (Long) via the Employee relationship
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.employee.id = :employeeId AND a.attendanceDate = :date")
    boolean existsByEmployeeIdAndAttendanceDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);

    // ✅ Fix 2: Get today's attendance for checkout
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId AND a.attendanceDate = :date")
    Optional<Attendance> findByEmployeeIdAndAttendanceDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);

    // Get all attendance for an employee (latest first)
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId ORDER BY a.attendanceDate DESC")
    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(
            @Param("employeeId") Long employeeId);

    // Get attendance between date range (for monthly report)
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId " +
           "AND a.attendanceDate BETWEEN :start AND :end ORDER BY a.attendanceDate")
    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    Page<Attendance> findByStatusAndAttendanceDate(
            AttendanceStatus status, LocalDate date, Pageable pageable);

    // Count present days in a month for payroll
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :employeeId " +
           "AND a.attendanceDate BETWEEN :start AND :end AND a.status = :status")
    long countByEmployeeIdAndDateRangeAndStatus(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("status") AttendanceStatus status);
    
 // For dashboard filter
    Page<Attendance> findByAttendanceDate(LocalDate date, Pageable pageable);

    Page<Attendance> findByAttendanceDateBetween(
            LocalDate from, LocalDate to, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = :date AND a.status = :status")
    long countByAttendanceDateAndStatus(
            @Param("date") LocalDate date,
            @Param("status") AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId = :empId " +
           "AND a.attendanceDate BETWEEN :from AND :to")
    Page<Attendance> findByEmployeeIdStringAndDateRange(
            @Param("empId") String empId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
    @Query("""
    		SELECT a FROM Attendance a
    		WHERE a.employee.id = :employeeId
    		AND DATE(a.checkIn) = :date
    		""")
    Attendance findByEmployeeAndDate(Long employeeId, LocalDate date);
//    List<Attendance> findByDateAndCheckOutTimeIsNotNull(LocalDate date);
//    List<Attendance> findByDateAndCheckInTimeIsNotNull(LocalDate date);
}
