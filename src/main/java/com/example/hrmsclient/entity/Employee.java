package com.example.hrmsclient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "employees",
    indexes = {
        @Index(name = "idx_emp_id",     columnList = "employeeId",       unique = true),
        @Index(name = "idx_email",       columnList = "emailId"),
        @Index(name = "idx_dept",        columnList = "department"),
        @Index(name = "idx_status",      columnList = "employmentStatus"),
        @Index(name = "idx_deleted",     columnList = "deleted")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @Column(name = "employee_id", unique = true, nullable = true)
    private String employeeId;

    @Version
    private Long version;

    // Audit 
    @CreatedDate  @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate                        private LocalDateTime updatedAt;
    @CreatedBy    @Column(updatable = false) private String createdBy;
    @LastModifiedBy                          private String updatedBy;

    //Soft Delete
    @Column(nullable = false)
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    // ── Personal
    private String prefix;
    private String firstName;
    private String lastName;

    @Email @NotBlank
    @Column(name = "email_id", nullable = false, unique = true)
    private String emailId;

    @Pattern(regexp = "^[6-9]\\d{9}$")
    @Column(length = 15)
    private String contactNumber1;

    @Column(length = 10) private String gender;
    @Past                private LocalDate dateOfBirth;
    private String nationality;

    @Email @Column(unique = true)
    private String workEmail;

    @PastOrPresent private LocalDate joiningDate;

    // ── Address
    private String houseNo;
    private String city;
    private String state;

    // ── Sensitive 
    @Column(length = 500) private String panNumber;
    @Column(length = 500) private String aadharNumber;
    @Column(length = 500) private String passportNumber;

    // ── Family
    private String fatherName;
    private String motherName;
    private String maritalStatus;

    // Employment 
    private String previousCompanyName;
    private String previousExperience;

    @NotBlank @Column(length = 100) private String department;
    @NotBlank @Column(length = 100) private String designation;

    private String previousCtc;
    private String higherQualification;
    @Min(0) private Long basicEmployeeSalary;
    @NotBlank @Column(length = 30) private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    private LocalDate resignationDate;
    private LocalDate lastWorkingDay;

    //  Banking 
    private String bankName;
    @Column(length = 500) private String accountNo;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$")
    @Column(length = 15) private String ifscCode;

    private String bankBranch;

    //  Auth
    @Column(nullable = false)
    private String password;

    // File URLs 
    @Column(length = 500) private String profilePhotoUrl;
    @Column(length = 500) private String document1Url;
    @Column(length = 500) private String document2Url;
    @Column(length = 500) private String document3Url;

    //  Relationships
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)

    private List<Attendance> attendances = new ArrayList<>();

    // Constructors
    public Employee() {}

    // Helpers
    public String getFullName() {
        return (prefix != null ? prefix + " " : "") + firstName + " " + lastName;
    }

    public void softDelete() {
        this.deleted    = true;
        this.deletedAt  = LocalDateTime.now();
        this.employmentStatus = EmploymentStatus.EXITED;
    }

    // Getters
    public Long getId()                        { return id;                  }
    public String getEmployeeId()              { return employeeId;          }
    public Long getVersion()                   { return version;             }
    public LocalDateTime getCreatedAt()        { return createdAt;           }
    public LocalDateTime getUpdatedAt()        { return updatedAt;           }
    public String getCreatedBy()               { return createdBy;           }
    public String getUpdatedBy()               { return updatedBy;           }
    public boolean isDeleted()                 { return deleted;             }
    public LocalDateTime getDeletedAt()        { return deletedAt;           }
    public String getPrefix()                  { return prefix;              }
    public String getFirstName()               { return firstName;           }
    public String getLastName()                { return lastName;            }
    public String getEmailId()                 { return emailId;             }
    public String getContactNumber1()          { return contactNumber1;      }
    public String getGender()                  { return gender;              }
    public LocalDate getDateOfBirth()          { return dateOfBirth;         }
    public String getNationality()             { return nationality;         }
    public String getWorkEmail()               { return workEmail;           }
    public LocalDate getJoiningDate()          { return joiningDate;         }
    public String getHouseNo()                 { return houseNo;             }
    public String getCity()                    { return city;                }
    public String getState()                   { return state;               }
    public String getPanNumber()               { return panNumber;           }
    public String getAadharNumber()            { return aadharNumber;        }
    public String getPassportNumber()          { return passportNumber;      }
    public String getFatherName()              { return fatherName;          }
    public String getMotherName()              { return motherName;          }
    public String getMaritalStatus()           { return maritalStatus;       }
    public String getPreviousCompanyName()     { return previousCompanyName; }
    public String getPreviousExperience()      { return previousExperience;  }
    public String getDepartment()              { return department;          }
    public String getDesignation()             { return designation;         }
    public String getPreviousCtc()             { return previousCtc;         }
    public String getHigherQualification()     { return higherQualification; }
    public Long getBasicEmployeeSalary()       { return basicEmployeeSalary; }
    public String getRole()                    { return role;                }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }
    public LocalDate getResignationDate()      { return resignationDate;     }
    public LocalDate getLastWorkingDay()       { return lastWorkingDay;      }
    public String getBankName()                { return bankName;            }
    public String getAccountNo()               { return accountNo;           }
    public String getIfscCode()                { return ifscCode;            }
    public String getBankBranch()              { return bankBranch;          }
    public String getPassword()                { return password;            }
    public String getProfilePhotoUrl()         { return profilePhotoUrl;     }
    public String getDocument1Url()            { return document1Url;        }
    public String getDocument2Url()            { return document2Url;        }
    public String getDocument3Url()            { return document3Url;        }
    public List<LeaveRequest> getLeaveRequests() { return leaveRequests;     }
    public List<Attendance> getAttendances()   { return attendances;         }

    public void setId(Long id)                                   { this.id                = id;                }
    public void setEmployeeId(String employeeId)                 { this.employeeId        = employeeId;        }
    public void setVersion(Long version)                         { this.version           = version;           }
    public void setCreatedAt(LocalDateTime createdAt)            { this.createdAt         = createdAt;         }
    public void setUpdatedAt(LocalDateTime updatedAt)            { this.updatedAt         = updatedAt;         }
    public void setCreatedBy(String createdBy)                   { this.createdBy         = createdBy;         }
    public void setUpdatedBy(String updatedBy)                   { this.updatedBy         = updatedBy;         }
    public void setDeleted(boolean deleted)                      { this.deleted           = deleted;           }
    public void setDeletedAt(LocalDateTime deletedAt)            { this.deletedAt         = deletedAt;         }
    public void setPrefix(String prefix)                         { this.prefix            = prefix;            }
    public void setFirstName(String firstName)                   { this.firstName         = firstName;         }
    public void setLastName(String lastName)                     { this.lastName          = lastName;          }
    public void setEmailId(String emailId)                       { this.emailId           = emailId;           }
    public void setContactNumber1(String v)                      { this.contactNumber1    = v;                 }
    public void setGender(String gender)                         { this.gender            = gender;            }
    public void setDateOfBirth(LocalDate dateOfBirth)            { this.dateOfBirth       = dateOfBirth;       }
    public void setNationality(String nationality)               { this.nationality       = nationality;       }
    public void setWorkEmail(String workEmail)                   { this.workEmail         = workEmail;         }
    public void setJoiningDate(LocalDate joiningDate)            { this.joiningDate       = joiningDate;       }
    public void setHouseNo(String houseNo)                       { this.houseNo           = houseNo;           }
    public void setCity(String city)                             { this.city              = city;              }
    public void setState(String state)                           { this.state             = state;             }
    public void setPanNumber(String panNumber)                   { this.panNumber         = panNumber;         }
    public void setAadharNumber(String aadharNumber)             { this.aadharNumber      = aadharNumber;      }
    public void setPassportNumber(String passportNumber)         { this.passportNumber    = passportNumber;    }
    public void setFatherName(String fatherName)                 { this.fatherName        = fatherName;        }
    public void setMotherName(String motherName)                 { this.motherName        = motherName;        }
    public void setMaritalStatus(String maritalStatus)           { this.maritalStatus     = maritalStatus;     }
    public void setPreviousCompanyName(String v)                 { this.previousCompanyName = v;               }
    public void setPreviousExperience(String v)                  { this.previousExperience  = v;               }
    public void setDepartment(String department)                 { this.department        = department;        }
    public void setDesignation(String designation)               { this.designation       = designation;       }
    public void setPreviousCtc(String previousCtc)               { this.previousCtc       = previousCtc;       }
    public void setHigherQualification(String v)                 { this.higherQualification = v;               }
    public void setBasicEmployeeSalary(Long v)                   { this.basicEmployeeSalary = v;               }
    public void setRole(String role)                             { this.role              = role;              }
    public void setEmploymentStatus(EmploymentStatus v)          { this.employmentStatus  = v;                 }
    public void setResignationDate(LocalDate resignationDate)    { this.resignationDate   = resignationDate;   }
    public void setLastWorkingDay(LocalDate lastWorkingDay)      { this.lastWorkingDay    = lastWorkingDay;    }
    public void setBankName(String bankName)                     { this.bankName          = bankName;          }
    public void setAccountNo(String accountNo)                   { this.accountNo         = accountNo;         }
    public void setIfscCode(String ifscCode)                     { this.ifscCode          = ifscCode;          }
    public void setBankBranch(String bankBranch)                 { this.bankBranch        = bankBranch;        }
    public void setPassword(String password)                     { this.password          = password;          }
    public void setProfilePhotoUrl(String profilePhotoUrl)       { this.profilePhotoUrl   = profilePhotoUrl;   }
    public void setDocument1Url(String document1Url)             { this.document1Url      = document1Url;      }
    public void setDocument2Url(String document2Url)             { this.document2Url      = document2Url;      }
    public void setDocument3Url(String document3Url)             { this.document3Url      = document3Url;      }
    public void setLeaveRequests(List<LeaveRequest> v)           { this.leaveRequests     = v;                 }
    public void setAttendances(List<Attendance> attendances)     { this.attendances       = attendances;       }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", employeeId='" + employeeId
             + "', name='" + getFullName()
             + "', dept='" + department + "'}";
    }
}