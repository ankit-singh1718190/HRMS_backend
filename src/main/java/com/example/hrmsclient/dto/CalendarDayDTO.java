package com.example.hrmsclient.dto;

import java.time.LocalDate;

public class CalendarDayDTO {

    private LocalDate date;
    private String dayStatus;    // PRESENT, ABSENT, LEAVE, HOLIDAY, WEEKEND, UPCOMING
    private String leaveType;    
    private String leaveStatus; 
    private String checkIn;      
    private String checkOut;     
    private String holidayName;  
    private String holidayType;  

    public CalendarDayDTO() {}
    public CalendarDayDTO(LocalDate date, String dayStatus, String leaveType,
                          String leaveStatus, String checkIn, String checkOut) {
        this.date        = date;
        this.dayStatus   = dayStatus;
        this.leaveType   = leaveType;
        this.leaveStatus = leaveStatus;
        this.checkIn     = checkIn;
        this.checkOut    = checkOut;
    }

    public LocalDate getDate()        { return date;        }
    public String getDayStatus()      { return dayStatus;   }
    public String getLeaveType()      { return leaveType;   }
    public String getLeaveStatus()    { return leaveStatus; }
    public String getCheckIn()        { return checkIn;     }
    public String getCheckOut()       { return checkOut;    }
    public String getHolidayName()    { return holidayName; }
    public String getHolidayType()    { return holidayType; }

    // ── Setters
    public void setDate(LocalDate date)           { this.date        = date;        }
    public void setDayStatus(String dayStatus)    { this.dayStatus   = dayStatus;   }
    public void setLeaveType(String leaveType)    { this.leaveType   = leaveType;   }
    public void setLeaveStatus(String leaveStatus){ this.leaveStatus = leaveStatus; }
    public void setCheckIn(String checkIn)        { this.checkIn     = checkIn;     }
    public void setCheckOut(String checkOut)      { this.checkOut    = checkOut;    }
    public void setHolidayName(String holidayName){ this.holidayName = holidayName; }
    public void setHolidayType(String holidayType){ this.holidayType = holidayType; }
}