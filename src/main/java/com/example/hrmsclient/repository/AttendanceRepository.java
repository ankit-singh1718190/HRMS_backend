package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    List<Attendance> findByAttendanceDate(LocalDate date); // NEW: all records for a date

    Page<Attendance> findByAttendanceDate(LocalDate date, Pageable pageable);
    Page<Attendance> findByAttendanceDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(
        Long employeeId, LocalDate from, LocalDate to);

    long countByAttendanceDateAndStatus(LocalDate date, AttendanceStatus status);
    List<Attendance> findByAttendanceDateAndCheckInIsNotNull(LocalDate attendanceDate);
    List<Attendance> findByAttendanceDateAndCheckOutIsNotNull(LocalDate attendanceDate);
    
    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee " +
            "WHERE a.attendanceDate BETWEEN :from AND :to " +
            "AND a.lastEditedAt IS NOT NULL " +
            "ORDER BY a.lastEditedAt DESC")
     List<Attendance> findEditedAttendanceWithEmployee(
         @Param("from") LocalDate from,
         @Param("to")   LocalDate to
     );
    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee " +
            "WHERE a.attendanceDate = :date")
     List<Attendance> findByAttendanceDateWithEmployee(@Param("date") LocalDate date);
    
    
}