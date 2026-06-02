package com.faturaasistani.controller;

import com.faturaasistani.dto.InvoiceUploadResponse;
import com.faturaasistani.service.ExcelExportService;
import com.faturaasistani.service.InvoiceProcessingService;
import com.faturaasistani.store.InvoiceMemoryStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceProcessingService invoiceProcessingService;
    private final InvoiceMemoryStore invoiceMemoryStore;
    private final ExcelExportService excelExportService;

    public InvoiceController(
            InvoiceProcessingService invoiceProcessingService,
            InvoiceMemoryStore invoiceMemoryStore,
            ExcelExportService excelExportService) {
        this.invoiceProcessingService = invoiceProcessingService;
        this.invoiceMemoryStore = invoiceMemoryStore;
        this.excelExportService = excelExportService;
    }

    /**
     * PDF veya gorsel fatura yukler, metin cikarir ve AI ile JSON dondurur.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InvoiceUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return invoiceProcessingService.processUpload(file);
    }

    @GetMapping
    public List<InvoiceUploadResponse> list() {
        return invoiceMemoryStore.findAll();
    }

    @GetMapping("/{id}")
    public InvoiceUploadResponse getById(@PathVariable String id) {
        return invoiceMemoryStore.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura bulunamadi."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!invoiceMemoryStore.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fatura bulunamadi.");
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] data = excelExportService.exportAll();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=faturalar.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Excel olusturulamadi: " + e.getMessage(), e);
        }
    }
}
