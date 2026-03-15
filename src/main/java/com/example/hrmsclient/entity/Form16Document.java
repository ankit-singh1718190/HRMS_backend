package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "form16_documents",
    indexes = {
        @Index(name = "idx_form16_emp", columnList = "employee_id"),
        @Index(name = "idx_form16_fy", columnList = "financialYear")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Form16Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(length = 20, nullable = false)
    private String financialYear;           // e.g. "2024-25"

    @Column(length = 255, nullable = false)
    private String originalFileName;

    @Column(length = 500, nullable = false)
    private String filePath;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 100)
    private String uploadedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    private boolean downloaded = false;

    private LocalDateTime downloadedAt;

    public Form16Document() {}

    // Getters
    public Long getId()                   { return id; }
    public Employee getEmployee()         { return employee; }
    public String getFinancialYear()      { return financialYear; }
    public String getOriginalFileName()   { return originalFileName; }
    public String getFilePath()           { return filePath; }
    public long getFileSize()             { return fileSize; }
    public String getUploadedBy()         { return uploadedBy; }
    public LocalDateTime getUploadedAt()  { return uploadedAt; }
    public boolean isDownloaded()         { return downloaded; }
    public LocalDateTime getDownloadedAt(){ return downloadedAt; }

    // Setters
    public void setId(Long id)                           { this.id = id; }
    public void setEmployee(Employee employee)           { this.employee = employee; }
    public void setFinancialYear(String financialYear)   { this.financialYear = financialYear; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public void setFilePath(String filePath)             { this.filePath = filePath; }
    public void setFileSize(long fileSize)               { this.fileSize = fileSize; }
    public void setUploadedBy(String uploadedBy)         { this.uploadedBy = uploadedBy; }
    public void setUploadedAt(LocalDateTime uploadedAt)  { this.uploadedAt = uploadedAt; }
    public void setDownloaded(boolean downloaded)        { this.downloaded = downloaded; }
    public void setDownloadedAt(LocalDateTime downloadedAt) { this.downloadedAt = downloadedAt; }
}

