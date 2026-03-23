package com.example.hrmsclient.dto;

import java.time.LocalDate;

public class CalendarDayDTO {

    private LocalDate date;
    private String dayStatus;   
    private String leaveType;
    private String leaveStatus;
    private String checkIn;
    private String checkOut;
    private String holidayName;
    private String holidayType;

  
    private Long attendanceId;   // DB id of attendance record (null if no record)
    private boolean isEdited;    // true if this record was manually edited
    private String editedByName; // who last edited
    private String editedByRole; // their role
    private String editedAt;     // when it was edited

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
    public Long getAttendanceId()     { return attendanceId; }
    public boolean isEdited()         { return isEdited;    }
    public String getEditedByName()   { return editedByName; }
    public String getEditedByRole()   { return editedByRole; }
    public String getEditedAt()       { return editedAt;    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setDate(LocalDate date)             { this.date        = date;        }
    public void setDayStatus(String dayStatus)      { this.dayStatus   = dayStatus;   }
    public void setLeaveType(String leaveType)      { this.leaveType   = leaveType;   }
    public void setLeaveStatus(String leaveStatus)  { this.leaveStatus = leaveStatus; }
    public void setCheckIn(String checkIn)          { this.checkIn     = checkIn;     }
    public void setCheckOut(String checkOut)        { this.checkOut    = checkOut;    }
    public void setHolidayName(String holidayName)  { this.holidayName = holidayName; }
    public void setHolidayType(String holidayType)  { this.holidayType = holidayType; }
    public void setAttendanceId(Long attendanceId)  { this.attendanceId = attendanceId; }
    public void setEdited(boolean isEdited)         { this.isEdited    = isEdited;    }
    public void setEditedByName(String v)           { this.editedByName = v;          }
    public void setEditedByRole(String v)           { this.editedByRole = v;          }
    public void setEditedAt(String v)               { this.editedAt    = v;           }
}