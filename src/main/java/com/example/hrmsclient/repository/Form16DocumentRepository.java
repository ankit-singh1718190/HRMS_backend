package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Form16Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Form16DocumentRepository extends JpaRepository<Form16Document, Long> {

    List<Form16Document> findByEmployeeIdOrderByFinancialYearDesc(Long employeeId);

    Optional<Form16Document> findByEmployeeIdAndFinancialYear(Long employeeId, String financialYear);

    List<Form16Document> findByFinancialYearOrderByEmployee_FirstNameAsc(String financialYear);

    boolean existsByEmployeeIdAndFinancialYear(Long employeeId, String financialYear);

    long countByFinancialYear(String financialYear);

    long countByFinancialYearAndDownloadedTrue(String financialYear);

    @Query("SELECT f FROM Form16Document f WHERE f.employee.emailId = :email ORDER BY f.financialYear DESC")
    List<Form16Document> findByEmployeeEmail(String email);
}