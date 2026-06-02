package com.faturaasistani.service;

import com.faturaasistani.dto.InvoiceExtractionResult;
import com.faturaasistani.dto.InvoiceUploadResponse;
import com.faturaasistani.store.InvoiceMemoryStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Dosyayi PaddleOCR'e gonderir, metni Ollama ile JSON'a cevirir.
 */
@Service
public class InvoiceProcessingService {

    private final PaddleOcrClientService paddleOcrClientService;
    private final OllamaInvoiceService ollamaInvoiceService;
    private final InvoiceMemoryStore invoiceMemoryStore;

    public InvoiceProcessingService(
            PaddleOcrClientService paddleOcrClientService,
            OllamaInvoiceService ollamaInvoiceService,
            InvoiceMemoryStore invoiceMemoryStore) {
        this.paddleOcrClientService = paddleOcrClientService;
        this.ollamaInvoiceService = ollamaInvoiceService;
        this.invoiceMemoryStore = invoiceMemoryStore;
    }

    public InvoiceUploadResponse processUpload(MultipartFile file) {
        FileTypeUtil.validateSupported(file);

        String rawText = paddleOcrClientService.extractText(file);
        if (rawText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Dosyadan metin cikarilamadi.");
        }

        InvoiceExtractionResult invoice = ollamaInvoiceService.extractInvoiceData(rawText);

        InvoiceUploadResponse response = new InvoiceUploadResponse();
        response.setFileName(file.getOriginalFilename());
        response.setRawText(rawText);
        response.setInvoice(invoice);
        response.setUploadedAt(Instant.now().toString());

        return invoiceMemoryStore.save(response);
    }
}
