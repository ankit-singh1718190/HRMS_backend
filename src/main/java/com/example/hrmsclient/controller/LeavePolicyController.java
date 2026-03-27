package com.example.hrmsclient.controller;

import com.example.hrmsclient.entity.LeavePolicy;
import com.example.hrmsclient.service.LeavePolicyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/leave-policy")
@CrossOrigin
public class LeavePolicyController {

    private final LeavePolicyService service;

    public LeavePolicyController(LeavePolicyService service) {
        this.service = service;
    }

    @PostMapping
    public LeavePolicy create(@RequestBody LeavePolicy policy) {
        return service.create(policy);
    }

    @GetMapping
    public List<LeavePolicy> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public LeavePolicy getById(@PathVariable Long id) {
        return service.getById(id);
    }
    @PutMapping("/{id}")
    public LeavePolicy update(@PathVariable Long id,
                             @RequestBody LeavePolicy policy) {
        return service.update(id, policy);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Deleted successfully";
    }
}