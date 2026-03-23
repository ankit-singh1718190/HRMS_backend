package com.example.hrmsclient.dto;

import com.example.hrmsclient.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class EditAttendanceRequestDTO {

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    @NotNull(message = "Reason is required")
    @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
    private String reason;

    private String remarks;

    public AttendanceStatus getStatus()            { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public LocalDateTime getCheckIn()              { return checkIn; }
    public void setCheckIn(LocalDateTime checkIn)  { this.checkIn = checkIn; }

    public LocalDateTime getCheckOut()             { return checkOut; }
    public void setCheckOut(LocalDateTime checkOut){ this.checkOut = checkOut; }

    public String getReason()                      { return reason; }
    public void setReason(String reason)           { this.reason = reason; }

    public String getRemarks()                     { return remarks; }
    public void setRemarks(String remarks)         { this.remarks = remarks; }
}