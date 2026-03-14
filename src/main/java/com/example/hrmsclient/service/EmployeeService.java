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

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository  employeeRepository;
    private final PasswordEncoder     passwordEncoder;
    private final EmployeeMapper      employeeMapper;
    private final HrmsEmailService    hrmsEmailService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder,
                           HrmsEmailService hrmsEmailService,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder    = passwordEncoder;
        this.hrmsEmailService   = hrmsEmailService;
        this.employeeMapper     = employeeMapper;
    }

    // ── CREATE 
    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO request) {

        // 1. Validate email uniqueness
        if (employeeRepository.existsByEmailIdAndDeletedFalse(request.getEmailId())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmailId());
        }

        // 2. CHANGE: Validate manual employeeId uniqueness
        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException(
                "Employee ID already exists: " + request.getEmployeeId()
                + ". Please use a unique ID.");
        }

        Employee employee = employeeMapper.toEntity(request);
        // 3. CHANGE: Use the manually supplied employeeId directly — no auto-gen
        employee.setEmployeeId(request.getEmployeeId());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));

        Employee saved = employeeRepository.save(employee);
        hrmsEmailService.sendWelcomeEmail(saved, request.getPassword());
        log.info("Created employee: {}", saved.getEmployeeId());
        return employeeMapper.toResponse(saved);
    }

    // ── READ
    @Cacheable(value = "employees", key = "#id")
    public EmployeeResponseDTO getById(Long id) {
        Employee e = employeeRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        return employeeMapper.toResponse(e);
    }

    public PageResponseDTO<EmployeeResponseDTO> getAll(int page, int size,
                                                        String sortBy, String dir) {
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

    public PageResponseDTO<EmployeeResponseDTO> filterByDepartment(String dept,
                                                                     int page, int size) {
        Page<Employee> result = employeeRepository
            .findByDepartmentIgnoreCaseAndDeletedFalse(dept, PageRequest.of(page, size));
        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    public PageResponseDTO<EmployeeResponseDTO> filterByStatus(EmploymentStatus status,
                                                                 int page, int size) {
        Page<Employee> result = employeeRepository
            .findByEmploymentStatusAndDeletedFalse(status, PageRequest.of(page, size));
        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    public List<EmployeeResponseDTO> getActiveEmployees() {
        return employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.ACTIVE,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by("firstName")))
            .map(employeeMapper::toResponse)
            .getContent();
    }

    public List<EmployeeResponseDTO> getExitEmployees() {
        return employeeRepository
            .findByEmploymentStatusAndDeletedFalse(
                EmploymentStatus.EXITED,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by("exitDate").descending()))
            .map(employeeMapper::toResponse)
            .getContent();
    }

    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO dto) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

        // If employeeId is being changed, check uniqueness
        if (dto.getEmployeeId() != null
                && !dto.getEmployeeId().equals(employee.getEmployeeId())
                && employeeRepository.existsByEmployeeId(dto.getEmployeeId())) {
            throw new IllegalArgumentException(
                "Employee ID already exists: " + dto.getEmployeeId());
        }

        employeeMapper.updateEntity(employee, dto);
        Employee saved = employeeRepository.save(employee);
        log.info("Updated employee: {}", saved.getEmployeeId());
        return employeeMapper.toResponse(saved);
    }

 
    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public EmployeeResponseDTO exitEmployee(Long id, java.time.LocalDate exitDate) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

        employee.setExitDate(exitDate);
        employee.setLastWorkingDay(exitDate);
        employee.setEmploymentStatus(EmploymentStatus.EXITED);
        Employee saved = employeeRepository.save(employee);
        log.info("Employee {} exited on {}", saved.getEmployeeId(), exitDate);
        return employeeMapper.toResponse(saved);
    }

    // ── DELETE (soft)
    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + id));
        employee.softDelete();
        employeeRepository.save(employee);
        log.info("Soft-deleted employee: {}", employee.getEmployeeId());
    }

    public List<String> getAllDepartments() {
        return employeeRepository.findDistinctDepartments();
    }

    public DashboardDTO getDashboardSummary() {
        long total  = employeeRepository.countByDeletedFalse();
        long active = employeeRepository.countByEmploymentStatusAndDeletedFalse(EmploymentStatus.ACTIVE);
        long notice = employeeRepository.countByEmploymentStatusAndDeletedFalse(EmploymentStatus.NOTICE_PERIOD);
        long exited = employeeRepository.countByEmploymentStatusAndDeletedFalse(EmploymentStatus.EXITED);
        return new DashboardDTO(total, active, notice, exited);
    }
}