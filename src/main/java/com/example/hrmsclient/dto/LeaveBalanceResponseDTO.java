package com.example.hrmsclient.dto;

public class LeaveBalanceResponseDTO {

    private Long id;
    private Long employeeId;
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getLeaveType() {
		return leaveType;
	}

	public void setLeaveType(String leaveType) {
		this.leaveType = leaveType;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getTotalAllocated() {
		return totalAllocated;
	}

	public void setTotalAllocated(int totalAllocated) {
		this.totalAllocated = totalAllocated;
	}

	public int getUsedPaidDays() {
		return usedPaidDays;
	}

	public void setUsedPaidDays(int usedPaidDays) {
		this.usedPaidDays = usedPaidDays;
	}

	public int getRemainingPaidDays() {
		return remainingPaidDays;
	}

	public void setRemainingPaidDays(int remainingPaidDays) {
		this.remainingPaidDays = remainingPaidDays;
	}

	public int getUsedUnpaidDays() {
		return usedUnpaidDays;
	}

	public void setUsedUnpaidDays(int usedUnpaidDays) {
		this.usedUnpaidDays = usedUnpaidDays;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	private String employeeName;
    private String leaveType;
    private int year;
    private int month;
    private int totalAllocated;
    private int usedPaidDays;
    private int remainingPaidDays;
    private int usedUnpaidDays;
    private String note;

    public LeaveBalanceResponseDTO() {}

    public LeaveBalanceResponseDTO(Long id, Long employeeId, String employeeName,
                                   String leaveType, int year, int month,
                                   int totalAllocated, int usedPaidDays,
                                   int remainingPaidDays, int usedUnpaidDays,
                                   String note) {

        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.leaveType = leaveType;
        this.year = year;
        this.month = month;
        this.totalAllocated = totalAllocated;
        this.usedPaidDays = usedPaidDays;
        this.remainingPaidDays = remainingPaidDays;
        this.usedUnpaidDays = usedUnpaidDays;
        this.note = note;
    }

	
}