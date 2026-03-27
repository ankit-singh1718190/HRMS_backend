package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // ✅ IMPORTANT
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIdAndDeletedFalse(String emailId);
    boolean existsByEmployeeId(String employeeId);

    Optional<Employee> findByIdAndDeletedFalse(Long id);
    Optional<Employee> findByEmailIdAndDeletedFalse(String emailId);
    Optional<Employee> findByEmployeeIdAndDeletedFalse(String employeeId);
    Page<Employee> findAllByDeletedFalse(Pageable pageable);

    Page<Employee> findByDepartmentIgnoreCaseAndDeletedFalse(String dept, Pageable pageable);
    Page<Employee> findByRoleIgnoreCaseAndDeletedFalse(String role, Pageable pageable);
    Page<Employee> findByEmploymentStatusAndDeletedFalse(EmploymentStatus status, Pageable pageable);

    Page<Employee> findByManagerIdAndDeletedFalse(Long managerId, Pageable pageable);

    Page<Employee> findByManagerIdAndDepartmentIgnoreCaseAndDeletedFalse(
            Long managerId, String dept, Pageable pageable);

    Page<Employee> findByManagerIdAndEmploymentStatusAndDeletedFalse(
            Long managerId, EmploymentStatus status, Pageable pageable);


    long countByDeletedFalse();
    long countByEmploymentStatusAndDeletedFalse(EmploymentStatus status);

    long countByManagerIdAndDeletedFalse(Long managerId);

    long countByManagerIdAndEmploymentStatusAndDeletedFalse(
            Long managerId, EmploymentStatus status);
    long countByManagerAndDeletedFalse(Employee manager);

    long countByManagerAndEmploymentStatusAndDeletedFalse(
            Employee manager,
            EmploymentStatus status
    );

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
    Page<Employee> searchEmployees(@Param("q") String q, Pageable pageable); // ✅ FIXED
    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.deleted = false ORDER BY e.department")
    List<String> findDistinctDepartments();
    @Query("""
    	    SELECT e FROM Employee e
    	    WHERE e.deleted = false
    	      AND e.manager.id = :managerId
    	      AND (
    	           LOWER(e.firstName)  LIKE LOWER(CONCAT('%', :q, '%'))
    	        OR LOWER(e.lastName)   LIKE LOWER(CONCAT('%', :q, '%'))
    	        OR LOWER(e.emailId)    LIKE LOWER(CONCAT('%', :q, '%'))
    	        OR LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :q, '%'))
    	        OR LOWER(e.department) LIKE LOWER(CONCAT('%', :q, '%'))
    	      )
    	""")
    	Page<Employee> searchEmployeesByManager(
    	        @Param("managerId") Long managerId,
    	        @Param("q") String q,
    	        Pageable pageable);
}