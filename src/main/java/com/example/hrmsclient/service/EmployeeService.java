package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.EmploymentStatus;
import com.example.hrmsclient.entity.LeaveStatus;
import com.example.hrmsclient.repository.AttendanceRepository;
import com.example.hrmsclient.repository.EmployeeRepository;
import com.example.hrmsclient.repository.LeaveRequestRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeMapper employeeMapper;
    private final HrmsEmailService hrmsEmailService;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            HrmsEmailService hrmsEmailService,
            EmployeeMapper employeeMapper,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository) {

        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.hrmsEmailService = hrmsEmailService;
        this.employeeMapper = employeeMapper;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /** Safely resolves role string — null user (Admin principal) defaults to "ADMIN" */
    private String roleOf(Employee user) {
        return (user != null && user.getRole() != null) ? user.getRole() : "ADMIN";
    }


    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO request) {

        if (employeeRepository.existsByEmailIdAndDeletedFalse(request.getEmailId())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmailId());
        }

        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }

        Employee employee = employeeMapper.toEntity(request);
        employee.setEmployeeId(request.getEmployeeId());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));

        Employee saved = employeeRepository.save(employee);
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

    public PageResponseDTO<EmployeeResponseDTO> getAll(
            int page, int size, String sortBy, String dir, Employee user) {

        Sort sort = dir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Employee> result;

        String role = roleOf(user);

        if ("ADMIN".equalsIgnoreCase(role) || "HR".equalsIgnoreCase(role)) {
            result = employeeRepository.findAllByDeletedFalse(pageable);
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            result = employeeRepository.findByManagerIdAndDeletedFalse(user.getId(), pageable);
        } else {
            result = employeeRepository.findById(user.getId())
                    .map(emp -> new PageImpl<>(List.of(emp), pageable, 1))
                    .orElse(new PageImpl<>(List.of(), pageable, 0));
        }

        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    public PageResponseDTO<EmployeeResponseDTO> search(
            String q, int page, int size, Employee user) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Employee> result;

        if ("MANAGER".equalsIgnoreCase(roleOf(user))) {
            result = employeeRepository.searchEmployeesByManager(user.getId(), q, pageable);
        } else {
            result = employeeRepository.searchEmployees(q, pageable);
        }

        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    // ── FILTER BY DEPARTMENT 

    public PageResponseDTO<EmployeeResponseDTO> filterByDepartment(
            String dept, int page, int size, Employee user) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Employee> result;

        if ("MANAGER".equalsIgnoreCase(roleOf(user))) {
            result = employeeRepository.findByManagerIdAndDepartmentIgnoreCaseAndDeletedFalse(
                    user.getId(), dept, pageable);
        } else {
            result = employeeRepository.findByDepartmentIgnoreCaseAndDeletedFalse(dept, pageable);
        }

        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    // ── FILTER BY STATUS

    public PageResponseDTO<EmployeeResponseDTO> filterByStatus(
            EmploymentStatus status, int page, int size, Employee user) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Employee> result;

        if ("MANAGER".equalsIgnoreCase(roleOf(user))) {
            result = employeeRepository.findByManagerIdAndEmploymentStatusAndDeletedFalse(
                    user.getId(), status, pageable);
        } else {
            result = employeeRepository.findByEmploymentStatusAndDeletedFalse(status, pageable);
        }

        return PageResponseDTO.from(result.map(employeeMapper::toResponse));
    }

    public List<EmployeeResponseDTO> getActiveEmployees(Employee user) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("firstName"));
        Page<Employee> result;

        if ("MANAGER".equalsIgnoreCase(roleOf(user))) {
            result = employeeRepository.findByManagerIdAndEmploymentStatusAndDeletedFalse(
                    user.getId(), EmploymentStatus.ACTIVE, pageable);
        } else {
            result = employeeRepository.findByEmploymentStatusAndDeletedFalse(
                    EmploymentStatus.ACTIVE, pageable);
        }

        return result.map(employeeMapper::toResponse).getContent();
    }

    public List<EmployeeResponseDTO> getExitEmployees(Employee user) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("exitDate").descending());
        Page<Employee> result;

        if ("MANAGER".equalsIgnoreCase(roleOf(user))) {
            result = employeeRepository.findByManagerIdAndEmploymentStatusAndDeletedFalse(
                    user.getId(), EmploymentStatus.EXITED, pageable);
        } else {
            result = employeeRepository.findByEmploymentStatusAndDeletedFalse(
                    EmploymentStatus.EXITED, pageable);
        }

        return result.map(employeeMapper::toResponse).getContent();
    }


    public List<EmployeeResponseDTO> getByRole(String role) {
        return employeeRepository
                .findByRoleIgnoreCaseAndDeletedFalse(
                        role,
                        PageRequest.of(0, Integer.MAX_VALUE, Sort.by("firstName")))
                .map(employeeMapper::toResponse)
                .getContent();
    }


    public List<String> getAllDepartments(Employee user) {
        if ("MANAGER".equalsIgnoreCase(roleOf(user))) {
            return employeeRepository
                    .findByManagerIdAndDeletedFalse(user.getId(), Pageable.unpaged())
                    .map(Employee::getDepartment)
                    .getContent()
                    .stream()
                    .distinct()
                    .toList();
        }

        return employeeRepository.findDistinctDepartments();
    }


    public DashboardDTO getDashboardSummary(Employee user) {

        boolean isManager = "MANAGER".equalsIgnoreCase(roleOf(user));

        long total;
        long active;

        if (isManager) {
            total = employeeRepository.countByManagerAndDeletedFalse(user);

            active = employeeRepository
                    .countByManagerAndEmploymentStatusAndDeletedFalse(
                            user, EmploymentStatus.ACTIVE);

        } else {

            total = employeeRepository.countByDeletedFalse();

            active = employeeRepository
                    .countByEmploymentStatusAndDeletedFalse(EmploymentStatus.ACTIVE);
        }

        long presentToday = isManager
                ? attendanceRepository.countByEmployeeManagerIdAndAttendanceDate(user.getId(), LocalDate.now())
                : attendanceRepository.countByAttendanceDate(LocalDate.now());

        long pendingLeaves = isManager
                ? leaveRequestRepository.countByEmployeeManagerIdAndStatus(user.getId(), LeaveStatus.PENDING)
                : leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        return new DashboardDTO(
                total,
                active,
                presentToday,
                pendingLeaves
        );
    }

    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO dto) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

        if (dto.getEmployeeId() != null
                && !dto.getEmployeeId().equals(employee.getEmployeeId())
                && employeeRepository.existsByEmployeeId(dto.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already exists: " + dto.getEmployeeId());
        }

        employeeMapper.updateEntity(employee, dto);
        Employee saved = employeeRepository.save(employee);
        log.info("Updated employee: {}", saved.getEmployeeId());

        return employeeMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public EmployeeResponseDTO exitEmployee(Long id, LocalDate exitDate) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

        employee.setExitDate(exitDate);
        employee.setLastWorkingDay(exitDate);
        employee.setEmploymentStatus(EmploymentStatus.EXITED);

        Employee saved = employeeRepository.save(employee);
        log.info("Employee {} exited on {}", saved.getEmployeeId(), exitDate);

        return employeeMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "employees", key = "#id")
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

        employee.softDelete();
        employeeRepository.save(employee);
        log.info("Soft-deleted employee: {}", employee.getEmployeeId());
    }
}