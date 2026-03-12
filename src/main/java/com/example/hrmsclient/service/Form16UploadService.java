package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.Form16Document;
import com.example.hrmsclient.repository.EmployeeRepository;
import com.example.hrmsclient.repository.Form16DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Form16UploadService {

    private static final Logger log = LoggerFactory.getLogger(Form16UploadService.class);

    @Value("${app.upload.form16-dir:uploads/form16/}")
    private String form16Dir;

    private final Form16DocumentRepository form16Repository;
    private final EmployeeRepository        employeeRepository;
    private final EmailService              emailService;

    public Form16UploadService(Form16DocumentRepository form16Repository,
                                EmployeeRepository employeeRepository,
                                EmailService emailService) {
        this.form16Repository  = form16Repository;
        this.employeeRepository = employeeRepository;
        this.emailService       = emailService;
    }

    /**
     * Accepts a ZIP file containing Form 16 PDFs.
     * Each PDF inside must be named starting with the Employee ID:
     *    EMP-001_Form16_FY2024-25.pdf
     *    EMP-002_Form16_FY2024-25.pdf
     */
    @Transactional
    public BulkUploadResult bulkUploadFromZip(MultipartFile zipFile,
                                               String financialYear,
                                               String uploadedBy) throws IOException {
        ensureDir(financialYear);

        int success = 0, failed = 0, skipped = 0;
        List<String> errors  = new ArrayList<>();
        List<String> skips   = new ArrayList<>();
        List<String> success_list = new ArrayList<>();

        log.info("=== BULK FORM 16 UPLOAD STARTED — FY {} ===", financialYear);

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Skip directories and non-PDF files
                if (entry.isDirectory() || !entryName.toLowerCase().endsWith(".pdf")) {
                    zis.closeEntry();
                    continue;
                }

                // Strip path prefix if ZIP has folders inside
                String fileName = Paths.get(entryName).getFileName().toString();

                try {
                    // Extract Employee ID from filename (e.g. "EMP-001" from "EMP-001_Form16_FY2024-25.pdf")
                    String employeeId = extractEmployeeId(fileName);
                    if (employeeId == null) {
                        errors.add(fileName + " — could not extract Employee ID from filename");
                        failed++;
                        zis.closeEntry();
                        continue;
                    }

                    // Look up employee
                    Optional<Employee> empOpt = employeeRepository.findByEmployeeId(employeeId);
                    if (empOpt.isEmpty()) {
                        errors.add(fileName + " — Employee ID not found: " + employeeId);
                        failed++;
                        zis.closeEntry();
                        continue;
                    }

                    Employee emp = empOpt.get();

                    // Skip if already uploaded for this FY (optional: overwrite logic)
                    if (form16Repository.existsByEmployeeIdAndFinancialYear(emp.getId(), financialYear)) {
                        skips.add(fileName + " — already uploaded for FY " + financialYear + " (skipped)");
                        skipped++;
                        zis.closeEntry();
                        continue;
                    }

                    // Save PDF to disk
                    String savedPath = saveFileToDisk(zis, fileName, financialYear);

                    // Save metadata to DB
                    Form16Document doc = new Form16Document();
                    doc.setEmployee(emp);
                    doc.setFinancialYear(financialYear);
                    doc.setOriginalFileName(fileName);
                    doc.setFilePath(savedPath);
                    doc.setFileSize(new File(savedPath).length());
                    doc.setUploadedBy(uploadedBy);
                    doc.setUploadedAt(LocalDateTime.now());
                    form16Repository.save(doc);

                    // Notify employee via email (async)
                    try {
                        emailService.sendForm16AvailableNotification(emp, financialYear);
                    } catch (Exception e) {
                        log.warn("Email notification failed for {}: {}", emp.getEmailId(), e.getMessage());
                    }

                    success_list.add(emp.getEmployeeId() + " — " + emp.getFullName());
                    success++;
                    log.info("✅ Uploaded Form 16: {} → {}", fileName, emp.getFullName());

                } catch (Exception e) {
                    errors.add(fileName + " — " + e.getMessage());
                    failed++;
                    log.error("❌ Failed: {} — {}", fileName, e.getMessage());
                }

                zis.closeEntry();
            }
        }

        log.info("=== BULK UPLOAD COMPLETE: Success={}, Failed={}, Skipped={} ===",
                success, failed, skipped);

        return new BulkUploadResult(success, failed, skipped, financialYear,
                success_list, errors, skips);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SINGLE UPLOAD — Admin uploads one PDF for a specific employee
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Form16Document singleUpload(Long employeeId,
                                        String financialYear,
                                        MultipartFile pdfFile,
                                        String uploadedBy) throws IOException {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        ensureDir(financialYear);

        // Overwrite if exists
        form16Repository.findByEmployeeIdAndFinancialYear(emp.getId(), financialYear)
                .ifPresent(form16Repository::delete);

        String savedPath = saveSingleFile(pdfFile, emp.getEmployeeId(), financialYear);

        Form16Document doc = new Form16Document();
        doc.setEmployee(emp);
        doc.setFinancialYear(financialYear);
        doc.setOriginalFileName(pdfFile.getOriginalFilename());
        doc.setFilePath(savedPath);
        doc.setFileSize(pdfFile.getSize());
        doc.setUploadedBy(uploadedBy);
        doc.setUploadedAt(LocalDateTime.now());
        form16Repository.save(doc);

        // Email notification to employee
        try {
            emailService.sendForm16AvailableNotification(emp, financialYear);
        } catch (Exception e) {
            log.warn("Email notification failed for {}: {}", emp.getEmailId(), e.getMessage());
        }

        log.info("✅ Single Form 16 uploaded for {} — FY {}", emp.getEmployeeId(), financialYear);
        return doc;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. EMPLOYEE DOWNLOAD — Returns the File for streaming
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public File getForm16ForDownload(Long employeeId, String financialYear) {
        Form16Document doc = form16Repository
                .findByEmployeeIdAndFinancialYear(employeeId, financialYear)
                .orElseThrow(() -> new RuntimeException(
                        "Form 16 not available for FY " + financialYear +
                        ". Please contact HR."));

        File file = new File(doc.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("Form 16 file not found on server. Please contact HR.");
        }

        // Mark as downloaded
        if (!doc.isDownloaded()) {
            doc.setDownloaded(true);
            doc.setDownloadedAt(LocalDateTime.now());
            form16Repository.save(doc);
        }

        return file;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts Employee ID from filename.
     * Supports formats:
     *   EMP-001_Form16_FY2024-25.pdf  → EMP-001
     *   EMP001_Form16_FY2024-25.pdf   → EMP001
     */
    private String extractEmployeeId(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        // Take everything before the first underscore
        int idx = fileName.indexOf('_');
        if (idx <= 0) return null;
        return fileName.substring(0, idx).trim();
    }

    /** Save a file from ZipInputStream to disk */
    private String saveFileToDisk(ZipInputStream zis, String fileName,
                                   String fy) throws IOException {
        String dir  = form16Dir + fy + "/";
        Files.createDirectories(Paths.get(dir));
        String path = dir + fileName;

        try (FileOutputStream fos = new FileOutputStream(path)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
        return path;
    }

    /** Save a single MultipartFile to disk */
    private String saveSingleFile(MultipartFile file, String empId,
                                   String fy) throws IOException {
        String dir      = form16Dir + fy + "/";
        Files.createDirectories(Paths.get(dir));
        String fileName = empId + "_Form16_FY" + fy + ".pdf";
        String path     = dir + fileName;
        file.transferTo(new File(path));
        return path;
    }

    private void ensureDir(String fy) throws IOException {
        Files.createDirectories(Paths.get(form16Dir + fy + "/"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESULT RECORD
    // ─────────────────────────────────────────────────────────────────────────
    public record BulkUploadResult(
            int success,
            int failed,
            int skipped,
            String financialYear,
            List<String> successList,
            List<String> errorList,
            List<String> skipList
    ) {}
}