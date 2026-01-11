package com.banking.statement.service;

import com.banking.statement.dto.TransactionDTO;
import com.banking.statement.model.Statement;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class PdfGenerationService {

    @Value("${statement.pdf.directory:./statements}")
    private String pdfDirectory;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String generateStatementPdf(Statement statement, List<TransactionDTO> transactions) {
        try {
            // Create directory if not exists
            Path directoryPath = Paths.get(pdfDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Generate filename
            String filename = String.format("%s_%s.pdf",
                    statement.getStatementId(),
                    statement.getStatementDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            String filePath = Paths.get(pdfDirectory, filename).toString();

            // Create PDF
            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add header
            addHeader(document, statement);

            // Add account summary
            addAccountSummary(document, statement);

            // Add transactions table
            if (transactions != null && !transactions.isEmpty()) {
                addTransactionsTable(document, transactions);
            }

            // Add footer
            addFooter(document, statement);

            document.close();

            log.info("PDF generated successfully: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Error generating PDF for statement: {}", statement.getStatementId(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, Statement statement) {
        document.add(new Paragraph("BANK STATEMENT")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("\n"));

        document.add(new Paragraph(String.format("Statement ID: %s", statement.getStatementId()))
                .setFontSize(10));
        document.add(new Paragraph(String.format("Account Number: %s", statement.getAccountNumber()))
                .setFontSize(10));
        document.add(new Paragraph(String.format("Statement Date: %s",
                statement.getStatementDate().format(DATE_FORMATTER)))
                .setFontSize(10));
        document.add(new Paragraph(String.format("Period: %s to %s",
                statement.getPeriodStart().format(DATE_FORMATTER),
                statement.getPeriodEnd().format(DATE_FORMATTER)))
                .setFontSize(10));

        document.add(new Paragraph("\n"));
    }

    private void addAccountSummary(Document document, Statement statement) {
        document.add(new Paragraph("Account Summary")
                .setFontSize(14)
                .setBold());

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();

        summaryTable.addCell(createCell("Opening Balance:", true));
        summaryTable.addCell(createCell(formatAmount(statement.getOpeningBalance()), false));

        summaryTable.addCell(createCell("Total Credits:", true));
        summaryTable.addCell(createCell(formatAmount(statement.getTotalCredits()), false));

        summaryTable.addCell(createCell("Total Debits:", true));
        summaryTable.addCell(createCell(formatAmount(statement.getTotalDebits()), false));

        summaryTable.addCell(createCell("Closing Balance:", true));
        summaryTable.addCell(createCell(formatAmount(statement.getClosingBalance()), false));

        summaryTable.addCell(createCell("Total Transactions:", true));
        summaryTable.addCell(createCell(String.valueOf(statement.getTransactionCount()), false));

        document.add(summaryTable);
        document.add(new Paragraph("\n"));
    }

    private void addTransactionsTable(Document document, List<TransactionDTO> transactions) {
        document.add(new Paragraph("Transaction Details")
                .setFontSize(14)
                .setBold());

        Table transactionsTable = new Table(UnitValue.createPercentArray(new float[]{15, 25, 20, 20, 20}))
                .useAllAvailableWidth();

        // Headers
        transactionsTable.addHeaderCell(createHeaderCell("Date"));
        transactionsTable.addHeaderCell(createHeaderCell("Description"));
        transactionsTable.addHeaderCell(createHeaderCell("Type"));
        transactionsTable.addHeaderCell(createHeaderCell("Amount"));
        transactionsTable.addHeaderCell(createHeaderCell("Balance"));

        // Rows
        for (TransactionDTO txn : transactions) {
            transactionsTable.addCell(createCell(txn.getTransactionDate().format(DATETIME_FORMATTER), false));
            transactionsTable.addCell(createCell(
                    txn.getDescription() != null ? txn.getDescription() : txn.getReference(), false));
            transactionsTable.addCell(createCell(txn.getTransactionType(), false));
            transactionsTable.addCell(createCell(formatAmount(txn.getAmount()), false));
            transactionsTable.addCell(createCell(formatAmount(txn.getBalanceAfter()), false));
        }

        document.add(transactionsTable);
        document.add(new Paragraph("\n"));
    }

    private void addFooter(Document document, Statement statement) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("This is a computer-generated statement and does not require a signature.")
                .setFontSize(8)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(String.format("Generated on: %s",
                statement.getGeneratedAt() != null ?
                        statement.getGeneratedAt().format(DATETIME_FORMATTER) :
                        "Processing"))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private Cell createCell(String content, boolean isBold) {
        Paragraph paragraph = new Paragraph(content).setFontSize(10);
        if (isBold) {
            paragraph.setBold();
        }
        return new Cell().add(paragraph);
    }

    private Cell createHeaderCell(String content) {
        return new Cell().add(new Paragraph(content)
                .setFontSize(10)
                .setBold())
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%,.2f", amount);
    }
}
