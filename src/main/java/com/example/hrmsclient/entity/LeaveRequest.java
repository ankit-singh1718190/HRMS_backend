package com.example.hrmsclient.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.persistence.EntityListeners;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests",
    indexes = {
        @Index(name = "idx_leave_emp",    columnList = "employee_id"),
        @Index(name = "idx_leave_status", columnList = "status"),
        @Index(name = "idx_leave_dates",  columnList = "startDate, endDate")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference
    private Employee employee;

    @NotBlank
    @Column(length = 50)
    private String leaveType;

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Size(max = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Size(max = 300)
    private String rejectionReason;
    private String approvedBy;
    private LocalDateTime approvedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
    public LeaveRequest() {}
    public LeaveRequest(Long id, Employee employee, String leaveType,
                        LocalDate startDate, LocalDate endDate, String reason,
                        LeaveStatus status, String rejectionReason,
                        String approvedBy, LocalDateTime approvedAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id              = id;
        this.employee        = employee;
        this.leaveType       = leaveType;
        this.startDate       = startDate;
        this.endDate         = endDate;
        this.reason          = reason;
        this.status          = status;
        this.rejectionReason = rejectionReason;
        this.approvedBy      = approvedBy;
        this.approvedAt      = approvedAt;
        this.createdAt       = createdAt;
        this.updatedAt       = updatedAt;
    }

    // ✅ Getters
    public Long getId()                   { return id;              }
    public Employee getEmployee()         { return employee;        }
    public String getLeaveType()          { return leaveType;       }
    public LocalDate getStartDate()       { return startDate;       }
    public LocalDate getEndDate()         { return endDate;         }
    public String getReason()             { return reason;          }
    public LeaveStatus getStatus()        { return status;          }
    public String getRejectionReason()    { return rejectionReason; }
    public String getApprovedBy()         { return approvedBy;      }
    public LocalDateTime getApprovedAt()  { return approvedAt;      }
    public LocalDateTime getCreatedAt()   { return createdAt;       }
    public LocalDateTime getUpdatedAt()   { return updatedAt;       }

    // ✅ Setters
    public void setId(Long id)                             { this.id              = id;              }
    public void setEmployee(Employee employee)             { this.employee        = employee;        }
    public void setLeaveType(String leaveType)             { this.leaveType       = leaveType;       }
    public void setStartDate(LocalDate startDate)          { this.startDate       = startDate;       }
    public void setEndDate(LocalDate endDate)              { this.endDate         = endDate;         }
    public void setReason(String reason)                   { this.reason          = reason;          }
    public void setStatus(LeaveStatus status)              { this.status          = status;          }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setApprovedBy(String approvedBy)           { this.approvedBy      = approvedBy;      }
    public void setApprovedAt(LocalDateTime approvedAt)    { this.approvedAt      = approvedAt;      }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt       = createdAt;       }
    public void setUpdatedAt(LocalDateTime updatedAt)      { this.updatedAt       = updatedAt;       }

    public long getLeaveDays() {
        if (startDate == null || endDate == null) return 0;
        return startDate.datesUntil(endDate.plusDays(1)).count();
    }
}