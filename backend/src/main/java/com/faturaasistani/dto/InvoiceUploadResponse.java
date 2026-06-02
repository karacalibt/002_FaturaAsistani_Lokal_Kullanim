package com.faturaasistani.dto;

/**
 * Upload endpoint cevabi.
 */
public class InvoiceUploadResponse {

    private String id;
    private String fileName;
    private String rawText;
    private InvoiceExtractionResult invoice;
    private String uploadedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public InvoiceExtractionResult getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceExtractionResult invoice) {
        this.invoice = invoice;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(String uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
