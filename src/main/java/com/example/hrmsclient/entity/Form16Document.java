package com.example.hrmsclient.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "form16_documents")
public class Form16Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 10)
    private String financialYear;

    // Original file name uploaded by admin
    @Column(nullable = false)
    private String originalFileName;

    // Stored file path on disk
    @Column(nullable = false)
    private String filePath;

    // File size in bytes
    private Long fileSize;

    // Who uploaded
    @Column(nullable = false)
    private String uploadedBy;

    // When uploaded
    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    // Has the employee downloaded it?
    private boolean downloaded = false;

    private LocalDateTime downloadedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public boolean isDownloaded() { return downloaded; }
    public void setDownloaded(boolean downloaded) { this.downloaded = downloaded; }

    public LocalDateTime getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(LocalDateTime downloadedAt) { this.downloadedAt = downloadedAt; }
}