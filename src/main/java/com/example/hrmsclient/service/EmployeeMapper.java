package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.EmployeeRequestDTO;
import com.example.hrmsclient.dto.EmployeeResponseDTO;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.repository.EmployeeRepository;
import org.springframework.stereotype.Component;


@Component
public class EmployeeMapper {

    private final EmployeeRepository employeeRepository;

    public EmployeeMapper(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee toEntity(EmployeeRequestDTO dto) {
        Employee e = new Employee();
        e.setEmployeeId(dto.getEmployeeId());
        e.setPrefix(dto.getPrefix());
        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName());
        e.setEmailId(dto.getEmailId());
        e.setContactNumber1(dto.getContactNumber1());
        e.setGender(dto.getGender());
        e.setDateOfBirth(dto.getDateOfBirth());
        e.setNationality(dto.getNationality());
        e.setWorkEmail(dto.getWorkEmail());
        e.setJoiningDate(dto.getJoiningDate());
        e.setExitDate(dto.getExitDate());
        e.setHouseNo(dto.getHouseNo());
        e.setCity(dto.getCity());
        e.setState(dto.getState());
        e.setPanNumber(dto.getPanNumber());
        e.setAadharNumber(dto.getAadharNumber());
        e.setPassportNumber(dto.getPassportNumber());
        e.setFatherName(dto.getFatherName());
        e.setMotherName(dto.getMotherName());
        e.setMaritalStatus(dto.getMaritalStatus());
        e.setPreviousCompanyName(dto.getPreviousCompanyName());
        e.setPreviousExperience(dto.getPreviousExperience());
        e.setDepartment(dto.getDepartment());
        e.setDesignation(dto.getDesignation());
        e.setPreviousCtc(dto.getPreviousCtc());
        e.setHigherQualification(dto.getHigherQualification());
        e.setRole(dto.getRole());
        e.setEmployeeType(dto.getEmployeeType());
        e.setBankName(dto.getBankName());
        e.setAccountNo(dto.getAccountNo());
        e.setIfscCode(dto.getIfscCode());
        e.setBankBranch(dto.getBankBranch());

        //  Resolve reportingManager employeeId string → actual Employee FK
        resolveManager(dto.getReportingManager(), e);

        // password is set separately (encoded) in service
        return e;
    }

    public void updateEntity(Employee e, EmployeeRequestDTO dto) {
        if (dto.getEmployeeId()          != null) e.setEmployeeId(dto.getEmployeeId());
        if (dto.getPrefix()              != null) e.setPrefix(dto.getPrefix());
        if (dto.getFirstName()           != null) e.setFirstName(dto.getFirstName());
        if (dto.getLastName()            != null) e.setLastName(dto.getLastName());
        if (dto.getEmailId()             != null) e.setEmailId(dto.getEmailId());
        if (dto.getContactNumber1()      != null) e.setContactNumber1(dto.getContactNumber1());
        if (dto.getGender()              != null) e.setGender(dto.getGender());
        if (dto.getDateOfBirth()         != null) e.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getNationality()         != null) e.setNationality(dto.getNationality());
        if (dto.getWorkEmail()           != null) e.setWorkEmail(dto.getWorkEmail());
        if (dto.getJoiningDate()         != null) e.setJoiningDate(dto.getJoiningDate());
        if (dto.getExitDate()            != null) e.setExitDate(dto.getExitDate());
        if (dto.getHouseNo()             != null) e.setHouseNo(dto.getHouseNo());
        if (dto.getCity()                != null) e.setCity(dto.getCity());
        if (dto.getState()               != null) e.setState(dto.getState());
        if (dto.getPanNumber()           != null) e.setPanNumber(dto.getPanNumber());
        if (dto.getAadharNumber()        != null) e.setAadharNumber(dto.getAadharNumber());
        if (dto.getPassportNumber()      != null) e.setPassportNumber(dto.getPassportNumber());
        if (dto.getFatherName()          != null) e.setFatherName(dto.getFatherName());
        if (dto.getMotherName()          != null) e.setMotherName(dto.getMotherName());
        if (dto.getMaritalStatus()       != null) e.setMaritalStatus(dto.getMaritalStatus());
        if (dto.getDepartment()          != null) e.setDepartment(dto.getDepartment());
        if (dto.getDesignation()         != null) e.setDesignation(dto.getDesignation());
        if (dto.getRole()                != null) e.setRole(dto.getRole());
        if (dto.getEmployeeType()        != null) e.setEmployeeType(dto.getEmployeeType());
        if (dto.getBankName()            != null) e.setBankName(dto.getBankName());
        if (dto.getAccountNo()           != null) e.setAccountNo(dto.getAccountNo());
        if (dto.getIfscCode()            != null) e.setIfscCode(dto.getIfscCode());
        if (dto.getBankBranch()          != null) e.setBankBranch(dto.getBankBranch());

        //  Always re-resolve manager — handles both setting and clearing it
        resolveManager(dto.getReportingManager(), e);
    }

    public EmployeeResponseDTO toResponse(Employee e) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setId(e.getId());
        dto.setEmployeeId(e.getEmployeeId());
        dto.setFullName(e.getFullName());
        dto.setFirstName(e.getFirstName());
        dto.setLastName(e.getLastName());
        dto.setEmailId(e.getEmailId());
        dto.setContactNumber1(e.getContactNumber1());
        dto.setGender(e.getGender());
        dto.setDateOfBirth(e.getDateOfBirth());
        dto.setWorkEmail(e.getWorkEmail());
        dto.setJoiningDate(e.getJoiningDate());
        dto.setExitDate(e.getExitDate());
        dto.setDepartment(e.getDepartment());
        dto.setDesignation(e.getDesignation());
        dto.setRole(e.getRole());
        dto.setEmployeeType(e.getEmployeeType() != null
                ? e.getEmployeeType().name() : null);
        dto.setReportingManager(e.getManager() != null
                ? e.getManager().getEmployeeId() : null);
        dto.setEmploymentStatus(e.getEmploymentStatus() != null
                ? e.getEmploymentStatus().name() : null);
        dto.setCity(e.getCity());
        dto.setState(e.getState());
        dto.setBankName(e.getBankName());
        dto.setProfilePhotoUrl(e.getProfilePhotoUrl());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }


    private void resolveManager(String reportingManagerId, Employee target) {
        if (reportingManagerId == null || reportingManagerId.isBlank()) {
            target.setManager(null);
            return;
        }
        Employee manager = employeeRepository
                .findByEmployeeIdAndDeletedFalse(reportingManagerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reporting manager not found: " + reportingManagerId));
        target.setManager(manager);
    }
}