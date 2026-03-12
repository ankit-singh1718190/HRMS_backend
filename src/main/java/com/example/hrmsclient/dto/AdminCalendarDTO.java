package com.example.hrmsclient.dto;

import java.util.List;

public class AdminCalendarDTO {

    private List<LeaveResponseDTO> leaves;
    private List<HolidayDTO> holidays;

    public AdminCalendarDTO() {}

    public AdminCalendarDTO(List<LeaveResponseDTO> leaves, List<HolidayDTO> holidays) {
        this.leaves   = leaves;
        this.holidays = holidays;
    }

    public List<LeaveResponseDTO> getLeaves()            { return leaves;   }
    public List<HolidayDTO> getHolidays()                { return holidays; }

    public void setLeaves(List<LeaveResponseDTO> leaves) { this.leaves   = leaves;   }
    public void setHolidays(List<HolidayDTO> holidays)   { this.holidays = holidays; }
}