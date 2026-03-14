package com.example.hrmsclient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeResponseDTO {

    private Long          id;
    private String        employeeId;
    private String        fullName;
    private String        firstName;
    private String        lastName;
    private String        emailId;
    private String        contactNumber1;
    private String        gender;
    private LocalDate     dateOfBirth;
    private String        workEmail;
    private LocalDate     joiningDate;
    private LocalDate     exitDate;           
    private String        department;
    private String        designation;
    private String        role;
    private String        employeeType;       
    private String        reportingManager;   
    private String        employmentStatus;
    private String        city;
    private String        state;
    private String        bankName;
    private String        profilePhotoUrl;
    private LocalDateTime createdAt;

    public EmployeeResponseDTO() {}

    // ── Getters 
    public Long getId()                      { return id; }
    public String getEmployeeId()            { return employeeId; }
    public String getFullName()              { return fullName; }
    public String getFirstName()             { return firstName; }
    public String getLastName()              { return lastName; }
    public String getEmailId()               { return emailId; }
    public String getContactNumber1()        { return contactNumber1; }
    public String getGender()                { return gender; }
    public LocalDate getDateOfBirth()        { return dateOfBirth; }
    public String getWorkEmail()             { return workEmail; }
    public LocalDate getJoiningDate()        { return joiningDate; }
    public LocalDate getExitDate()           { return exitDate; }
    public String getDepartment()            { return department; }
    public String getDesignation()           { return designation; }
    public String getRole()                  { return role; }
    public String getEmployeeType()          { return employeeType; }
    public String getReportingManager()      { return reportingManager; }
    public String getEmploymentStatus()      { return employmentStatus; }
    public String getCity()                  { return city; }
    public String getState()                 { return state; }
    public String getBankName()              { return bankName; }
    public String getProfilePhotoUrl()       { return profilePhotoUrl; }
    public LocalDateTime getCreatedAt()      { return createdAt; }

    // ── Setters 
    public void setId(Long id)                           { this.id               = id; }
    public void setEmployeeId(String v)                  { this.employeeId       = v; }
    public void setFullName(String v)                    { this.fullName         = v; }
    public void setFirstName(String v)                   { this.firstName        = v; }
    public void setLastName(String v)                    { this.lastName         = v; }
    public void setEmailId(String v)                     { this.emailId          = v; }
    public void setContactNumber1(String v)              { this.contactNumber1   = v; }
    public void setGender(String v)                      { this.gender           = v; }
    public void setDateOfBirth(LocalDate v)              { this.dateOfBirth      = v; }
    public void setWorkEmail(String v)                   { this.workEmail        = v; }
    public void setJoiningDate(LocalDate v)              { this.joiningDate      = v; }
    public void setExitDate(LocalDate v)                 { this.exitDate         = v; }
    public void setDepartment(String v)                  { this.department       = v; }
    public void setDesignation(String v)                 { this.designation      = v; }
    public void setRole(String v)                        { this.role             = v; }
    public void setEmployeeType(String v)                { this.employeeType     = v; }
    public void setReportingManager(String v)            { this.reportingManager = v; }
    public void setEmploymentStatus(String v)            { this.employmentStatus = v; }
    public void setCity(String v)                        { this.city             = v; }
    public void setState(String v)                       { this.state            = v; }
    public void setBankName(String v)                    { this.bankName         = v; }
    public void setProfilePhotoUrl(String v)             { this.profilePhotoUrl  = v; }
    public void setCreatedAt(LocalDateTime v)            { this.createdAt        = v; }
}