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

        // ── Admin: still uses a generic wrapper (Admin does not implement UserDetails)
        Admin admin = adminRepository.findByEmailId(emailId).orElse(null);

        if (admin != null) {
            String roleValue = admin.getRole() != null
                    ? admin.getRole().toUpperCase().replace(" ", "_")
                    : "ADMIN";

            return User.builder()
                    .username(admin.getEmailId())
                    .password(admin.getPassword())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + roleValue)))
                    .build();
        }

      
        return employeeRepository
                .findByEmailIdAndDeletedFalse(emailId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + emailId));
    }
}