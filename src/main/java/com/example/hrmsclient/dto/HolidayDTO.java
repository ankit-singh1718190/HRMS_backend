package com.example.hrmsclient.dto;

import com.example.hrmsclient.entity.HolidayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class HolidayDTO {

    private Long id;                  // null on create, present on update/response

    @NotBlank(message = "Holiday name is required")
    private String name;

    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;

    private String description;

    private HolidayType holidayType = HolidayType.PUBLIC;

    public HolidayDTO() {}

    public HolidayDTO(Long id, String name, LocalDate holidayDate,
                      String description, HolidayType holidayType) {
        this.id          = id;
        this.name        = name;
        this.holidayDate = holidayDate;
        this.description = description;
        this.holidayType = holidayType;
    }

    public Long getId()                 { return id;          }
    public String getName()             { return name;        }
    public LocalDate getHolidayDate()   { return holidayDate; }
    public String getDescription()      { return description; }
    public HolidayType getHolidayType() { return holidayType; }

    public void setId(Long id)                        { this.id          = id;          }
    public void setName(String name)                  { this.name        = name;        }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
    public void setDescription(String description)    { this.description = description; }
    public void setHolidayType(HolidayType type)      { this.holidayType = type;        }
}