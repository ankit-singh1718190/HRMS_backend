package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIdAndDeletedFalse(String emailId);
    boolean existsByEmployeeId(String employeeId);   // For duplicate check on manual ID

    Optional<Employee> findByIdAndDeletedFalse(Long id);
    Optional<Employee> findByEmailIdAndDeletedFalse(String emailId);

    Page<Employee> findAllByDeletedFalse(Pageable pageable);
    Page<Employee> findByDeletedFalse(Pageable pageable);

    Page<Employee> findByDepartmentIgnoreCaseAndDeletedFalse(String dept, Pageable pageable);
    Page<Employee> findByRoleIgnoreCaseAndDeletedFalse(String role, Pageable pageable);
    Page<Employee> findByEmploymentStatusAndDeletedFalse(EmploymentStatus status, Pageable pageable);

    long countByDeletedFalse();
    long countByEmploymentStatusAndDeletedFalse(EmploymentStatus status);

    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.deleted = false ORDER BY e.department")
    List<String> findDistinctDepartments();

    @Query("""
        SELECT e FROM Employee e
        WHERE e.deleted = false
          AND (
               LOWER(e.firstName)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(e.lastName)   LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(e.emailId)    LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(e.department) LIKE LOWER(CONCAT('%', :q, '%'))
          )
    """)
    Page<Employee> searchEmployees(@Param("q") String q, Pageable pageable);

    List<Employee> findByReportingManagerAndDeletedFalse(String reportingManager);
    Optional<Employee> findByEmailId(String emailId);
    Optional<Employee> findByEmployeeIdAndDeletedFalse(String employeeId);
    Optional<Employee> findByEmployeeId(String employeeId);
}