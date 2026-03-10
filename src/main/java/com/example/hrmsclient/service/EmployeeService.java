package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeMapper employeeMapper;
    private final HrmsEmailService hrmsEmailService;
    private final AtomicLong idCounter = new AtomicLong(1000);

    public EmployeeService(EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder,
                           HrmsEmailService hrmsEmailService,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder    = passwordEncoder;
        this.hrmsEmailService   = hrmsEmailService;
        this.employeeMapper     = employeeMapper;
    }

    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO request) {

        if (employeeRepository.existsByEmailIdAndDeletedFalse(request.getEmailId())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmailId());
        }

        Employee employee = employeeMapper.toEntity(request);
        employee.setPassword(passwordEncoder.encode(request.getPassword()));

        Employee saved = employeeRepository.save(employee);

        // Generate a unique, human‑readable employeeId.
        // Base like EMP1001, EMP1002, ... and if it already exists, append -1, -2, ...
        String baseEmpId = "EMP" + (1000 + saved.getId());
        String candidateEmpId = baseEmpId;
        int suffix = 1;
        while (employeeRepository.existsByEmployeeId(candidateEmpId)) {
            candidateEmpId = baseEmpId + "-" + suffix++;
        }
        saved.setEmployeeId(candidateEmpId);

        saved = employeeRepository.save(saved);

        hrmsEmailService.sendWelcomeEmail(saved, request.getPassword());

        log.info("Created employee: {}", saved.getEmployeeId());

        return employeeMapper.toResponse(saved);
    }

    @Cacheable(value = "employees", key = "#id")
    public EmployeeResponseDTO getById(Long id) {
        Employee e = employeeRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        return employeeMapper.toResponse(e);
    }

    public PageResponseDTO<EmployeeResponseDTO> getAll(int page, int size, String sortBy, String dir) {
        Sort sort = dir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Page<Employee> result = employeeRepository.findAllByDeletedFalse(
            PageRequest.of(page, size, sort));
        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    public PageResponseDTO<EmployeeResponseDTO> search(String q, int page, int size) {
        Page<Employee> result = employeeRepository.searchEmployees(
            q, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    public PageResponseDTO<EmployeeResponseDTO> filterByDepartment(String dept, int page, int size) {
        Page<Employee> result = employeeRepository
            .findByDepartmentIgnoreCaseAndDeletedFalse(dept, PageRequest.of(page, size));
        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    public PageResponseDTO<EmployeeResponseDTO> filterByStatus(EmploymentStatus status, int page, int size) {
        Page<Employee> result = employeeRepository
            .findByEmploymentStatusAndDeletedFalse(status, PageRequest.of(page, size));
        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO request) {
        Employee existing = employeeRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        employeeMapper.updateEntity(existing, request);
        return employeeMapper.toResponse(employeeRepository.save(existing));
    }

    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public void deleteEmployee(Long id) {
        int rows = employeeRepository.softDeleteById(id, LocalDateTime.now());
        if (rows == 0) {
            throw new RuntimeException("Employee not found or already deleted");
        }
    }
    public List<String> getAllDepartments() {
        return employeeRepository.findAllDepartments();
    }

    public DashboardDTO getDashboardSummary() {
        DashboardDTO dto = new DashboardDTO();
        dto.setTotalEmployees(employeeRepository.countByDeletedFalse());
        dto.setActiveEmployees(employeeRepository
            .countByEmploymentStatusAndDeletedFalse(EmploymentStatus.ACTIVE));
        dto.setOnNoticePeriod(employeeRepository
            .countByEmploymentStatusAndDeletedFalse(EmploymentStatus.NOTICE_PERIOD));
        dto.setTerminated(employeeRepository
            .countByEmploymentStatusAndDeletedFalse(EmploymentStatus.TERMINATED));
        return dto;
    }
}