package com.example.hrmsclient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequestDTO {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    public String getToken()                        { return token; }
    public void setToken(String token)              { this.token = token; }
    public String getNewPassword()                  { return newPassword; }
    public void setNewPassword(String newPassword)  { this.newPassword = newPassword; }
    public String getConfirmPassword()              { return confirmPassword; }
    public void setConfirmPassword(String v)        { this.confirmPassword = v; }
}