package com.example.hrmsclient.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.hrmsclient.entity.Employee;
import com.example.hrmsclient.entity.Payroll;

import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class EmailService {

	private static final Logger log = Logger.getLogger(EmailService.class.getName());

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final PayslipPdfService payslipPdfService;

	@Value("${app.mail.from}")
	private String fromEmail;

	@Value("${app.mail.from-name}")
	private String fromName;

	public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, PayslipPdfService payslipPdfService) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.payslipPdfService = payslipPdfService;
	}

	public void sendSimpleEmail(String to, String subject, String body) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
			log.info("Simple email sent to: " + to);
		} catch (Exception e) {
			log.severe("Failed to send simple email to " + to + ": " + e.getMessage());
			throw new RuntimeException("Email send failed", e);
		}
	}

	public void sendHtmlEmail(String to, String subject, String htmlContent) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromEmail, fromName);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			mailSender.send(message);
			log.info("HTML email sent to: " + to);
		} catch (Exception e) {
			log.severe("Failed to send HTML email to " + to + ": " + e.getMessage());
			throw new RuntimeException("Email send failed", e);
		}
	}

	@Async
	public void sendHtmlEmailAsync(String to, String subject, String htmlContent) {
		sendHtmlEmail(to, subject, htmlContent);
	}

	// Template-based Email
	public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> variables) {
		try {
			Context context = new Context();
			context.setVariables(variables);
			String html = templateEngine.process(templateName, context);
			sendHtmlEmail(to, subject, html);
		} catch (Exception e) {
			log.severe("Failed to process template " + templateName + ": " + e.getMessage());
			throw new RuntimeException("Template email failed", e);
		}
	}

	// Async Template-based Email
	@Async
	public void sendTemplatedEmailAsync(String to, String subject, String templateName, Map<String, Object> variables) {
		sendTemplatedEmail(to, subject, templateName, variables);
	}

	// Email with Attachment
	@Async
	public void sendEmailWithAttachment(String to, String subject, String htmlContent, File attachment) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromEmail, fromName);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			if (attachment != null && attachment.exists()) {
				helper.addAttachment(attachment.getName(), attachment);
			}
			mailSender.send(message);
			log.info("Email with attachment sent to: " + to);
		} catch (Exception e) {
			log.severe("Failed to send email with attachment to " + to + ": " + e.getMessage());
			throw new RuntimeException("Email send failed", e);
		}
	}

	// Bulk Email (BCC)
	@Async
	public void sendBulkEmail(String[] recipients, String subject, String htmlContent) {
		if (recipients == null || recipients.length == 0) {
			log.warning("No recipients provided for bulk email");
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromEmail, fromName);
			helper.setBcc(recipients);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			mailSender.send(message);
			log.info("Bulk email sent to " + recipients.length + " recipients");
		} catch (Exception e) {
			log.severe("Failed to send bulk email: " + e.getMessage());
			throw new RuntimeException("Bulk email send failed", e);
		}
	}

	// 8. PAYSLIP EMAIL
	@Async
	public void sendPayslipEmail(Payroll payroll) {

	    String to    = payroll.getEmployee().getEmailId();
	    String name  = payroll.getEmployee().getFullName();
	    String month = payroll.getPayrollMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
	    String subject = "Salary Credited - Payslip for " + month;

	    try {
	        // Step 1: Build variables for Thymeleaf template
	        Map<String, Object> vars = Map.ofEntries(
	            Map.entry("employeeName",     name),
	            Map.entry("employeeId",       payroll.getEmployee().getEmployeeId()),
	            Map.entry("month",            month),
	            Map.entry("basicSalary",      fmt(payroll.getBasicSalary())),
	            Map.entry("hra",              fmt(payroll.getHra())),
	            Map.entry("da",               fmt(payroll.getDa())),
	            Map.entry("specialAllowance", fmt(payroll.getSpecialAllowance())),
	            Map.entry("arrears",          fmt(payroll.getArrears())),
	            Map.entry("perfPay",          fmt(payroll.getPerfPay())),
	            Map.entry("weekendWorkDays",  payroll.getWeekendWorkDays()),
	            Map.entry("weekendWorkAmount",fmt(payroll.getWeekendWorkAmount())),
	            Map.entry("reimbursement",    fmt(payroll.getReimbursement())),
	            Map.entry("fbp",              fmt(payroll.getFbp())),
	            Map.entry("bonusAmount",      fmt(payroll.getBonusAmount())),
	            Map.entry("grossSalary",      fmt(payroll.getGrossSalary())),
	            Map.entry("pfEmployee",       fmt(payroll.getPfEmployee())),
	            Map.entry("professionalTax",  fmt(payroll.getProfessionalTax())),
	            Map.entry("tds",              fmt(payroll.getTds())),
	            Map.entry("salaryAdvance",    fmt(payroll.getSalaryAdvance())),   // was loanDeduction
	            Map.entry("otherDeduction",   fmt(payroll.getOtherDeduction())),
	            Map.entry("totalDeductions",  fmt(payroll.getTotalDeductions())),
	            Map.entry("netSalary",        fmt(payroll.getNetSalary())),
	            Map.entry("workingDays",      payroll.getWorkingDays()),
	            Map.entry("presentDays",      payroll.getPresentDays()),
	            Map.entry("leaveDays",        payroll.getLeaveDays()),
	            Map.entry("absentDays",       payroll.getAbsentDays()),
	            Map.entry("paymentDate",      payroll.getPaymentDate() != null
	                                            ? payroll.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
	                                            : "N/A"),
	            Map.entry("paymentRef",       payroll.getPaymentReference() != null
	                                            ? payroll.getPaymentReference() : "N/A"),
	            Map.entry("paymentMode",      payroll.getPaymentMode() != null
	                                            ? payroll.getPaymentMode() : "NEFT")
	        );

	        // Step 2: Render Thymeleaf template → HTML
	        Context context = new Context();
	        context.setVariables(vars);
	        String htmlContent = templateEngine.process("email/payslip-email", context);

	        // Step 3: Generate PDF payslip
	        File pdfFile = payslipPdfService.generatePayslip(payroll);

	        // Step 4: Send HTML email + PDF attached
	        MimeMessage message = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
	        helper.setFrom(fromEmail, fromName);
	        helper.setTo(to);
	        helper.setSubject(subject);
	        helper.setText(htmlContent, true);
	        helper.addAttachment(pdfFile.getName(), pdfFile);

	        mailSender.send(message);
	        log.info("Payslip email sent to: " + to + " | Month: " + month);

	    } catch (Exception e) {
	    	log.severe("Payslip email failed for " + to + " | Month: " + month + " | Reason: " + e.getMessage());
	    }
	}

	// HELPER
	private String fmt(BigDecimal val) {
		if (val == null)
			return "0.00";
		return String.format("%,.2f", val);
	}

	@Async
	public void sendForm16AvailableNotification(Employee employee, String financialYear) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

			helper.setFrom(fromEmail, fromName);
			helper.setTo(employee.getEmailId());
			helper.setSubject("Your Form 16 for FY " + financialYear + " is now available");

			String body = "Dear " + employee.getFirstName() + ",\n\n" + "Your Form 16 for Financial Year "
					+ financialYear + " has been uploaded by HR\n" + "and is now available for download.\n\n"
					+ "HOW TO DOWNLOAD:\n" + "1. Login to your Employee Dashboard\n"
					+ "2. Go to: My Documents > Form 16\n" + "3. Click Download next to FY " + financialYear + "\n\n"
					+ "You will need Form 16 to file your Income Tax Return (ITR).\n\n"
					+ "For any queries, please contact HR at " + fromEmail + ".\n\n" + "Regards,\n" + "HR Team\n"
					+ fromName;

			helper.setText(body, false);
			mailSender.send(message);

			log.info("Form 16 availability notification sent to " + employee.getEmailId());

		} catch (Exception e) {
			log.severe("Failed to send Form 16 notification to " + employee.getEmailId() + ": " + e.getMessage());
		}
	}

}