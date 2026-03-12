package com.example.hrmsclient.controller;

import com.example.hrmsclient.entity.Form16Document;
import com.example.hrmsclient.repository.Form16DocumentRepository;
import com.example.hrmsclient.service.Form16UploadService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * EmployeeForm16Controller
 * ─────────────────────────────────────────────────────────────────────────────
 * Endpoints for EMPLOYEES to view and download their own Form 16.
 * Employee is identified from JWT — they can ONLY see their own Form 16.
 *
 * Secure in SecurityConfig:
 *   .requestMatchers("/api/employee/form16/**").hasAnyRole("EMPLOYEE", "ADMIN", "HR")
 *
 * Endpoints:
 *   GET /api/employee/form16/my-list              → See all Form 16s available for me
 *   GET /api/employee/form16/download?fy=2024-25  → Download my Form 16 PDF
 */
@RestController
@RequestMapping("/api/employee/form16")
public class EmployeeForm16Controller {

    private final Form16DocumentRepository form16Repository;
    private final Form16UploadService       uploadService;

    public EmployeeForm16Controller(Form16DocumentRepository form16Repository,
                                     Form16UploadService uploadService) {
        this.form16Repository = form16Repository;
        this.uploadService    = uploadService;
    }

    // ── LIST MY FORM 16s ─────────────────────────────────────────────────────
    /**
     * Employee sees all years for which Form 16 is available.
     * GET /api/employee/form16/my-list
     */
    @GetMapping("/my-list")
    public ResponseEntity<?> getMyForm16List(
            @AuthenticationPrincipal UserDetails userDetails) {

        // Identify employee from JWT email
        String email = userDetails.getUsername();

        List<Form16Document> docs = form16Repository.findByEmployeeEmail(email);

        if (docs.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "status",  "success",
                "message", "No Form 16 available yet. Please check back after June 15.",
                "data",    List.of()
            ));
        }

        List<Map<String, Object>> result = docs.stream().map(d -> Map.<String, Object>of(
            "id",            d.getId(),
            "financialYear", d.getFinancialYear(),
            "fileName",      d.getOriginalFileName(),
            "uploadedAt",    d.getUploadedAt().toString(),
            "downloaded",    d.isDownloaded(),
            "downloadedAt",  d.getDownloadedAt() != null ? d.getDownloadedAt().toString() : "Not downloaded yet"
        )).toList();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data",   result,
            "total",  result.size()
        ));
    }

    // ── DOWNLOAD MY FORM 16 ───────────────────────────────────────────────────
    /**
     * Employee downloads their Form 16 for a specific FY.
     * GET /api/employee/form16/download?fy=2024-25
     *
     * Security: Employee can only download their own Form 16.
     * The employeeId is resolved from JWT — never trusted from request param.
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadMyForm16(
            @RequestParam(defaultValue = "2024-25") String fy,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Resolve employee from JWT email — never accept employeeId from request
        String email = userDetails.getUsername();
        List<Form16Document> docs = form16Repository.findByEmployeeEmail(email);

        // Find the one for the requested FY
        Form16Document doc = docs.stream()
                .filter(d -> d.getFinancialYear().equals(fy))
                .findFirst()
                .orElse(null);

        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // This also marks the record as downloaded
            File file = uploadService.getForm16ForDownload(doc.getEmployee().getId(), fy);

            Resource resource     = new FileSystemResource(file);
            String downloadName   = doc.getEmployee().getEmployeeId()
                                    + "_Form16_FY" + fy + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}