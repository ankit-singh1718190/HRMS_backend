package com.example.hrmsclient.dto;

import com.example.hrmsclient.entity.EmployeeType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class EmployeeRequestDTO {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 20)
    private String employeeId;

    @Size(max = 10)
    private String prefix;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Email @NotBlank(message = "Email is required")
    private String emailId;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number")
    private String contactNumber1;

    private String gender;

    @Past
    private LocalDate dateOfBirth;

    private String nationality;

    @Email
    private String workEmail;

    @PastOrPresent
    private LocalDate joiningDate;

    /** NEW: Exit date — set when employee leaves the organisation */
    private LocalDate exitDate;

    private String houseNo;
    private String city;
    private String state;
    private String panNumber;
    private String aadharNumber;
    private String passportNumber;
    private String fatherName;
    private String motherName;
    private String maritalStatus;
    private String previousCompanyName;
    private String previousExperience;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Designation is required")
    private String designation;

    private String previousCtc;
    private String higherQualification;

    @NotBlank(message = "Role is required")
    private String role;

    /**
     * NEW: Employee type — drives leave entitlements and payroll rules.
     * Allowed values: FULL_TIME | CONTRACT | TEMPORARY | INTERN
     */
    @NotNull(message = "Employee type is required")
    private EmployeeType employeeType;

    /**
     * NEW: Reporting manager's employeeId (e.g. "EMP005").
     * Used for manager-level leave approval.
     */
    private String reportingManager;

    private String bankName;
    private String accountNo;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code")
    private String ifscCode;

    private String bankBranch;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    public EmployeeRequestDTO() {}

    // ── Getters 
    public String getEmployeeId()           { return employeeId; }
    public String getPrefix()               { return prefix; }
    public String getFirstName()            { return firstName; }
    public String getLastName()             { return lastName; }
    public String getEmailId()              { return emailId; }
    public String getContactNumber1()       { return contactNumber1; }
    public String getGender()               { return gender; }
    public LocalDate getDateOfBirth()       { return dateOfBirth; }
    public String getNationality()          { return nationality; }
    public String getWorkEmail()            { return workEmail; }
    public LocalDate getJoiningDate()       { return joiningDate; }
    public LocalDate getExitDate()          { return exitDate; }
    public String getHouseNo()              { return houseNo; }
    public String getCity()                 { return city; }
    public String getState()                { return state; }
    public String getPanNumber()            { return panNumber; }
    public String getAadharNumber()         { return aadharNumber; }
    public String getPassportNumber()       { return passportNumber; }
    public String getFatherName()           { return fatherName; }
    public String getMotherName()           { return motherName; }
    public String getMaritalStatus()        { return maritalStatus; }
    public String getPreviousCompanyName()  { return previousCompanyName; }
    public String getPreviousExperience()   { return previousExperience; }
    public String getDepartment()           { return department; }
    public String getDesignation()          { return designation; }
    public String getPreviousCtc()          { return previousCtc; }
    public String getHigherQualification()  { return higherQualification; }
    public String getRole()                 { return role; }
    public EmployeeType getEmployeeType()   { return employeeType; }
    public String getReportingManager()     { return reportingManager; }
    public String getBankName()             { return bankName; }
    public String getAccountNo()            { return accountNo; }
    public String getIfscCode()             { return ifscCode; }
    public String getBankBranch()           { return bankBranch; }
    public String getPassword()             { return password; }

    // ── Setters
    public void setEmployeeId(String employeeId)                  { this.employeeId          = employeeId; }
    public void setPrefix(String prefix)                          { this.prefix              = prefix; }
    public void setFirstName(String firstName)                    { this.firstName           = firstName; }
    public void setLastName(String lastName)                      { this.lastName            = lastName; }
    public void setEmailId(String emailId)                        { this.emailId             = emailId; }
    public void setContactNumber1(String v)                       { this.contactNumber1      = v; }
    public void setGender(String gender)                          { this.gender              = gender; }
    public void setDateOfBirth(LocalDate dateOfBirth)             { this.dateOfBirth         = dateOfBirth; }
    public void setNationality(String nationality)                { this.nationality         = nationality; }
    public void setWorkEmail(String workEmail)                    { this.workEmail           = workEmail; }
    public void setJoiningDate(LocalDate joiningDate)             { this.joiningDate         = joiningDate; }
    public void setExitDate(LocalDate exitDate)                   { this.exitDate            = exitDate; }
    public void setHouseNo(String houseNo)                        { this.houseNo             = houseNo; }
    public void setCity(String city)                              { this.city                = city; }
    public void setState(String state)                            { this.state               = state; }
    public void setPanNumber(String v)                            { this.panNumber           = v; }
    public void setAadharNumber(String v)                         { this.aadharNumber        = v; }
    public void setPassportNumber(String v)                       { this.passportNumber      = v; }
    public void setFatherName(String v)                           { this.fatherName          = v; }
    public void setMotherName(String v)                           { this.motherName          = v; }
    public void setMaritalStatus(String v)                        { this.maritalStatus       = v; }
    public void setPreviousCompanyName(String v)                  { this.previousCompanyName = v; }
    public void setPreviousExperience(String v)                   { this.previousExperience  = v; }
    public void setDepartment(String department)                  { this.department          = department; }
    public void setDesignation(String designation)                { this.designation         = designation; }
    public void setPreviousCtc(String v)                          { this.previousCtc         = v; }
    public void setHigherQualification(String v)                  { this.higherQualification = v; }
    public void setRole(String role)                              { this.role                = role; }
    public void setEmployeeType(EmployeeType employeeType)        { this.employeeType        = employeeType; }
    public void setReportingManager(String reportingManager)      { this.reportingManager    = reportingManager; }
    public void setBankName(String bankName)                      { this.bankName            = bankName; }
    public void setAccountNo(String accountNo)                    { this.accountNo           = accountNo; }
    public void setIfscCode(String ifscCode)                      { this.ifscCode            = ifscCode; }
    public void setBankBranch(String bankBranch)                  { this.bankBranch          = bankBranch; }
    public void setPassword(String password)                      { this.password            = password; }
}