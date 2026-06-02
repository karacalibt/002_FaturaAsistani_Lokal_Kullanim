package com.faturaasistani.service;

import com.faturaasistani.dto.InvoiceExtractionResult;
import com.faturaasistani.dto.InvoiceLineItem;
import com.faturaasistani.dto.InvoiceUploadResponse;
import com.faturaasistani.store.InvoiceMemoryStore;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Yuklenen faturalari Excel (.xlsx) olarak disa aktarir.
 */
@Service
public class ExcelExportService {

    private final InvoiceMemoryStore invoiceMemoryStore;

    public ExcelExportService(InvoiceMemoryStore invoiceMemoryStore) {
        this.invoiceMemoryStore = invoiceMemoryStore;
    }

    public byte[] exportAll() throws IOException {
        List<InvoiceUploadResponse> invoices = invoiceMemoryStore.snapshot();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet summarySheet = workbook.createSheet("Faturalar");
            createSummaryHeader(summarySheet.createRow(0));
            int rowIndex = 1;
            for (InvoiceUploadResponse upload : invoices) {
                Row row = summarySheet.createRow(rowIndex++);
                fillSummaryRow(row, upload);
            }
            autosize(summarySheet, 12);

            Sheet linesSheet = workbook.createSheet("Kalemler");
            createLineHeader(linesSheet.createRow(0));
            int lineRow = 1;
            for (InvoiceUploadResponse upload : invoices) {
                InvoiceExtractionResult inv = upload.getInvoice();
                if (inv == null || inv.getLineItems() == null) {
                    continue;
                }
                for (InvoiceLineItem item : inv.getLineItems()) {
                    Row row = linesSheet.createRow(lineRow++);
                    fillLineRow(row, upload, item);
                }
            }
            autosize(linesSheet, 8);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createSummaryHeader(Row header) {
        String[] cols = {
                "ID", "Dosya", "Yukleme", "Fatura No", "Tarih", "Tedarikci", "VKN",
                "Vergi Dairesi", "Para Birimi", "Ara Toplam", "KDV", "Toplam"
        };
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
    }

    private void fillSummaryRow(Row row, InvoiceUploadResponse upload) {
        InvoiceExtractionResult inv = upload.getInvoice();
        row.createCell(0).setCellValue(nullSafe(upload.getId()));
        row.createCell(1).setCellValue(nullSafe(upload.getFileName()));
        row.createCell(2).setCellValue(nullSafe(upload.getUploadedAt()));
        if (inv != null) {
            row.createCell(3).setCellValue(nullSafe(inv.getInvoiceNumber()));
            row.createCell(4).setCellValue(nullSafe(inv.getInvoiceDate()));
            row.createCell(5).setCellValue(nullSafe(inv.getSupplierName()));
            row.createCell(6).setCellValue(nullSafe(inv.getSupplierTaxNumber()));
            row.createCell(7).setCellValue(nullSafe(inv.getTaxOffice()));
            row.createCell(8).setCellValue(nullSafe(inv.getCurrency()));
            setDecimal(row, 9, inv.getSubtotalAmount());
            setDecimal(row, 10, inv.getVatAmount());
            setDecimal(row, 11, inv.getTotalAmount());
        }
    }

    private void createLineHeader(Row header) {
        String[] cols = {
                "Fatura ID", "Dosya", "Aciklama", "Miktar", "Birim Fiyat", "KDV %", "Satir Toplam"
        };
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
    }

    private void fillLineRow(Row row, InvoiceUploadResponse upload, InvoiceLineItem item) {
        row.createCell(0).setCellValue(nullSafe(upload.getId()));
        row.createCell(1).setCellValue(nullSafe(upload.getFileName()));
        row.createCell(2).setCellValue(nullSafe(item.getDescription()));
        setDecimal(row, 3, item.getQuantity());
        setDecimal(row, 4, item.getUnitPrice());
        setDecimal(row, 5, item.getVatRate());
        setDecimal(row, 6, item.getLineTotal());
    }

    private void setDecimal(Row row, int col, java.math.BigDecimal value) {
        if (value != null) {
            row.createCell(col).setCellValue(value.doubleValue());
        } else {
            row.createCell(col).setCellValue("");
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private void autosize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Exception ignored) {
                sheet.setColumnWidth(i, 5000);
            }
        }
    }
}
