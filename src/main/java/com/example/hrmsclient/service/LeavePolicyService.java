package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.EmployeeType;
import com.example.hrmsclient.entity.LeavePolicy;
import com.example.hrmsclient.repository.LeavePolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LeavePolicyService {

    private final LeavePolicyRepository repo;

    public LeavePolicyService(LeavePolicyRepository repo) {
        this.repo = repo;
    }

    //  CREATE
    public LeavePolicy create(LeavePolicy policy) {
        if (repo.existsByEmployeeTypeAndLeaveType(
                policy.getEmployeeType(),
                policy.getLeaveType())) {

            throw new IllegalArgumentException(
                "Policy already exists for "
                + policy.getEmployeeType() + " - " + policy.getLeaveType());
        }

        return repo.save(policy);
    }

    @Transactional(readOnly = true)
    public List<LeavePolicy> getAll() {
        return repo.findAll();
    }

    //  GET BY ID
    @Transactional(readOnly = true)
    public LeavePolicy getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
    }

    public LeavePolicy update(Long id, LeavePolicy updated) {
        LeavePolicy existing = getById(id);

//        existing.setEmployeeType(updated.getEmployeeType());
//        existing.setLeaveType(updated.getLeaveType());
        existing.setTotalDays(updated.getTotalDays());
        existing.setDescription(updated.getDescription());

        return repo.save(existing);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Policy not found");
        }
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public int getLeaveDays(EmployeeType type, String leaveType) {
        return repo.findByEmployeeTypeAndLeaveType(type, leaveType)
                .map(LeavePolicy::getTotalDays)
                .orElse(0);
    }
}