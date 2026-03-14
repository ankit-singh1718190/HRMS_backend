package com.example.hrmsclient.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * NEW: PayrollRequestDTO
 *
 * Used by admin to set/update salary components for an active employee
 * for a given payroll month.
 *
 * Auto-calculated fields (set by PayrollCalculationService, not by admin):
 *   - weekendWorkAmount  (= basicSalary/26 * weekendWorkDays)
 *   - pfEmployee         (= 12% of basicSalary)
 *   - professionalTax    (= slab based on gross)
 *   - grossSalary        (= sum of all earnings)
 *   - totalDeductions    (= sum of all deductions)
 *   - netSalary          (= gross - totalDeductions)
 */
public class PayrollRequestDTO {

    private Long       employeeId;      
    private LocalDate  payrollMonth;    

    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal specialAllowance;   
    private BigDecimal arrears;
    private BigDecimal perfPay;
    private Integer    weekendWorkDays;   
    private BigDecimal reimbursement;
    private BigDecimal fbp;

    private BigDecimal tds;
    private BigDecimal salaryAdvance;
    private BigDecimal otherDeduction;

    private String     remarks;

    public PayrollRequestDTO() {}

    public Long getEmployeeId()                { return employeeId; }
    public void setEmployeeId(Long v)          { this.employeeId = v; }

    public LocalDate getPayrollMonth()         { return payrollMonth; }
    public void setPayrollMonth(LocalDate v)   { this.payrollMonth = v; }

    public BigDecimal getBasicSalary()         { return basicSalary; }
    public void setBasicSalary(BigDecimal v)   { this.basicSalary = v; }

    public BigDecimal getHra()                 { return hra; }
    public void setHra(BigDecimal v)           { this.hra = v; }

    public BigDecimal getSpecialAllowance()    { return specialAllowance; }
    public void setSpecialAllowance(BigDecimal v) { this.specialAllowance = v; }

    public BigDecimal getArrears()             { return arrears; }
    public void setArrears(BigDecimal v)       { this.arrears = v; }

    public BigDecimal getPerfPay()             { return perfPay; }
    public void setPerfPay(BigDecimal v)       { this.perfPay = v; }

    public Integer getWeekendWorkDays()        { return weekendWorkDays; }
    public void setWeekendWorkDays(Integer v)  { this.weekendWorkDays = v; }

    public BigDecimal getReimbursement()       { return reimbursement; }
    public void setReimbursement(BigDecimal v) { this.reimbursement = v; }

    public BigDecimal getFbp()                 { return fbp; }
    public void setFbp(BigDecimal v)           { this.fbp = v; }

    public BigDecimal getTds()                 { return tds; }
    public void setTds(BigDecimal v)           { this.tds = v; }

    public BigDecimal getSalaryAdvance()       { return salaryAdvance; }
    public void setSalaryAdvance(BigDecimal v) { this.salaryAdvance = v; }

    public BigDecimal getOtherDeduction()      { return otherDeduction; }
    public void setOtherDeduction(BigDecimal v){ this.otherDeduction = v; }

    public String getRemarks()                 { return remarks; }
    public void setRemarks(String v)           { this.remarks = v; }
}