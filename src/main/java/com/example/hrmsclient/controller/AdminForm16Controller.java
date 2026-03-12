package com.example.hrmsclient.controller;

import com.example.hrmsclient.entity.Form16Document;
import com.example.hrmsclient.repository.Form16DocumentRepository;
import com.example.hrmsclient.service.Form16UploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/form16")
public class AdminForm16Controller {

    private final Form16UploadService      uploadService;
    private final Form16DocumentRepository form16Repository;

    public AdminForm16Controller(Form16UploadService uploadService,
                                  Form16DocumentRepository form16Repository) {
        this.uploadService    = uploadService;
        this.form16Repository = form16Repository;
    }


    @PostMapping(value = "/upload-bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUpload(
            @RequestParam("file") MultipartFile zipFile,
            @RequestParam(defaultValue = "2024-25") String fy,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (zipFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status",  "error",
                "message", "ZIP file is empty"
            ));
        }

        if (!zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
            return ResponseEntity.badRequest().body(Map.of(
                "status",  "error",
                "message", "Only ZIP files are accepted for bulk upload"
            ));
        }

        try {
            Form16UploadService.BulkUploadResult result = uploadService.bulkUploadFromZip(
                    zipFile, fy, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                "status",       "success",
                "financialYear", result.financialYear(),
                "totalSuccess", result.success(),
                "totalFailed",  result.failed(),
                "totalSkipped", result.skipped(),
                "successList",  result.successList(),
                "errorList",    result.errorList(),
                "skipList",     result.skipList(),
                "message",      "Bulk upload complete: " + result.success() +
                                " uploaded, " + result.failed() + " failed, " +
                                result.skipped() + " skipped"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status",  "error",
                "message", "Bulk upload failed: " + e.getMessage()
            ));
        }
    }

    // ── SINGLE EMPLOYEE UPLOAD ────────────────────────────────────────────────
    /**
     * POST /api/admin/form16/upload/{employeeId}?fy=2024-25
     * Content-Type: multipart/form-data
     * Body: file = <PDF file>
     */
    @PostMapping(value = "/upload/{employeeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> singleUpload(
            @PathVariable Long employeeId,
            @RequestParam("file") MultipartFile pdfFile,
            @RequestParam(defaultValue = "2024-25") String fy,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of(
                "status",  "error",
                "message", "Only PDF files are accepted"
            ));
        }

        try {
            Form16Document doc = uploadService.singleUpload(
                    employeeId, fy, pdfFile, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                "status",       "success",
                "message",      "Form 16 uploaded for employee ID " + employeeId,
                "financialYear", doc.getFinancialYear(),
                "fileName",     doc.getOriginalFileName(),
                "uploadedAt",   doc.getUploadedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status",  "error",
                "message", e.getMessage()
            ));
        }
    }
    // GET /api/admin/form16/list?fy=2024-25
    @GetMapping("/list")
    public ResponseEntity<?> listByYear(
            @RequestParam(defaultValue = "2024-25") String fy) {

        List<Form16Document> docs = form16Repository
                .findByFinancialYearOrderByEmployee_FirstNameAsc(fy);

        List<Map<String, Object>> result = docs.stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",            d.getId());
            map.put("employeeId",    d.getEmployee().getEmployeeId());
            map.put("employeeName",  d.getEmployee().getFullName());
            map.put("email",         d.getEmployee().getEmailId());
            map.put("financialYear", d.getFinancialYear());
            map.put("fileName",      d.getOriginalFileName());
            map.put("fileSize",      formatFileSize(d.getFileSize()));
            map.put("uploadedBy",    d.getUploadedBy());
            map.put("uploadedAt",    d.getUploadedAt().toString());
            map.put("downloaded",    d.isDownloaded());
            map.put("downloadedAt",  d.getDownloadedAt() != null ? d.getDownloadedAt().toString() : "Not yet");
            return map;
        }).toList();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "fy",     fy,
            "total",  result.size(),
            "data",   result
        ));
    }

    // ── UPLOAD STATUS SUMMARY ─────────────────────────────────────────────────
    // GET /api/admin/form16/status?fy=2024-25
    @GetMapping("/status")
    public ResponseEntity<?> uploadStatus(
            @RequestParam(defaultValue = "2024-25") String fy) {

        long total      = form16Repository.countByFinancialYear(fy);
        long downloaded = form16Repository.countByFinancialYearAndDownloadedTrue(fy);

        return ResponseEntity.ok(Map.of(
            "status",           "success",
            "financialYear",    fy,
            "totalUploaded",    total,
            "totalDownloaded",  downloaded,
            "totalPending",     total - downloaded
        ));
    }

    // ── DELETE A FORM 16 RECORD ───────────────────────────────────────────────
    // DELETE /api/admin/form16/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Form16Document doc = form16Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found: " + id));

        // Delete file from disk
        java.io.File file = new java.io.File(doc.getFilePath());
        if (file.exists()) file.delete();

        form16Repository.delete(doc);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Form 16 record deleted"));
    }

    // HELPER
    private String formatFileSize(Long bytes) {
        if (bytes == null) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}