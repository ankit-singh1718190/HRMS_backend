package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin",
    indexes = {
        @Index(name = "idx_admin_email", columnList = "emailId", unique = true),
        @Index(name = "idx_admin_id",    columnList = "adminId", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Admin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(name = "admin_id", unique = true, nullable = false, length = 20)
    private String adminId;            // ADM001

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Email @NotBlank
    @Column(name = "email_id", unique = true, nullable = false, length = 100)
    private String emailId;

    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "role", nullable = false, length = 20)
    private String role = "ADMIN";     // ADMIN or SUPER_ADMIN

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreatedDate  @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getFullName() { return firstName + " " + lastName; }

    // Getters & Setters
    public Long getId()                               { return id; }
    public void setId(Long id)                        { this.id = id; }
    public String getAdminId()                        { return adminId; }
    public void setAdminId(String v)                  { this.adminId = v; }
    public String getFirstName()                      { return firstName; }
    public void setFirstName(String v)                { this.firstName = v; }
    public String getLastName()                       { return lastName; }
    public void setLastName(String v)                 { this.lastName = v; }
    public String getEmailId()                        { return emailId; }
    public void setEmailId(String v)                  { this.emailId = v; }
    public String getPassword()                       { return password; }
    public void setPassword(String v)                 { this.password = v; }
    public String getPhone()                          { return phone; }
    public void setPhone(String v)                    { this.phone = v; }
    public String getProfilePhotoUrl()                { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String v)          { this.profilePhotoUrl = v; }
    public String getRole()                           { return role; }
    public void setRole(String v)                     { this.role = v; }
    public String getDepartment()                     { return department; }
    public void setDepartment(String v)               { this.department = v; }
    public String getDesignation()                    { return designation; }
    public void setDesignation(String v)              { this.designation = v; }
    public boolean isActive()                         { return active; }
    public void setActive(boolean v)                  { this.active = v; }
    public boolean isDeleted()                        { return deleted; }
    public void setDeleted(boolean v)                 { this.deleted = v; }
    public LocalDateTime getDeletedAt()               { return deletedAt; }
    public void setDeletedAt(LocalDateTime v)         { this.deletedAt = v; }
    public LocalDateTime getLastLoginAt()             { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v)       { this.lastLoginAt = v; }
    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime v)         { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()               { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)         { this.updatedAt = v; }
}