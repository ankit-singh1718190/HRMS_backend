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
@Table(name = "hrms_attendance",
    indexes = {
        @Index(name = "idx_att_emp",      columnList = "employee_id"),
        @Index(name = "idx_att_date",     columnList = "attendanceDate"),
        @Index(name = "idx_att_emp_date", columnList = "employee_id, attendanceDate", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "att_seq")
    @SequenceGenerator(name = "att_seq", sequenceName = "att_seq", allocationSize = 1)
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

    public void setWorkingHours(String workingHours) {
		this.workingHours = workingHours;
	}
	@CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

	@Transient
	public String getWorkingHours() {

	    if (checkIn == null) {
	        return "0h 0m";
	    }

	    LocalDateTime endTime = (checkOut != null) ? checkOut : LocalDateTime.now();

	    Duration duration = Duration.between(checkIn, endTime);

	    long hours = duration.toHours();
	    long minutes = duration.toMinutesPart();

	    return hours + "h " + minutes + "m";
	}
    public Long getId()                        { return id; }
    public Employee getEmployee()              { return employee; }
    public LocalDate getAttendanceDate()       { return attendanceDate; }
    public LocalDateTime getCheckIn()          { return checkIn; }
    public LocalDateTime getCheckOut()         { return checkOut; }
    public AttendanceStatus getStatus()        { return status; }
    public String getRemarks()                 { return remarks; }
    public String getLoginPhotoUrl()           { return loginPhotoUrl; }       
    public Double getCheckInLatitude()         { return checkInLatitude; }     
    public Double getCheckInLongitude()        { return checkInLongitude; }    
    public String getCheckInAddress()          { return checkInAddress; }      
    public LocalDateTime getCreatedAt()        { return createdAt; }

    public void setId(Long id)                              { this.id = id; }
    public void setEmployee(Employee employee)              { this.employee = employee; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public void setCheckIn(LocalDateTime checkIn)           { this.checkIn = checkIn; }
    public void setCheckOut(LocalDateTime checkOut)         { this.checkOut = checkOut; }
    public void setStatus(AttendanceStatus status)          { this.status = status; }
    public void setRemarks(String remarks)                  { this.remarks = remarks; }
    public void setLoginPhotoUrl(String loginPhotoUrl)      { this.loginPhotoUrl = loginPhotoUrl; }       // ✅
    public void setCheckInLatitude(Double checkInLatitude)  { this.checkInLatitude = checkInLatitude; }   // ✅
    public void setCheckInLongitude(Double checkInLongitude){ this.checkInLongitude = checkInLongitude; } // ✅
    public void setCheckInAddress(String checkInAddress)    { this.checkInAddress = checkInAddress; }     // ✅
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }
}