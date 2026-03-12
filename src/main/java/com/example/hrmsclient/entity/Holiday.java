package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holidays",
    indexes = {
        @Index(name = "idx_holiday_date", columnList = "holidayDate"),
        @Index(name = "idx_holiday_year", columnList = "year")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 100)
    private String name;             // "Diwali", "Christmas", etc.

    @NotNull
    @Column(unique = true)
    private LocalDate holidayDate;

    @Column(length = 300)
    private String description;

    @Column(name = "year")
    private int year;                // derived from holidayDate for easy filtering

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HolidayType holidayType = HolidayType.PUBLIC; // PUBLIC, OPTIONAL, RESTRICTED

    private String createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Holiday() {}

    @PrePersist
    @PreUpdate
    private void deriveYear() {
        if (holidayDate != null) this.year = holidayDate.getYear();
    }

    // Getters
    public Long getId()                  { return id;          }
    public String getName()              { return name;        }
    public LocalDate getHolidayDate()    { return holidayDate; }
    public String getDescription()       { return description; }
    public int getYear()                 { return year;        }
    public HolidayType getHolidayType()  { return holidayType; }
    public String getCreatedBy()         { return createdBy;   }
    public LocalDateTime getCreatedAt()  { return createdAt;   }

    // Setters
    public void setId(Long id)                         { this.id          = id;          }
    public void setName(String name)                   { this.name        = name;        }
    public void setHolidayDate(LocalDate holidayDate)  { this.holidayDate = holidayDate; }
    public void setDescription(String description)     { this.description = description; }
    public void setYear(int year)                      { this.year        = year;        }
    public void setHolidayType(HolidayType type)       { this.holidayType = type;        }
    public void setCreatedBy(String createdBy)         { this.createdBy   = createdBy;   }
    public void setCreatedAt(LocalDateTime createdAt)  { this.createdAt   = createdAt;   }
}