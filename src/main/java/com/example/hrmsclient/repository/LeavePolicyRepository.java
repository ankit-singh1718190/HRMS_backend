package com.example.hrmsclient.repository;

import com.example.hrmsclient.entity.EmployeeType;
import com.example.hrmsclient.entity.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {

    Optional<LeavePolicy> findByEmployeeTypeAndLeaveType(
            EmployeeType employeeType,
            String leaveType
    );

    boolean existsByEmployeeTypeAndLeaveType(
            EmployeeType employeeType,
            String leaveType
    );
}