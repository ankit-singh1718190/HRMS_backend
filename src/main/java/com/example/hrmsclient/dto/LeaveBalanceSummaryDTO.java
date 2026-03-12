package com.example.hrmsclient.dto;

import java.util.List;

public class LeaveBalanceSummaryDTO {

    private Long employeeId;
    private String employeeName;
    private int year;

    private List<LeaveBalanceResponseDTO> yearlyBalances;
    private List<LeaveBalanceResponseDTO> monthlyBalances;

    public LeaveBalanceSummaryDTO() {
    }

    public LeaveBalanceSummaryDTO(Long employeeId,
                                  String employeeName,
                                  int year,
                                  List<LeaveBalanceResponseDTO> yearlyBalances,
                                  List<LeaveBalanceResponseDTO> monthlyBalances) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.year = year;
        this.yearlyBalances = yearlyBalances;
        this.monthlyBalances = monthlyBalances;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<LeaveBalanceResponseDTO> getYearlyBalances() {
        return yearlyBalances;
    }

    public void setYearlyBalances(List<LeaveBalanceResponseDTO> yearlyBalances) {
        this.yearlyBalances = yearlyBalances;
    }

    public List<LeaveBalanceResponseDTO> getMonthlyBalances() {
        return monthlyBalances;
    }

    public void setMonthlyBalances(List<LeaveBalanceResponseDTO> monthlyBalances) {
        this.monthlyBalances = monthlyBalances;
    }
}