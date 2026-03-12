package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Payroll;
import com.example.hrmsclient.entity.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    @Query("SELECT COUNT(p) > 0 FROM Payroll p WHERE p.employee.id = :empId AND p.payrollMonth = :month")
    boolean existsByEmployeeIdAndPayrollMonth(
            @Param("empId") Long empId,
            @Param("month") LocalDate month);

    @Query("SELECT p FROM Payroll p WHERE p.employee.id = :empId AND p.payrollMonth = :month")
    Optional<Payroll> findByEmployeeIdAndPayrollMonth(
            @Param("empId") Long empId,
            @Param("month") LocalDate month);

    @Query("SELECT p FROM Payroll p WHERE p.employee.id = :empId ORDER BY p.payrollMonth DESC")
    List<Payroll> findByEmployeeId(@Param("empId") Long empId);

    // Get all payrolls by month (for bulk processing)
    @Query("SELECT p FROM Payroll p WHERE p.payrollMonth = :month ORDER BY p.employee.firstName")
    Page<Payroll> findByPayrollMonth(@Param("month") LocalDate month, Pageable pageable);

    @Query("SELECT p FROM Payroll p WHERE p.payrollMonth = :month AND p.status = :status")
    List<Payroll> findByPayrollMonthAndStatus(
            @Param("month") LocalDate month,
            @Param("status") PayrollStatus status);

    List<Payroll> findByStatus(PayrollStatus status);

    @Query("SELECT SUM(p.netSalary) FROM Payroll p WHERE p.payrollMonth = :month AND p.status = 'PAID'")
    BigDecimal getTotalNetSalaryByMonth(@Param("month") LocalDate month);
    
    @Query("SELECT COUNT(p) FROM Payroll p WHERE p.payrollMonth = :month AND p.status = :status")
    long countByMonthAndStatus(
            @Param("month") LocalDate month,
            @Param("status") PayrollStatus status);
    Page<Payroll> findByPayrollMonthAndStatus(
            LocalDate month, PayrollStatus status, Pageable pageable);
    List<Payroll> findByEmployeeIdAndPayrollMonthBetween(
            Long employeeId,
            LocalDate startMonth,
            LocalDate endMonth);
}