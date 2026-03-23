package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }
    public String getToken()                     { return token; }
    public void setToken(String token)           { this.token = token; }
    public LocalDateTime getExpiresAt()          { return expiresAt; }
    public void setExpiresAt(LocalDateTime v)    { this.expiresAt = v; }
    public boolean isUsed()                      { return used; }
    public void setUsed(boolean used)            { this.used = used; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}