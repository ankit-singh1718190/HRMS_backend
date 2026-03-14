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

    Page<LeaveRequest> findByEmployeeId(Long empId, Pageable pageable);
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    // NEW: Pending leaves where employee belongs to a specific manager
    Page<LeaveRequest> findByStatusAndEmployeeIdIn(
        LeaveStatus status, List<Long> employeeIds, Pageable pageable);

    // NEW: Count pending leaves that have a reporting manager (for dashboard tile)
    @Query("""
        SELECT COUNT(lr) FROM LeaveRequest lr
        WHERE lr.status = 'PENDING'
          AND lr.employee.reportingManager IS NOT NULL
          AND lr.employee.reportingManager <> ''
    """)
    long countPendingLeavesWithReportingManager();

    // NEW: Count approved leaves by type in a date range (for leave balance report)
    @Query("""
        SELECT COUNT(lr) FROM LeaveRequest lr
        WHERE lr.employee.id = :empId
          AND lr.status = 'APPROVED'
          AND lr.leaveType = :leaveType
          AND lr.startDate >= :from
          AND lr.endDate   <= :to
    """)
    long countApprovedLeavesByType(
        @Param("empId")     Long empId,
        @Param("leaveType") String leaveType,
        @Param("from")      LocalDate from,
        @Param("to")        LocalDate to);
    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.employee.id = :empId
              AND lr.startDate >= :from
              AND lr.endDate   <= :to
        """)
        List<LeaveRequest> findByEmployeeIdAndDateRange(
            @Param("empId") Long empId,
            @Param("from")  LocalDate from,
            @Param("to")    LocalDate to);

        @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.startDate >= :from
              AND lr.endDate   <= :to
        """)
        List<LeaveRequest> findAllByDateRange(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);
}