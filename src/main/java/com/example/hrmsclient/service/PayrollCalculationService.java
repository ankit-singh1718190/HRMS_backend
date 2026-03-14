package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.Payroll;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PayrollCalculationService {

    private static final BigDecimal PF_RATE            = new BigDecimal("0.12");
    private static final BigDecimal ESI_EMP_RATE       = new BigDecimal("0.0075");
    private static final BigDecimal ESI_EMPLOYER_RATE  = new BigDecimal("0.0325");
    private static final BigDecimal ESI_GROSS_LIMIT    = new BigDecimal("21000");
    private static final int        WORKING_DAYS_PM     = 26;  // per-day divisor

    public Payroll calculate(Employee employee, Payroll payroll) {

        BigDecimal basic = safe(payroll.getBasicSalary());

        if (basic.compareTo(BigDecimal.ZERO) == 0 && employee.getRole() != null) {
        }

        BigDecimal perDayRate = basic.compareTo(BigDecimal.ZERO) > 0
            ? basic.divide(BigDecimal.valueOf(WORKING_DAYS_PM), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        int weekendDays = payroll.getWeekendWorkDays() != null ? payroll.getWeekendWorkDays() : 0;
        BigDecimal weekendAmount = perDayRate.multiply(BigDecimal.valueOf(weekendDays))
                                             .setScale(2, RoundingMode.HALF_UP);
        payroll.setWeekendWorkAmount(weekendAmount);

        // ── Step 2: Gross Salary
        BigDecimal grossSalary = basic
            .add(safe(payroll.getHra()))
            .add(safe(payroll.getSpecialAllowance()))
            .add(safe(payroll.getArrears()))
            .add(safe(payroll.getPerfPay()))
            .add(weekendAmount)
            .add(safe(payroll.getReimbursement()))
            .add(safe(payroll.getFbp()))
            .setScale(2, RoundingMode.HALF_UP);
        payroll.setGrossSalary(grossSalary);

        BigDecimal pfBase     = basic.min(new BigDecimal("15000")); // PF ceiling ₹15,000
        BigDecimal pfEmployee = pfBase.multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pfEmployer = pfBase.multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP);
        payroll.setPfEmployee(pfEmployee);
        payroll.setPfEmployer(pfEmployer);

        BigDecimal esiEmployee = BigDecimal.ZERO;
        BigDecimal esiEmployer = BigDecimal.ZERO;
        if (grossSalary.compareTo(ESI_GROSS_LIMIT) <= 0) {
            esiEmployee = grossSalary.multiply(ESI_EMP_RATE).setScale(2, RoundingMode.HALF_UP);
            esiEmployer = grossSalary.multiply(ESI_EMPLOYER_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal professionalTax;
        if (grossSalary.compareTo(new BigDecimal("15000")) <= 0) {
            professionalTax = BigDecimal.ZERO;
        } else if (grossSalary.compareTo(new BigDecimal("20000")) <= 0) {
            professionalTax = new BigDecimal("150");
        } else {
            professionalTax = new BigDecimal("200");
        }
        payroll.setProfessionalTax(professionalTax);

        BigDecimal totalDeductions = pfEmployee
            .add(esiEmployee)
            .add(professionalTax)
            .add(safe(payroll.getTds()))
            .add(safe(payroll.getSalaryAdvance()))
            .add(safe(payroll.getOtherDeduction()))
            .setScale(2, RoundingMode.HALF_UP);
        payroll.setTotalDeductions(totalDeductions);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions)
                                          .setScale(2, RoundingMode.HALF_UP);
        payroll.setNetSalary(netSalary);

        return payroll;
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}