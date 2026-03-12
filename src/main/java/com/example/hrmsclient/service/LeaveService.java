package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.*;
import com.example.hrmsclient.entity.*;
import com.example.hrmsclient.repository.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class LeaveService {

    private final LeaveRequestRepository leaveRepo;
    private final EmployeeRepository     employeeRepo;
    private final HrmsEmailService       hrmsEmailService;
    private final LeaveBalanceService    leaveBalanceService;

    public LeaveService(LeaveRequestRepository leaveRepo,
                        EmployeeRepository employeeRepo,
                        HrmsEmailService hrmsEmailService,
                        LeaveBalanceService leaveBalanceService) {
        this.leaveRepo           = leaveRepo;
        this.employeeRepo        = employeeRepo;
        this.hrmsEmailService    = hrmsEmailService;
        this.leaveBalanceService = leaveBalanceService;
    }

    @Transactional
    public LeaveResponseDTO applyLeave(LeaveRequestDTO dto) {

        Employee employee = employeeRepo.findByIdAndDeletedFalse(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        if (leaveRepo.hasOverlappingLeave(
                dto.getEmployeeId(),
                dto.getStartDate(),
                dto.getEndDate())) {

            throw new IllegalArgumentException("Overlapping leave request exists");
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(dto.getLeaveType());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setReason(dto.getReason());
        leave.setStatus(LeaveStatus.PENDING);

        int totalDays = (int) (dto.getEndDate().toEpochDay() - dto.getStartDate().toEpochDay()) + 1;

        int leaveYear = dto.getStartDate().getYear();
        int leaveMonth = dto.getStartDate().getMonthValue();

        LeaveDeductionResultDTO result = leaveBalanceService.deductDays(
                dto.getEmployeeId(),
                dto.getLeaveType(),
                leaveYear,
                leaveMonth,
                totalDays
        );

        leave.setPaidDays(result.getPaidDays());
        leave.setUnpaidDays(result.getUnpaidDays());

        LeaveRequest saved = leaveRepo.save(leave);

        hrmsEmailService.sendLeaveAppliedEmail(saved);

        return toDto(saved);
    }

    // ─── Approve

    @Transactional
    public LeaveResponseDTO approveLeave(Long leaveId) {
        LeaveRequest leave = leaveRepo.findById(leaveId)
            .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new IllegalStateException("Leave is not in PENDING state");

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(
            SecurityContextHolder.getContext().getAuthentication().getName());
        leave.setApprovedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRepo.save(leave);
        hrmsEmailService.sendLeaveApprovedEmail(saved);
        return toDto(saved);
    }

    // ─── Reject

    @Transactional
    public LeaveResponseDTO rejectLeave(Long leaveId, String reason) {
        LeaveRequest leave = leaveRepo.findById(leaveId)
            .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new IllegalStateException("Only PENDING leaves can be rejected");

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectionReason(reason);
        leaveBalanceService.restoreDays(
            leave.getEmployee().getId(),
            leave.getLeaveType(),
            leave.getStartDate().getYear(),
            leave.getStartDate().getMonthValue(),
            leave.getPaidDays(),
            leave.getUnpaidDays()
        );
        leave.setPaidDays(0);
        leave.setUnpaidDays(0);

        LeaveRequest saved = leaveRepo.save(leave);
        hrmsEmailService.sendLeaveRejectedEmail(saved);
        return toDto(saved);
    }

    // ─── Cancel

    @Transactional
    public LeaveResponseDTO cancelLeave(Long leaveId) {
        LeaveRequest leave = leaveRepo.findById(leaveId)
            .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new IllegalStateException("Only PENDING leaves can be cancelled");

        leave.setStatus(LeaveStatus.CANCELLED);

        leaveBalanceService.restoreDays(
            leave.getEmployee().getId(),
            leave.getLeaveType(),
            leave.getStartDate().getYear(),
            leave.getStartDate().getMonthValue(),
            leave.getPaidDays(),
            leave.getUnpaidDays()
        );
        leave.setPaidDays(0);
        leave.setUnpaidDays(0);

        return toDto(leaveRepo.save(leave));
    }

    // ─── Queries

    public PageResponseDTO<LeaveResponseDTO> getLeavesByEmployee(
            Long empId, int page, int size) {
        Page<LeaveRequest> result = leaveRepo.findByEmployee_IdOrderByCreatedAtDesc(
            empId, PageRequest.of(page, size));
        return PageResponseDTO.from(result.map(this::toDto));
    }

    public PageResponseDTO<LeaveResponseDTO> getPendingLeaves(int page, int size) {
        Page<LeaveRequest> result = leaveRepo.findByStatusOrderByCreatedAtDesc(
            LeaveStatus.PENDING, PageRequest.of(page, size));
        return PageResponseDTO.from(result.map(this::toDto));
    }

    // ─── Mapper 

    private LeaveResponseDTO toDto(LeaveRequest l) {
        return new LeaveResponseDTO(
            l.getId(),
            l.getEmployee().getEmployeeId(),
            l.getEmployee().getFullName(),
            l.getLeaveType(),
            l.getStartDate(),
            l.getEndDate(),
            l.getLeaveDays(),
            l.getPaidDays(),
            l.getUnpaidDays(),
            l.getStatus(),
            l.getReason(),
            l.getRejectionReason(),
            l.getApprovedBy(),
            l.getApprovedAt(),
            l.getCreatedAt()
        );
    }
}