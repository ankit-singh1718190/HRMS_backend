package com.example.hrmsclient.dto;

public class LeaveDeductionResultDTO {

    private int paidDays;
    private int unpaidDays;

    public LeaveDeductionResultDTO(int paidDays, int unpaidDays) {
        this.paidDays = paidDays;
        this.unpaidDays = unpaidDays;
    }

    public int getPaidDays() {
        return paidDays;
    }

    public int getUnpaidDays() {
        return unpaidDays;
    }
}