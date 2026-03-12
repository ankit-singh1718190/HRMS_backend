package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.persistence.EntityListeners;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balances",
    uniqueConstraints = {
        @UniqueConstraint(
            name        = "uq_leave_balance",
            columnNames = {"employee_id", "leave_type", "year", "month"}
        )
    },
    indexes = {
        @Index(name = "idx_lb_employee", columnList = "employee_id"),
        @Index(name = "idx_lb_year",     columnList = "year"),
        @Index(name = "idx_lb_type",     columnList = "leave_type")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** "CASUAL", "SICK", "EARNED" — must match LeaveRequest.leaveType */
    @NotBlank
    @Column(name = "leave_type", length = 50, nullable = false)
    private String leaveType;

    /** Calendar year e.g. 2025 */
    @Min(2000)
    @Column(nullable = false)
    private int year;
    @Min(1) @Max(12)
    @Column(nullable = true)
    private Integer month;

    @Min(0)
    @Column(nullable = false)
    private int totalAllocated;

    /** Paid days already consumed (≤ totalAllocated) */
    @Min(0)
    @Column(nullable = false)
    private int usedPaidDays = 0;

    /** Unpaid days consumed after balance was exhausted */
    @Min(0)
    @Column(nullable = false)
    private int usedUnpaidDays = 0;

    /**
     * Optional HR note, e.g. "Ramadan allowance", "carry-forward Q1".
     */
    @Column(length = 300)
    private String note;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ─── Constructors ──────────────────────────────────────────────────────────

    public LeaveBalance() {}

    /** Yearly allocation constructor (month = null) */
    public LeaveBalance(Employee employee, String leaveType,
                        int year, int totalAllocated) {
        this.employee       = employee;
        this.leaveType      = leaveType;
        this.year           = year;
        this.month          = null;
        this.totalAllocated = totalAllocated;
    }

    /** Monthly allocation constructor */
    public LeaveBalance(Employee employee, String leaveType,
                        int year, int month, int totalAllocated) {
        this.employee       = employee;
        this.leaveType      = leaveType;
        this.year           = year;
        this.month          = month;
        this.totalAllocated = totalAllocated;
    }

    // ─── Derived helpers ───────────────────────────────────────────────────────

    public boolean isYearly()  { return month == null; }
    public boolean isMonthly() { return month != null; }

    /** Remaining paid days before going unpaid */
    public int getRemainingPaidDays() {
        return Math.max(0, totalAllocated - usedPaidDays);
    }

    /**
     * Consume {@code days}.
     * Automatically overflows into unpaid when balance is exhausted.
     *
     * @return number of unpaid (overflow) days — 0 means fully paid
     */
    public int consumeDays(int days) {
        int paid   = Math.min(days, getRemainingPaidDays());
        int unpaid = days - paid;
        usedPaidDays   += paid;
        usedUnpaidDays += unpaid;
        return unpaid;
    }

    /** Reverse a consumption block (used on cancel / reject). */
    public void restoreDays(int paidToRestore, int unpaidToRestore) {
        usedPaidDays   = Math.max(0, usedPaidDays   - paidToRestore);
        usedUnpaidDays = Math.max(0, usedUnpaidDays - unpaidToRestore);
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public Long     getId()             { return id;             }
    public Employee getEmployee()       { return employee;       }
    public String   getLeaveType()      { return leaveType;      }
    public int      getYear()           { return year;           }
    public Integer  getMonth()          { return month;          }
    public int      getTotalAllocated() { return totalAllocated; }
    public int      getUsedPaidDays()   { return usedPaidDays;   }
    public int      getUsedUnpaidDays() { return usedUnpaidDays; }
    public String   getNote()           { return note;           }
    public LocalDateTime getCreatedAt() { return createdAt;      }
    public LocalDateTime getUpdatedAt() { return updatedAt;      }

    public void setId(Long id)                        { this.id             = id;             }
    public void setEmployee(Employee e)               { this.employee       = e;              }
    public void setLeaveType(String t)                { this.leaveType      = t;              }
    public void setYear(int year)                     { this.year           = year;           }
    public void setMonth(Integer month)               { this.month          = month;          }
    public void setTotalAllocated(int v)              { this.totalAllocated = v;              }
    public void setUsedPaidDays(int v)                { this.usedPaidDays   = v;              }
    public void setUsedUnpaidDays(int v)              { this.usedUnpaidDays = v;              }
    public void setNote(String note)                  { this.note           = note;           }
    public void setCreatedAt(LocalDateTime v)         { this.createdAt      = v;             }
    public void setUpdatedAt(LocalDateTime v)         { this.updatedAt      = v;             }
}