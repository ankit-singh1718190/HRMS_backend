package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.LoginRequestDTO;
import com.example.hrmsclient.dto.LoginResponseDTO;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.repository.AdminRepository;
import com.example.hrmsclient.repository.EmployeeRepository;
import com.example.hrmsclient.security.JwtUtil;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtUtil jwtUtil,
                       EmployeeRepository employeeRepository,
                       AdminRepository adminRepository) {

        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmailId(),
                request.getPassword()
            )
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(request.getEmailId());

        String accessToken = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // check admin first
        return adminRepository.findByEmailId(request.getEmailId())
                .map(admin -> new LoginResponseDTO(
                        accessToken,
                        refreshToken,
                        admin.getEmailId(),
                        admin.getFirstName() + " " + admin.getLastName(),
                        admin.getRole(),
                        admin.getAdminId()
                ))
                .orElseGet(() -> {
                    Employee employee = employeeRepository.findByEmailId(request.getEmailId())
                            .orElseThrow(() -> new BadCredentialsException("User not found"));

                    return new LoginResponseDTO(
                            accessToken,
                            refreshToken,
                            employee.getEmailId(),
                            employee.getFullName(),
                            employee.getRole(),
                            employee.getEmployeeId(),
                            employee.getId()
                    );
                });
    }
}
