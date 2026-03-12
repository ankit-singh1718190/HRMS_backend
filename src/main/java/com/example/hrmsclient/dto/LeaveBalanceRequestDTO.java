package com.example.hrmsclient.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LeaveBalanceRequestDTO {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Leave type is required")
    private String leaveType;

    @NotNull(message = "Year is required")
    private Integer year;

    // Nullable → null means yearly allocation
    private Integer month;

    @NotNull(message = "Total allocated leave is required")
    @Min(value = 0, message = "Total allocated must be >= 0")
    private Integer totalAllocated;

    private String note;

    public LeaveBalanceRequestDTO() {
    }

    public LeaveBalanceRequestDTO(Long employeeId,
                                  String leaveType,
                                  Integer year,
                                  Integer month,
                                  Integer totalAllocated,
                                  String note) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.year = year;
        this.month = month;
        this.totalAllocated = totalAllocated;
        this.note = note;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(Integer totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}