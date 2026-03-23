package com.example.hrmsclient.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "attendance",
    indexes = {
        @Index(name = "idx_att_emp",      columnList = "employee_id"),
        @Index(name = "idx_att_date",     columnList = "attendanceDate"),
        @Index(name = "idx_att_emp_date", columnList = "employee_id, attendanceDate", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference
    private Employee employee;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Size(max = 200)
    private String remarks;

    @Column(name = "login_photo_url", length = 500)
    private String loginPhotoUrl;

    @Column(name = "check_in_latitude")
    private Double checkInLatitude;

    @Column(name = "check_in_longitude")
    private Double checkInLongitude;

    @Column(name = "check_in_address", length = 500)
    private String checkInAddress;

    @Column(name = "working_hours")
    private String workingHours;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "last_edited_by", length = 100)
    private String lastEditedBy;           

    @Column(name = "last_edited_by_name", length = 100)
    private String lastEditedByName;       

    @Column(name = "last_edited_by_role", length = 30)
    private String lastEditedByRole;       

    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;    

    @Column(name = "edit_reason", length = 500)
    private String editReason;             

    @Column(name = "original_status", length = 20)
    private String originalStatus;        

    @Column(name = "edit_count")
    private Integer editCount = 0;         


    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    @Transient
    public String getWorkingHours() {
        if (checkIn == null) return "—";
        LocalDateTime endTime = (checkOut != null) ? checkOut : LocalDateTime.now();
        Duration duration = Duration.between(checkIn, endTime);
        return duration.toHours() + "h " + duration.toMinutesPart() + "m";
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public Employee getEmployee()              { return employee; }
    public void setEmployee(Employee e)        { this.employee = e; }

    public LocalDate getAttendanceDate()       { return attendanceDate; }
    public void setAttendanceDate(LocalDate d) { this.attendanceDate = d; }

    public LocalDateTime getCheckIn()          { return checkIn; }
    public void setCheckIn(LocalDateTime t)    { this.checkIn = t; }

    public LocalDateTime getCheckOut()         { return checkOut; }
    public void setCheckOut(LocalDateTime t)   { this.checkOut = t; }

    public AttendanceStatus getStatus()        { return status; }
    public void setStatus(AttendanceStatus s)  { this.status = s; }

    public String getRemarks()                 { return remarks; }
    public void setRemarks(String r)           { this.remarks = r; }

    public String getLoginPhotoUrl()           { return loginPhotoUrl; }
    public void setLoginPhotoUrl(String u)     { this.loginPhotoUrl = u; }

    public Double getCheckInLatitude()         { return checkInLatitude; }
    public void setCheckInLatitude(Double v)   { this.checkInLatitude = v; }

    public Double getCheckInLongitude()        { return checkInLongitude; }
    public void setCheckInLongitude(Double v)  { this.checkInLongitude = v; }

    public String getCheckInAddress()          { return checkInAddress; }
    public void setCheckInAddress(String v)    { this.checkInAddress = v; }

    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime v)  { this.createdAt = v; }

    // Audit getters & setters
    public String getLastEditedBy()                  { return lastEditedBy; }
    public void setLastEditedBy(String v)            { this.lastEditedBy = v; }

    public String getLastEditedByName()              { return lastEditedByName; }
    public void setLastEditedByName(String v)        { this.lastEditedByName = v; }

    public String getLastEditedByRole()              { return lastEditedByRole; }
    public void setLastEditedByRole(String v)        { this.lastEditedByRole = v; }

    public LocalDateTime getLastEditedAt()           { return lastEditedAt; }
    public void setLastEditedAt(LocalDateTime v)     { this.lastEditedAt = v; }

    public String getEditReason()                    { return editReason; }
    public void setEditReason(String v)              { this.editReason = v; }

    public String getOriginalStatus()                { return originalStatus; }
    public void setOriginalStatus(String v)          { this.originalStatus = v; }

    public Integer getEditCount()                    { return editCount; }
    public void setEditCount(Integer v)              { this.editCount = v; }
}