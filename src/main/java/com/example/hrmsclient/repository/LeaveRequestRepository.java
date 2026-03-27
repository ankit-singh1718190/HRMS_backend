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
    Page<LeaveRequest> findByStatusAndEmployeeIdIn(
            LeaveStatus status, List<Long> employeeIds, Pageable pageable);

    @Query("""
        SELECT COUNT(lr)
        FROM LeaveRequest lr
        WHERE lr.status = 'PENDING'
          AND lr.employee.manager IS NOT NULL
    """)
    long countPendingLeavesWithReportingManager();

    @Query("""
        SELECT COUNT(lr) FROM LeaveRequest lr
        WHERE lr.employee.id = :empId
          AND lr.status = 'APPROVED'
          AND lr.leaveType = :leaveType
          AND lr.startDate >= :from
          AND lr.endDate   <= :to
    """)
    long countApprovedLeavesByType(
            @Param("empId") Long empId,
            @Param("leaveType") String leaveType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.employee.id = :empId
          AND lr.startDate >= :from
          AND lr.endDate   <= :to
    """)
    List<LeaveRequest> findByEmployeeIdAndDateRange(
            @Param("empId") Long empId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.startDate >= :from
          AND lr.endDate   <= :to
    """)
    List<LeaveRequest> findAllByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    long countByEmployeeManagerIdAndStatus(Long managerId, LeaveStatus status);
    long countByStatus(LeaveStatus status);

    // ── ADMIN / HR — filter by status, leaveType, employeeName (no employee restriction) ──
    @Query("""
        SELECT l FROM LeaveRequest l
        WHERE (:status IS NULL OR l.status = :status)
          AND (:leaveType IS NULL OR l.leaveType = :leaveType)
          AND (:employeeName IS NULL OR
               LOWER(CONCAT(
                   COALESCE(l.employee.firstName, ''), ' ',
                   COALESCE(l.employee.lastName, '')
               ))
               LIKE LOWER(CONCAT('%', :employeeName, '%')))
    """)
    Page<LeaveRequest> findWithFilters(
            @Param("status") LeaveStatus status,
            @Param("leaveType") String leaveType,
            @Param("employeeName") String employeeName,
            Pageable pageable
    );

    // ── MANAGER — same filters but restricted to their team's employee IDs ────
    @Query("""
        SELECT l FROM LeaveRequest l
        WHERE l.employee.id IN :employeeIds
          AND (:status IS NULL OR l.status = :status)
          AND (:leaveType IS NULL OR l.leaveType = :leaveType)
          AND (:employeeName IS NULL OR
               LOWER(CONCAT(
                   COALESCE(l.employee.firstName, ''), ' ',
                   COALESCE(l.employee.lastName, '')
               ))
               LIKE LOWER(CONCAT('%', :employeeName, '%')))
    """)
    Page<LeaveRequest> findWithFiltersAndEmployeeIds(
            @Param("status") LeaveStatus status,
            @Param("leaveType") String leaveType,
            @Param("employeeName") String employeeName,
            @Param("employeeIds") List<Long> employeeIds,
            Pageable pageable
    );
}