package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "employees",
    indexes = {
        @Index(name = "idx_emp_id",     columnList = "employeeId",       unique = true),
        @Index(name = "idx_email",       columnList = "emailId"),
        @Index(name = "idx_dept",        columnList = "department"),
        @Index(name = "idx_status",      columnList = "employmentStatus"),
        @Index(name = "idx_deleted",     columnList = "deleted"),
        @Index(name = "idx_emp_type",    columnList = "employeeType")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Employee implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_id", unique = true, nullable = false)
    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Version
    private Long version;

    // Audit
    @CreatedDate  @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate                        private LocalDateTime updatedAt;
    @CreatedBy    @Column(updatable = false) private String createdBy;
    @LastModifiedBy                          private String updatedBy;

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

    @Column(name = "exit_date")
    private LocalDate exitDate;

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

    @NotBlank @Column(length = 30) private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false, length = 20)
    private EmployeeType employeeType = EmployeeType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    // Kept for backward compat; prefer exitDate
    private LocalDate resignationDate;
    private LocalDate lastWorkingDay;

    // Banking
    private String bankName;
    @Column(length = 500) private String accountNo;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$")
    @Column(length = 15) private String ifscCode;

    private String bankBranch;

    // Auth
    @Column(nullable = false)
    private String password;

    // File URLs
    @Column(length = 500) private String profilePhotoUrl;
    @Column(length = 500) private String document1Url;
    @Column(length = 500) private String document2Url;
    @Column(length = 500) private String document3Url;
    
    private LocalDate attendanceDate;

    // Relationships
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Attendance> attendances = new ArrayList<>();

    public Employee() {}

    public String getFullName() {
        return (prefix != null ? prefix + " " : "") + firstName + " " + lastName;
    }

    public void softDelete() {
        this.deleted          = true;
        this.deletedAt        = LocalDateTime.now();
        this.employmentStatus = EmploymentStatus.EXITED;
        this.exitDate         = LocalDate.now();
    }

    // ── UserDetails implementation ──────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(
                "ROLE_" + (role != null ? role.toUpperCase() : "EMPLOYEE")));
    }

    /** Spring Security username = emailId */
    @Override
    public String getUsername() {
        return emailId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !deleted;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !deleted;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public Long getId()                           { return id; }
    public void setId(Long id)                    { this.id = id; }

    public String getEmployeeId()                 { return employeeId; }
    public void setEmployeeId(String employeeId)  { this.employeeId = employeeId; }

    public Long getVersion()                      { return version; }
    public void setVersion(Long version)          { this.version = version; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime v)     { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()           { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)     { this.updatedAt = v; }

    public String getCreatedBy()                  { return createdBy; }
    public void setCreatedBy(String v)            { this.createdBy = v; }

    public String getUpdatedBy()                  { return updatedBy; }
    public void setUpdatedBy(String v)            { this.updatedBy = v; }

    public boolean isDeleted()                    { return deleted; }
    public void setDeleted(boolean deleted)       { this.deleted = deleted; }

    public LocalDateTime getDeletedAt()           { return deletedAt; }
    public void setDeletedAt(LocalDateTime v)     { this.deletedAt = v; }

    public String getPrefix()                     { return prefix; }
    public void setPrefix(String prefix)          { this.prefix = prefix; }

    public String getFirstName()                  { return firstName; }
    public void setFirstName(String firstName)    { this.firstName = firstName; }

    public String getLastName()                   { return lastName; }
    public void setLastName(String lastName)      { this.lastName = lastName; }

    public String getEmailId()                    { return emailId; }
    public void setEmailId(String emailId)        { this.emailId = emailId; }

    public String getContactNumber1()             { return contactNumber1; }
    public void setContactNumber1(String v)       { this.contactNumber1 = v; }

    public String getGender()                     { return gender; }
    public void setGender(String gender)          { this.gender = gender; }

    public LocalDate getDateOfBirth()             { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dob)     { this.dateOfBirth = dob; }

    public String getNationality()                { return nationality; }
    public void setNationality(String v)          { this.nationality = v; }

    public String getWorkEmail()                  { return workEmail; }
    public void setWorkEmail(String workEmail)    { this.workEmail = workEmail; }

    public LocalDate getJoiningDate()             { return joiningDate; }
    public void setJoiningDate(LocalDate v)       { this.joiningDate = v; }

    public LocalDate getExitDate()                { return exitDate; }
    public void setExitDate(LocalDate exitDate)   { this.exitDate = exitDate; }

    public String getHouseNo()                    { return houseNo; }
    public void setHouseNo(String houseNo)        { this.houseNo = houseNo; }

    public String getCity()                       { return city; }
    public void setCity(String city)              { this.city = city; }

    public String getState()                      { return state; }
    public void setState(String state)            { this.state = state; }

    public String getPanNumber()                  { return panNumber; }
    public void setPanNumber(String v)            { this.panNumber = v; }

    public String getAadharNumber()               { return aadharNumber; }
    public void setAadharNumber(String v)         { this.aadharNumber = v; }

    public String getPassportNumber()             { return passportNumber; }
    public void setPassportNumber(String v)       { this.passportNumber = v; }

    public String getFatherName()                 { return fatherName; }
    public void setFatherName(String v)           { this.fatherName = v; }

    public String getMotherName()                 { return motherName; }
    public void setMotherName(String v)           { this.motherName = v; }

    public String getMaritalStatus()              { return maritalStatus; }
    public void setMaritalStatus(String v)        { this.maritalStatus = v; }

    public String getPreviousCompanyName()        { return previousCompanyName; }
    public void setPreviousCompanyName(String v)  { this.previousCompanyName = v; }

    public String getPreviousExperience()         { return previousExperience; }
    public void setPreviousExperience(String v)   { this.previousExperience = v; }

    public String getDepartment()                 { return department; }
    public void setDepartment(String department)  { this.department = department; }

    public String getDesignation()                { return designation; }
    public void setDesignation(String v)          { this.designation = v; }

    public String getPreviousCtc()                { return previousCtc; }
    public void setPreviousCtc(String v)          { this.previousCtc = v; }

    public String getHigherQualification()        { return higherQualification; }
    public void setHigherQualification(String v)  { this.higherQualification = v; }

    public String getRole()                       { return role; }
    public void setRole(String role)              { this.role = role; }

    public EmployeeType getEmployeeType()                  { return employeeType; }
    public void setEmployeeType(EmployeeType employeeType) { this.employeeType = employeeType; }

    public EmploymentStatus getEmploymentStatus()              { return employmentStatus; }
    public void setEmploymentStatus(EmploymentStatus v)        { this.employmentStatus = v; }

    public LocalDate getResignationDate()              { return resignationDate; }
    public void setResignationDate(LocalDate v)        { this.resignationDate = v; }

    public LocalDate getLastWorkingDay()               { return lastWorkingDay; }
    public void setLastWorkingDay(LocalDate v)         { this.lastWorkingDay = v; }

    public String getBankName()                        { return bankName; }
    public void setBankName(String bankName)           { this.bankName = bankName; }

    public String getAccountNo()                       { return accountNo; }
    public void setAccountNo(String accountNo)         { this.accountNo = accountNo; }

    public String getIfscCode()                        { return ifscCode; }
    public void setIfscCode(String ifscCode)           { this.ifscCode = ifscCode; }

    public String getBankBranch()                      { return bankBranch; }
    public void setBankBranch(String bankBranch)       { this.bankBranch = bankBranch; }

    public String getPassword()                        { return password; }
    public void setPassword(String password)           { this.password = password; }

    public String getProfilePhotoUrl()                 { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String v)           { this.profilePhotoUrl = v; }

    public String getDocument1Url()                    { return document1Url; }
    public void setDocument1Url(String v)              { this.document1Url = v; }

    public String getDocument2Url()                    { return document2Url; }
    public void setDocument2Url(String v)              { this.document2Url = v; }

    public String getDocument3Url()                    { return document3Url; }
    public void setDocument3Url(String v)              { this.document3Url = v; }

    public List<LeaveRequest> getLeaveRequests()       { return leaveRequests; }
    public void setLeaveRequests(List<LeaveRequest> v) { this.leaveRequests = v; }

    public List<Attendance> getAttendances()           { return attendances; }
    public void setAttendances(List<Attendance> v)     { this.attendances = v; }

    public Employee getManager()                       { return manager; }
    public void setManager(Employee manager)           { this.manager = manager; }

	public LocalDate getAttendanceDate() {
		return attendanceDate;
	}

	public void setAttendanceDate(LocalDate attendanceDate) {
		this.attendanceDate = attendanceDate;
	}
    
}