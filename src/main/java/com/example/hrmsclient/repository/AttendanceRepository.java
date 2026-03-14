package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Attendance;
import com.example.hrmsclient.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}