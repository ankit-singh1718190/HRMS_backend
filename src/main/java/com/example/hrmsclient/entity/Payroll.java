package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll",
    indexes = {
        @Index(name = "idx_payroll_emp",    columnList = "employee_id"),
        @Index(name = "idx_payroll_month",  columnList = "payroll_month"),
        @Index(name = "idx_payroll_status", columnList = "status"),
        @Index(name = "idx_payroll_emp_month",
               columnList = "employee_id, payroll_month", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ── Payroll Period ────────────────────────────────────────────────────────
    @Column(name = "payroll_month", nullable = false)
    private LocalDate payrollMonth;        

    @Column(name = "working_days")
    private Integer workingDays;          

    @Column(name = "present_days")
    private Integer presentDays;          

    @Column(name = "leave_days")
    private Integer leaveDays;

    @Column(name = "absent_days")
    private Integer absentDays;

    // ── Earnings (in INR) ─────────────────────────────────────────────────────
    @Column(name = "basic_salary",   precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra",            precision = 12, scale = 2)
    private BigDecimal hra;                

    @Column(name = "da",             precision = 12, scale = 2)
    private BigDecimal da;                 

    @Column(name = "special_allowance", precision = 12, scale = 2)
    private BigDecimal specialAllowance;   

    @Column(name = "overtime_amount",   precision = 12, scale = 2)
    private BigDecimal overtimeAmount;

    @Column(name = "bonus_amount",      precision = 12, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "reimbursement",     precision = 12, scale = 2)
    private BigDecimal reimbursement;

    @Column(name = "gross_salary",   precision = 12, scale = 2)
    private BigDecimal grossSalary;       

    @Column(name = "pf_employee",    precision = 12, scale = 2)
    private BigDecimal pfEmployee;         // 12% of basic (employee share)

    @Column(name = "pf_employer",    precision = 12, scale = 2)
    private BigDecimal pfEmployer;         // 12% of basic (employer share)

    @Column(name = "esi_employee",   precision = 12, scale = 2)
    private BigDecimal esiEmployee;        // 0.75% of gross (if gross <= 21000)

    @Column(name = "esi_employer",   precision = 12, scale = 2)
    private BigDecimal esiEmployer;       

    @Column(name = "tds",            precision = 12, scale = 2)
    private BigDecimal tds;                // Income tax deduction

    @Column(name = "professional_tax", precision = 12, scale = 2)
    private BigDecimal professionalTax;   

    @Column(name = "loan_deduction",   precision = 12, scale = 2)
    private BigDecimal loanDeduction;

    @Column(name = "other_deduction",  precision = 12, scale = 2)
    private BigDecimal otherDeduction;

    @Column(name = "total_deductions", precision = 12, scale = 2)
    private BigDecimal totalDeductions;    // Sum of all deductions

    // ── Net Pay ───────────────────────────────────────────────────────────────
    @Column(name = "net_salary",     precision = 12, scale = 2, nullable = false)
    private BigDecimal netSalary;          // Gross - Deductions = Amount transferred

    // ── Payment Info ──────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "payment_date")
    private LocalDate paymentDate;         // Date salary was transferred

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;       // Bank transaction reference number

    @Column(name = "payment_mode", length = 30)
    private String paymentMode;            // NEFT / IMPS / RTGS

    @Column(name = "bank_account", length = 500)
    private String bankAccount;            // Encrypted account number

    @Column(name = "ifsc_code", length = 15)
    private String ifscCode;

    @Column(name = "payslip_url", length = 500)
    private String payslipUrl;             // PDF payslip URL

    @Column(name = "payslip_sent")
    private boolean payslipSent = false;   // Email sent flag

    @Column(name = "remarks", length = 300)
    private String remarks;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreatedDate  @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate                        private LocalDateTime updatedAt;
    @Column(name = "processed_by")           private String processedBy;
    @Column(name = "approved_by")            private String approvedBy;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public Employee getEmployee()                   { return employee; }
    public void setEmployee(Employee e)             { this.employee = e; }

    public LocalDate getPayrollMonth()              { return payrollMonth; }
    public void setPayrollMonth(LocalDate m)        { this.payrollMonth = m; }

    public Integer getWorkingDays()                 { return workingDays; }
    public void setWorkingDays(Integer d)           { this.workingDays = d; }

    public Integer getPresentDays()                 { return presentDays; }
    public void setPresentDays(Integer d)           { this.presentDays = d; }

    public Integer getLeaveDays()                   { return leaveDays; }
    public void setLeaveDays(Integer d)             { this.leaveDays = d; }

    public Integer getAbsentDays()                  { return absentDays; }
    public void setAbsentDays(Integer d)            { this.absentDays = d; }

    public BigDecimal getBasicSalary()              { return basicSalary; }
    public void setBasicSalary(BigDecimal v)        { this.basicSalary = v; }

    public BigDecimal getHra()                      { return hra; }
    public void setHra(BigDecimal v)                { this.hra = v; }

    public BigDecimal getDa()                       { return da; }
    public void setDa(BigDecimal v)                 { this.da = v; }

    public BigDecimal getSpecialAllowance()         { return specialAllowance; }
    public void setSpecialAllowance(BigDecimal v)   { this.specialAllowance = v; }

    public BigDecimal getOvertimeAmount()           { return overtimeAmount; }
    public void setOvertimeAmount(BigDecimal v)     { this.overtimeAmount = v; }

    public BigDecimal getBonusAmount()              { return bonusAmount; }
    public void setBonusAmount(BigDecimal v)        { this.bonusAmount = v; }

    public BigDecimal getReimbursement()            { return reimbursement; }
    public void setReimbursement(BigDecimal v)      { this.reimbursement = v; }

    public BigDecimal getGrossSalary()              { return grossSalary; }
    public void setGrossSalary(BigDecimal v)        { this.grossSalary = v; }

    public BigDecimal getPfEmployee()               { return pfEmployee; }
    public void setPfEmployee(BigDecimal v)         { this.pfEmployee = v; }

    public BigDecimal getPfEmployer()               { return pfEmployer; }
    public void setPfEmployer(BigDecimal v)         { this.pfEmployer = v; }

    public BigDecimal getEsiEmployee()              { return esiEmployee; }
    public void setEsiEmployee(BigDecimal v)        { this.esiEmployee = v; }

    public BigDecimal getEsiEmployer()              { return esiEmployer; }
    public void setEsiEmployer(BigDecimal v)        { this.esiEmployer = v; }

    public BigDecimal getTds()                      { return tds; }
    public void setTds(BigDecimal v)                { this.tds = v; }

    public BigDecimal getProfessionalTax()          { return professionalTax; }
    public void setProfessionalTax(BigDecimal v)    { this.professionalTax = v; }

    public BigDecimal getLoanDeduction()            { return loanDeduction; }
    public void setLoanDeduction(BigDecimal v)      { this.loanDeduction = v; }

    public BigDecimal getOtherDeduction()           { return otherDeduction; }
    public void setOtherDeduction(BigDecimal v)     { this.otherDeduction = v; }

    public BigDecimal getTotalDeductions()          { return totalDeductions; }
    public void setTotalDeductions(BigDecimal v)    { this.totalDeductions = v; }

    public BigDecimal getNetSalary()                { return netSalary; }
    public void setNetSalary(BigDecimal v)          { this.netSalary = v; }

    public PayrollStatus getStatus()                { return status; }
    public void setStatus(PayrollStatus s)          { this.status = s; }

    public LocalDate getPaymentDate()               { return paymentDate; }
    public void setPaymentDate(LocalDate d)         { this.paymentDate = d; }

    public String getPaymentReference()             { return paymentReference; }
    public void setPaymentReference(String r)       { this.paymentReference = r; }

    public String getPaymentMode()                  { return paymentMode; }
    public void setPaymentMode(String m)            { this.paymentMode = m; }

    public String getBankAccount()                  { return bankAccount; }
    public void setBankAccount(String v)            { this.bankAccount = v; }

    public String getIfscCode()                     { return ifscCode; }
    public void setIfscCode(String v)               { this.ifscCode = v; }

    public String getPayslipUrl()                   { return payslipUrl; }
    public void setPayslipUrl(String v)             { this.payslipUrl = v; }

    public boolean isPayslipSent()                  { return payslipSent; }
    public void setPayslipSent(boolean v)           { this.payslipSent = v; }

    public String getRemarks()                      { return remarks; }
    public void setRemarks(String v)                { this.remarks = v; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)       { this.updatedAt = v; }

    public String getProcessedBy()                  { return processedBy; }
    public void setProcessedBy(String v)            { this.processedBy = v; }

    public String getApprovedBy()                   { return approvedBy; }
    public void setApprovedBy(String v)             { this.approvedBy = v; }
}