package com.example.hrmsclient.controller;

import com.example.hrmsclient.entity.Form16Document;
import com.example.hrmsclient.repository.Form16DocumentRepository;
import com.example.hrmsclient.service.Form16UploadService;
import com.example.hrmsclient.service.Form16UploadService.BulkUploadResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AdminForm16Controller
 * Endpoints for ADMIN / HR users to upload and manage Form 16 documents.
 *
 * Mapped URLs (all under /api/admin/form16):
 *  - POST   /upload-bulk?fy=YYYY-YY              → Upload ZIP with multiple PDFs
 *  - POST   /upload/{employeeId}?fy=YYYY-YY      → Single PDF upload for one employee
 *  - GET    /list?fy=YYYY-YY                     → List all Form 16 docs for a FY
 *  - GET    /status?fy=YYYY-YY                   → Summary counts for a FY
 *  - DELETE /{id}                                → Delete a Form 16 record
 */
@RestController
@RequestMapping("/api/admin/form16")

public class AdminForm16Controller {

    private final Form16UploadService       uploadService;
    private final Form16DocumentRepository  form16Repository;

    public AdminForm16Controller(Form16UploadService uploadService,
                                 Form16DocumentRepository form16Repository) {
        this.uploadService    = uploadService;
        this.form16Repository = form16Repository;
    }

    // ── BULK UPLOAD (ZIP) ─────────────────────────────────────────────────────
    @PostMapping("/upload-bulk")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> uploadBulkZip(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fy") String financialYear,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        BulkUploadResult result =
                uploadService.bulkUploadFromZip(file, financialYear, userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "status",  "success",
                "message", "Bulk upload completed for FY " + financialYear,
                "data",    Map.of(
                        "financialYear", financialYear,
                        "success",       result.success(),
                        "failed",        result.failed(),
                        "skipped",       result.skipped(),
                        "successList",   result.successList(),
                        "errorList",     result.errorList(),
                        "skipList",      result.skipList()
                )
        ));
    }

    // ── SINGLE UPLOAD ─────────────────────────────────────────────────────────
    @PostMapping("/upload/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> uploadSingle(
            @PathVariable Long employeeId,
            @RequestParam("fy") String financialYear,
            @RequestParam("file") MultipartFile pdfFile,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        Form16Document doc =
                uploadService.singleUpload(employeeId, financialYear, pdfFile, userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "status",  "success",
                "message", "Form 16 uploaded successfully for FY " + financialYear,
                "data",    Map.of(
                        "id",            doc.getId(),
                        "employeeId",    doc.getEmployee().getEmployeeId(),
                        "employeeName",  doc.getEmployee().getFullName(),
                        "financialYear", doc.getFinancialYear(),
                        "fileName",      doc.getOriginalFileName(),
                        "uploadedAt",    doc.getUploadedAt().toString(),
                        "downloaded",    doc.isDownloaded()
                )
        ));
    }

    // ── LIST BY FINANCIAL YEAR ────────────────────────────────────────────────
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listByYear(@RequestParam("fy") String financialYear) {
        List<Form16Document> docs =
                form16Repository.findByFinancialYearOrderByEmployee_FirstNameAsc(financialYear);

        List<Map<String, Object>> data = docs.stream().map(d -> Map.<String, Object>of(
                "id",            d.getId(),
                "employeeId",    d.getEmployee().getEmployeeId(),
                "employeeName",  d.getEmployee().getFullName(),
                "financialYear", d.getFinancialYear(),
                "fileName",      d.getOriginalFileName(),
                "uploadedAt",    d.getUploadedAt() != null ? d.getUploadedAt().toString() : null,
                "downloaded",    d.isDownloaded()
        )).toList();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data",   data,
                "total",  data.size()
        ));
    }

    // ── STATUS SUMMARY FOR FY ─────────────────────────────────────────────────
    @GetMapping("/status")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<?> statusByYear(@RequestParam("fy") String financialYear) {
        long total      = form16Repository.countByFinancialYear(financialYear);
        long downloaded = form16Repository.countByFinancialYearAndDownloadedTrue(financialYear);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data",   Map.of(
                        "financialYear", financialYear,
                        "total",         total,
                        "downloaded",    downloaded
                )
        ));
    }

    // ── DELETE RECORD ─────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        form16Repository.deleteById(id);
        return ResponseEntity.ok(Map.of(
                "status",  "success",
                "message", "Form 16 record deleted"
        ));
    }
}