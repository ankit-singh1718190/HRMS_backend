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

    boolean existsByEmployeeIdAndPayrollMonth(Long employeeId, LocalDate payrollMonth);
    boolean existsByPayrollMonthAndStatus(LocalDate month, PayrollStatus status); // NEW: for lock check

    Optional<Payroll> findByEmployeeIdAndPayrollMonth(Long employeeId, LocalDate payrollMonth);

    Page<Payroll> findByPayrollMonth(LocalDate month, Pageable pageable);
    Page<Payroll> findByPayrollMonthAndStatus(LocalDate month, PayrollStatus status, Pageable pageable);

    List<Payroll> findByEmployeeId(Long employeeId);
    List<Payroll> findByPayrollMonthAndStatus(LocalDate month, PayrollStatus status);

    List<Payroll> findByPayrollMonthAndStatusIn(LocalDate month, List<PayrollStatus> statuses);

    @Query("SELECT COUNT(p) FROM Payroll p WHERE p.payrollMonth = :month AND p.status = :status")
    long countByMonthAndStatus(@Param("month") LocalDate month, @Param("status") PayrollStatus status);

    @Query("SELECT SUM(p.netSalary) FROM Payroll p WHERE p.payrollMonth = :month AND p.status = 'PAID'")
    BigDecimal getTotalNetSalaryByMonth(@Param("month") LocalDate month);
}