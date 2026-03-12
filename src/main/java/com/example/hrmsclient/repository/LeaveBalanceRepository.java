package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    // ── Lookup: exact monthly row ──────────────────────────────────────────────

    /** Find a specific MONTHLY balance row (month is not null). */
    Optional<LeaveBalance> findByEmployee_IdAndLeaveTypeAndYearAndMonth(
            Long employeeId, String leaveType, int year, int month);

    // ── Lookup: yearly row (month IS NULL) ────────────────────────────────────

    /** Find a YEARLY balance row (month = null). */
    @Query("""
        SELECT lb FROM LeaveBalance lb
         WHERE lb.employee.id = :empId
           AND lb.leaveType   = :leaveType
           AND lb.year        = :year
           AND lb.month IS NULL
        """)
    Optional<LeaveBalance> findYearly(
            @Param("empId")     Long   empId,
            @Param("leaveType") String leaveType,
            @Param("year")      int    year);

    // ── All rows for a given employee + year (both yearly and monthly) ─────────

    List<LeaveBalance> findByEmployee_IdAndYear(Long employeeId, int year);

    /** Only the yearly rows (month IS NULL) for one employee. */
    @Query("""
        SELECT lb FROM LeaveBalance lb
         WHERE lb.employee.id = :empId
           AND lb.year        = :year
           AND lb.month IS NULL
        ORDER BY lb.leaveType
        """)
    List<LeaveBalance> findYearlyRows(
            @Param("empId") Long empId,
            @Param("year")  int  year);

    /** Only the monthly rows (month IS NOT NULL) for one employee + year. */
    @Query("""
        SELECT lb FROM LeaveBalance lb
         WHERE lb.employee.id = :empId
           AND lb.year        = :year
           AND lb.month IS NOT NULL
        ORDER BY lb.month, lb.leaveType
        """)
    List<LeaveBalance> findMonthlyRows(
            @Param("empId") Long empId,
            @Param("year")  int  year);

    // ── Full history across all years ─────────────────────────────────────────

    List<LeaveBalance> findByEmployee_IdOrderByYearDescMonthAsc(Long employeeId);

    // ── Existence checks ──────────────────────────────────────────────────────

    boolean existsByEmployee_IdAndLeaveTypeAndYearAndMonth(
            Long employeeId, String leaveType, int year, Integer month);

    // ── HR aggregate report ───────────────────────────────────────────────────

    /**
     * Company-wide totals per leave type for a given year
     * (sums both yearly and monthly rows together).
     */
    @Query("""
        SELECT lb.leaveType,
               SUM(lb.totalAllocated)  AS totalAllocated,
               SUM(lb.usedPaidDays)    AS usedPaid,
               SUM(lb.usedUnpaidDays)  AS usedUnpaid
          FROM LeaveBalance lb
         WHERE lb.year = :year
         GROUP BY lb.leaveType
        """)
    List<Object[]> aggregateByLeaveTypeForYear(@Param("year") int year);
}