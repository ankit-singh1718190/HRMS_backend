package com.example.hrmsclient.service;

import com.example.hrmsclient.entity.Payroll;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PayslipPdfService {

    @Value("${app.upload.payslip-dir}")
    private String payslipDir;

    @Value("${app.mail.from-name}")
    private String companyName;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final BaseColor PRIMARY    = new BaseColor(37, 99, 235);
    private static final BaseColor LIGHT_BLUE = new BaseColor(219, 234, 254);
    private static final BaseColor DARK_TEXT  = new BaseColor(17, 24, 39);
    private static final BaseColor GREY_TEXT  = new BaseColor(107, 114, 128);
    private static final BaseColor GREEN      = new BaseColor(22, 163, 74);
    private static final BaseColor RED        = new BaseColor(220, 38, 38);
    private static final BaseColor ROW_ALT    = new BaseColor(248, 250, 252);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE   = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,   BaseColor.WHITE);
    private static final Font FONT_SUB     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.WHITE);
    private static final Font FONT_SECTION = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   new BaseColor(37, 99, 235));
    private static final Font FONT_LABEL   = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   new BaseColor(17, 24, 39));
    private static final Font FONT_VALUE   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, new BaseColor(17, 24, 39));
    private static final Font FONT_AMOUNT  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   new BaseColor(17, 24, 39));
    private static final Font FONT_GREEN   = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   new BaseColor(22, 163, 74));
    private static final Font FONT_RED     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   new BaseColor(220, 38, 38));
    private static final Font FONT_NET     = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   BaseColor.WHITE);
    private static final Font FONT_GREY    = new Font(Font.FontFamily.HELVETICA, 8,  Font.ITALIC, new BaseColor(107, 114, 128));

    public File generatePayslip(Payroll payroll) throws Exception {

        // Create directory if not exists
        File dir = new File(payslipDir);
        if (!dir.exists()) dir.mkdirs();

        // File name: uploads/payslips/EMP001_2024-03_payslip.pdf
        String month    = payroll.getPayrollMonth().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String empId    = payroll.getEmployee().getEmployeeId();
        String filePath = payslipDir + empId + "_" + month + "_payslip.pdf";
        File   pdfFile  = new File(filePath);

        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        document.open();

        addHeader(document, payroll);
        addEmployeeInfo(document, payroll);
        addAttendanceSummary(document, payroll);
        addEarningsDeductionsTable(document, payroll);
        addNetSalaryBanner(document, payroll);
        addPaymentInfo(document, payroll);
        addFooter(document);

        document.close();
        return pdfFile;
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private void addHeader(Document doc, Payroll p) throws Exception {
        String monthYear = p.getPayrollMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60f, 40f});

        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(PRIMARY);
        left.setPadding(20);
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Phrase(companyName.toUpperCase(), FONT_TITLE));
        left.addElement(new Phrase("Human Resource Management System", FONT_SUB));
        header.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(PRIMARY);
        right.setPadding(20);
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Font bigWhite = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.WHITE);
        right.addElement(new Phrase("PAYSLIP", bigWhite));
        right.addElement(new Phrase(monthYear, FONT_SUB));
        header.addCell(right);

        doc.add(header);
        doc.add(Chunk.NEWLINE);
    }

    // ── EMPLOYEE INFO ─────────────────────────────────────────────────────────
    private void addEmployeeInfo(Document doc, Payroll p) throws Exception {
        addSectionTitle(doc, "Employee Information");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25f, 25f, 25f, 25f});
        table.setSpacingBefore(5f);

        addInfoRow(table,
            "Employee ID",   p.getEmployee().getEmployeeId(),
            "Name",          p.getEmployee().getFullName());
        addInfoRow(table,
            "Department",    nvl(p.getEmployee().getDepartment()),
            "Designation",   nvl(p.getEmployee().getDesignation()));
        addInfoRow(table,
            "Email",         p.getEmployee().getEmailId(),
            "Bank Account",  maskAccount(p.getBankAccount()));
        addInfoRow(table,
            "IFSC Code",     nvl(p.getIfscCode()),
            "Pay Period",    p.getPayrollMonth()
                                .format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ── ATTENDANCE ────────────────────────────────────────────────────────────
    private void addAttendanceSummary(Document doc, Payroll p) throws Exception {
        addSectionTitle(doc, "Attendance Summary");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);

        addInfoRow(table,
            "Working Days",  String.valueOf(p.getWorkingDays()),
            "Present Days",  String.valueOf(p.getPresentDays()));
        addInfoRow(table,
            "Leave Days",    String.valueOf(p.getLeaveDays()),
            "Absent (LOP)",  String.valueOf(p.getAbsentDays()));

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ── EARNINGS & DEDUCTIONS ─────────────────────────────────────────────────
    private void addEarningsDeductionsTable(Document doc, Payroll p) throws Exception {
        addSectionTitle(doc, "Earnings & Deductions");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35f, 15f, 35f, 15f});
        table.setSpacingBefore(5f);

        addTableHeader(table, "Earnings", "Amount (₹)", "Deductions", "Amount (₹)");
        String[][] rows = {
        	    { "Basic Salary",         fmt(p.getBasicSalary()),
        	      "PF Employee (12%)",    fmt(p.getPfEmployee()) },
        	    { "HRA (40%)",            fmt(p.getHra()),
        	      "Salary Advance",       fmt(p.getSalaryAdvance()) },   
        	    { "DA (10%)",             fmt(p.getDa()),
        	      "Professional Tax",     fmt(p.getProfessionalTax()) },
        	    { "Special Allowance",    fmt(p.getSpecialAllowance()),
        	      "TDS (Income Tax)",     fmt(p.getTds()) },
        	    { "Weekend Work",         fmt(p.getWeekendWorkAmount()),  
        	      "Other Deduction",      fmt(p.getOtherDeduction()) },
        	    { "Bonus",                fmt(p.getBonusAmount()),
        	      "Arrears",              fmt(p.getArrears()) },
        	    { "Reimbursement",        fmt(p.getReimbursement()),
        	      "FBP",                  fmt(p.getFbp()) }
        	};

        boolean alt = false;
        for (String[] row : rows) {
            BaseColor bg = alt ? ROW_ALT : BaseColor.WHITE;
            table.addCell(styledCell(row[0], FONT_VALUE,  bg, Element.ALIGN_LEFT));
            table.addCell(styledCell(row[1], FONT_AMOUNT, bg, Element.ALIGN_RIGHT));
            table.addCell(styledCell(row[2], FONT_VALUE,  bg, Element.ALIGN_LEFT));
            table.addCell(styledCell(row[3].isEmpty() ? "-" : row[3],
                                      FONT_AMOUNT, bg, Element.ALIGN_RIGHT));
            alt = !alt;
        }

        // Totals row
        table.addCell(styledCell("GROSS SALARY",      FONT_LABEL, LIGHT_BLUE, Element.ALIGN_LEFT));
        table.addCell(styledCell("₹ " + fmt(p.getGrossSalary()), FONT_GREEN, LIGHT_BLUE, Element.ALIGN_RIGHT));
        table.addCell(styledCell("TOTAL DEDUCTIONS",  FONT_LABEL, LIGHT_BLUE, Element.ALIGN_LEFT));
        table.addCell(styledCell("₹ " + fmt(p.getTotalDeductions()), FONT_RED, LIGHT_BLUE, Element.ALIGN_RIGHT));

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ── NET SALARY BANNER ─────────────────────────────────────────────────────
    private void addNetSalaryBanner(Document doc, Payroll p) throws Exception {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(5f);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(GREEN);
        cell.setPadding(15);
        cell.setBorder(Rectangle.NO_BORDER);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.WHITE);
        Paragraph para = new Paragraph();
        para.setAlignment(Element.ALIGN_CENTER);
        para.add(new Phrase("NET SALARY CREDITED  |  ", labelFont));
        para.add(new Phrase("₹ " + fmt(p.getNetSalary()), FONT_NET));
        cell.addElement(para);
        banner.addCell(cell);

        doc.add(banner);
        doc.add(Chunk.NEWLINE);
    }

    // ── PAYMENT INFO ──────────────────────────────────────────────────────────
    private void addPaymentInfo(Document doc, Payroll p) throws Exception {
        addSectionTitle(doc, "Payment Details");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);

        String payDate = p.getPaymentDate() != null
                ? p.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "N/A";

        addInfoRow(table,
            "Payment Date",      payDate,
            "Transaction Ref",   nvl(p.getPaymentReference()));
        addInfoRow(table,
            "Payment Mode",      nvl(p.getPaymentMode()),
            "Status",            "PAID ✓");

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────
    private void addFooter(Document doc) throws Exception {
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(15f);

        PdfPCell cell = new PdfPCell();
        cell.setBorderWidthTop(0.5f);
        cell.setBorderColorTop(GREY_TEXT);
        cell.setBorderWidthBottom(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setPaddingTop(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.addElement(new Phrase(
            "This is a system-generated payslip and does not require a signature. " +
            "For queries contact hr@yourcompany.com", FONT_GREY));
        footer.addCell(cell);

        doc.add(footer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void addSectionTitle(Document doc, String title) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(title, FONT_SECTION));
        c.setBorderWidthBottom(1.5f);
        c.setBorderColorBottom(PRIMARY);
        c.setBorderWidthTop(0);
        c.setBorderWidthLeft(0);
        c.setBorderWidthRight(0);
        c.setPaddingBottom(4);
        t.addCell(c);
        doc.add(t);
    }

    private void addTableHeader(PdfPTable table,
                                 String e1, String e2,
                                 String d1, String d2) {
        table.addCell(styledCell(e1, FONT_LABEL, LIGHT_BLUE, Element.ALIGN_LEFT));
        table.addCell(styledCell(e2, FONT_LABEL, LIGHT_BLUE, Element.ALIGN_RIGHT));
        table.addCell(styledCell(d1, FONT_LABEL, LIGHT_BLUE, Element.ALIGN_LEFT));
        table.addCell(styledCell(d2, FONT_LABEL, LIGHT_BLUE, Element.ALIGN_RIGHT));
    }

    private void addInfoRow(PdfPTable table,
                             String l1, String v1,
                             String l2, String v2) {
        table.addCell(styledCell(l1, FONT_LABEL, ROW_ALT,        Element.ALIGN_LEFT));
        table.addCell(styledCell(v1, FONT_VALUE, BaseColor.WHITE, Element.ALIGN_LEFT));
        table.addCell(styledCell(l2, FONT_LABEL, ROW_ALT,        Element.ALIGN_LEFT));
        table.addCell(styledCell(v2, FONT_VALUE, BaseColor.WHITE, Element.ALIGN_LEFT));
    }

    private PdfPCell styledCell(String text, Font font, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorderColor(new BaseColor(229, 231, 235));
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private String fmt(java.math.BigDecimal val) {
        if (val == null) return "0.00";
        return String.format("%,.2f", val);
    }

    private String nvl(String val) {
        return val != null ? val : "N/A";
    }

    private String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 4) return "****";
        return "****" + accountNo.substring(accountNo.length() - 4);
    }
}