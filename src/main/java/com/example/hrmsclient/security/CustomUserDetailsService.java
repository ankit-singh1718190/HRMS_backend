package com.example.hrmsclient.security;

import com.example.hrmsclient.entity.Admin;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.repository.AdminRepository;
import com.example.hrmsclient.repository.EmployeeRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository,
                                    AdminRepository adminRepository) {
        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String emailId) throws UsernameNotFoundException {

        // Check Admin first
        Admin admin = adminRepository.findByEmailId(emailId).orElse(null);

        if (admin != null) {
            String roleValue = admin.getRole() != null
                    ? admin.getRole().toUpperCase().replace(" ", "_")
                    : "ADMIN";
            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + roleValue);

            return User.builder()
                    .username(admin.getEmailId())
                    .password(admin.getPassword())
                    .authorities(List.of(authority))
                    .build();
        }

        // Check Employee
        Employee employee = employeeRepository.findByEmailId(emailId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + emailId));

        String roleValue = employee.getRole() != null
                ? employee.getRole().toUpperCase()
                : "EMPLOYEE";

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + roleValue);

        return User.builder()
                .username(employee.getEmailId())
                .password(employee.getPassword())
                .authorities(List.of(authority))
                .accountExpired(false)
                .accountLocked(employee.isDeleted())
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}