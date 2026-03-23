package com.example.hrmsclient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdatePasswordRequestDTO {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    public String getCurrentPassword()             { return currentPassword; }
    public void setCurrentPassword(String v)       { this.currentPassword = v; }
    public String getNewPassword()                 { return newPassword; }
    public void setNewPassword(String v)           { this.newPassword = v; }
    public String getConfirmPassword()             { return confirmPassword; }
    public void setConfirmPassword(String v)       { this.confirmPassword = v; }
}