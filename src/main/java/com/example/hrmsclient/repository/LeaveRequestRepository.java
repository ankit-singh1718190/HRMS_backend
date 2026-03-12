package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.LeaveRequest;
import com.example.hrmsclient.entity.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByEmployee_IdOrderByCreatedAtDesc(Long employeeId, Pageable pageable);

    Page<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveStatus status, Pageable pageable);

    List<LeaveRequest> findByEmployee_IdAndStatus(Long employeeId, LeaveStatus status);

    // Check overlapping leave for same employee
    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE l.employee.id = :empId " +
           "AND l.status != 'REJECTED' AND l.status != 'CANCELLED' " +
           "AND l.startDate <= :endDate AND l.endDate >= :startDate")
    boolean hasOverlappingLeave(@Param("empId") Long empId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    long countByStatus(LeaveStatus status);

    // Monthly leave summary
    @Query("SELECT SUM(DATEDIFF(l.endDate, l.startDate) + 1) FROM LeaveRequest l " +
           "WHERE l.employee.id = :empId AND l.status = 'APPROVED' " +
           "AND YEAR(l.startDate) = :year AND MONTH(l.startDate) = :month")
    Long sumApprovedLeaveDaysForMonth(@Param("empId") Long empId,
                                      @Param("year") int year,
                                      @Param("month") int month);

 // For CalendarService - employee calendar
 @Query("SELECT l FROM LeaveRequest l WHERE l.employee.id = :empId " +
        "AND l.status IN ('APPROVED','PENDING') " +
        "AND l.startDate <= :end AND l.endDate >= :start")
 List<LeaveRequest> findByEmployeeIdAndDateRange(
         @Param("empId") Long empId,
         @Param("start") LocalDate start,
         @Param("end") LocalDate end);

 // For CalendarService - admin calendar (all employees)
 @Query("SELECT l FROM LeaveRequest l WHERE " +
        "l.startDate <= :end AND l.endDate >= :start " +
        "ORDER BY l.startDate")
 List<LeaveRequest> findAllByDateRange(
         @Param("start") LocalDate start,
         @Param("end") LocalDate end);
}