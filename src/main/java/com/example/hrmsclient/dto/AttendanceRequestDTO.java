package com.example.hrmsclient.dto;

import com.example.hrmsclient.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;


public class AttendanceRequestDTO {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;          

    @NotNull(message = "Date is required")
    private LocalDate date;           

    @NotNull(message = "Status is required")
    private AttendanceStatus status; 

    private LocalDateTime inTime;     
    private LocalDateTime outTime;   
    private String remarks;

    public AttendanceRequestDTO() {}

    public Long getEmployeeId()               { return employeeId; }
    public void setEmployeeId(Long v)         { this.employeeId = v; }

    public LocalDate getDate()                { return date; }
    public void setDate(LocalDate v)          { this.date = v; }

    public AttendanceStatus getStatus()       { return status; }
    public void setStatus(AttendanceStatus v) { this.status = v; }

    public LocalDateTime getInTime()          { return inTime; }
    public void setInTime(LocalDateTime v)    { this.inTime = v; }

    public LocalDateTime getOutTime()         { return outTime; }
    public void setOutTime(LocalDateTime v)   { this.outTime = v; }

    public String getRemarks()                { return remarks; }
    public void setRemarks(String v)          { this.remarks = v; }
}