package com.example.hrmsclient.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Duplicate Entry 
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEntry(
            DataIntegrityViolationException ex) {

        String message = "Duplicate entry. Please check your data.";
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "";

        //  Detect which field is duplicate and return clean message
        if (rootMsg.contains("email_id") || rootMsg.contains("emailId")) {
            message = "Email address already exists. Please use a different email.";
        } else if (rootMsg.contains("work_email") || rootMsg.contains("workEmail")) {
            message = "Work email already exists. Please use a different work email.";
        } else if (rootMsg.contains("employee_id") || rootMsg.contains("employeeId")) {
            message = "Employee ID already exists.";
        } else if (rootMsg.contains("pan_number") || rootMsg.contains("panNumber")) {
            message = "PAN number already registered.";
        } else if (rootMsg.contains("aadhar_number") || rootMsg.contains("aadharNumber")) {
            message = "Aadhar number already registered.";
        } else if (rootMsg.contains("account_no") || rootMsg.contains("accountNo")) {
            message = "Bank account number already registered.";
        } else if (rootMsg.contains("contact_number") || rootMsg.contains("contactNumber")) {
            message = "Contact number already registered.";
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT) 
                .body(Map.of(
                    "status", "error",
                    "message", message
                ));
    }

    // ── Validation Errors (@Valid) ────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  
                .body(Map.of(
                    "status", "error",
                    "message", "Validation failed",
                    "errors", fieldErrors
                ));
    }

    // ── Access denied (e.g. employee viewing another's leaves) → 403 ─────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "status", "error",
                    "message", ex.getMessage() != null ? ex.getMessage() : "Access Denied"
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(
            Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
                .body(Map.of(
                    "status", "error",
                    "message", "Something went wrong. Please try again."
                ));
    }
}