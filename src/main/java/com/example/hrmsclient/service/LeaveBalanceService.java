package com.example.hrmsclient.service;

import com.example.hrmsclient.dto.LeaveBalanceRequestDTO;
import com.example.hrmsclient.dto.LeaveBalanceResponseDTO;
import com.example.hrmsclient.dto.LeaveBalanceSummaryDTO;
import com.example.hrmsclient.dto.LeaveDeductionResultDTO;
import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.LeaveBalance;
import com.example.hrmsclient.repository.EmployeeRepository;
import com.example.hrmsclient.repository.LeaveBalanceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class LeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(LeaveBalanceService.class);

    private final LeaveBalanceRepository balanceRepo;
    private final EmployeeRepository employeeRepo;

    public LeaveBalanceService(LeaveBalanceRepository balanceRepo,
                               EmployeeRepository employeeRepo) {
        this.balanceRepo = balanceRepo;
        this.employeeRepo = employeeRepo;
    }

    @Transactional
    public LeaveBalanceResponseDTO allocate(LeaveBalanceRequestDTO dto) {

        validateAllocation(dto);

        Employee employee = employeeRepo.findByIdAndDeletedFalse(dto.getEmployeeId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found: " + dto.getEmployeeId()));

        LeaveBalance balance = findExistingRow(
                dto.getEmployeeId(),
                dto.getLeaveType(),
                dto.getYear(),
                dto.getMonth())
                .orElseGet(() -> createNewBalance(employee, dto));

        if (dto.getTotalAllocated() < balance.getUsedPaidDays()) {
            throw new IllegalArgumentException(
                    "Cannot allocate " + dto.getTotalAllocated()
                            + " leave days. Employee already used "
                            + balance.getUsedPaidDays());
        }

        balance.setTotalAllocated(dto.getTotalAllocated());

        if (dto.getNote() != null) {
            balance.setNote(dto.getNote());
        }

        LeaveBalance saved = balanceRepo.save(balance);

        log.info("Leave balance allocated → employee={} type={} year={} month={}",
                dto.getEmployeeId(), dto.getLeaveType(), dto.getYear(), dto.getMonth());

        return toDto(saved);
    }

    public LeaveBalanceSummaryDTO getSummary(Long empId, int year) {

        Employee employee = employeeRepo.findByIdAndDeletedFalse(empId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found: " + empId));

        List<LeaveBalanceResponseDTO> yearlyRows =
                balanceRepo.findYearlyRows(empId, year)
                        .stream()
                        .map(this::toDto)
                        .toList();

        List<LeaveBalanceResponseDTO> monthlyRows =
                balanceRepo.findMonthlyRows(empId, year)
                        .stream()
                        .map(this::toDto)
                        .toList();

        return new LeaveBalanceSummaryDTO(
                empId,
                employee.getFullName(),
                year,
                yearlyRows,
                monthlyRows
        );
    }

    public LeaveBalanceResponseDTO getBalance(Long empId,
                                              String leaveType,
                                              int year,
                                              Integer month) {

        LeaveBalance balance = findExistingRow(empId, leaveType, year, month)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Leave balance not found → employee=" + empId +
                                ", leaveType=" + leaveType +
                                ", year=" + year +
                                (month != null ? ", month=" + month : "")));

        return toDto(balance);
    }
    @Transactional
    public LeaveDeductionResultDTO deductDays(Long empId,
                                              String leaveType,
                                              int year,
                                              int leaveMonth,
                                              int days) {

        if (days <= 0) {
            throw new IllegalArgumentException("Days must be greater than zero");
        }

        Optional<LeaveBalance> monthlyRow =
                balanceRepo.findByEmployee_IdAndLeaveTypeAndYearAndMonth(
                        empId, leaveType, year, leaveMonth);

        Optional<LeaveBalance> yearlyRow =
                monthlyRow.isPresent()
                        ? Optional.empty()
                        : balanceRepo.findYearly(empId, leaveType, year);

        LeaveBalance balance = monthlyRow.orElse(yearlyRow.orElse(null));

        if (balance == null) {
            log.info("No leave allocation found → employee={} leaveType={}", empId, leaveType);
            return new LeaveDeductionResultDTO(0, days);
        }

        int unpaid = balance.consumeDays(days);
        int paid = days - unpaid;

        balanceRepo.save(balance);

        log.info("Leave deducted → employee={} paid={} unpaid={}", empId, paid, unpaid);

        return new LeaveDeductionResultDTO(paid, unpaid);
    }

    @Transactional
    public void restoreDays(Long empId,
                            String leaveType,
                            int year,
                            int leaveMonth,
                            int paidDays,
                            int unpaidDays) {

        if (paidDays == 0 && unpaidDays == 0) {
            return;
        }

        Optional<LeaveBalance> row =
                balanceRepo.findByEmployee_IdAndLeaveTypeAndYearAndMonth(
                        empId, leaveType, year, leaveMonth);

        if (row.isEmpty()) {
            row = balanceRepo.findYearly(empId, leaveType, year);
        }

        row.ifPresent(balance -> {
            balance.restoreDays(paidDays, unpaidDays);
            balanceRepo.save(balance);

            log.info("Leave restored → employee={} paid={} unpaid={}",
                    empId, paidDays, unpaidDays);
        });
    }
    private void validateAllocation(LeaveBalanceRequestDTO dto) {

        if (dto.getEmployeeId() == null) {
            throw new IllegalArgumentException("EmployeeId is required");
        }

        if (dto.getLeaveType() == null || dto.getLeaveType().isBlank()) {
            throw new IllegalArgumentException("LeaveType is required");
        }

        if (dto.getTotalAllocated() < 0) {
            throw new IllegalArgumentException("TotalAllocated cannot be negative");
        }
    }

    private LeaveBalance createNewBalance(Employee employee, LeaveBalanceRequestDTO dto) {

        if (dto.getMonth() == null) {
            return new LeaveBalance(employee, dto.getLeaveType(), dto.getYear(), 0);
        }

        return new LeaveBalance(
                employee,
                dto.getLeaveType(),
                dto.getYear(),
                dto.getMonth(),
                0
        );
    }

    private Optional<LeaveBalance> findExistingRow(Long empId,
                                                   String leaveType,
                                                   int year,
                                                   Integer month) {

        if (month == null) {
            return balanceRepo.findYearly(empId, leaveType, year);
        }

        return balanceRepo.findByEmployee_IdAndLeaveTypeAndYearAndMonth(
                empId,
                leaveType,
                year,
                month
        );
    }

    private LeaveBalanceResponseDTO toDto(LeaveBalance balance) {

        Employee emp = balance.getEmployee();

        return new LeaveBalanceResponseDTO(
                balance.getId(),
                emp != null ? emp.getId() : null,
                emp != null ? emp.getFullName() : null,
                balance.getLeaveType(),
                balance.getYear(),
                balance.getMonth(),
                balance.getTotalAllocated(),
                balance.getUsedPaidDays(),
                balance.getRemainingPaidDays(),
                balance.getUsedUnpaidDays(),
                balance.getNote()
        );
    }
}