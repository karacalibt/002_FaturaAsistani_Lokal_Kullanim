package com.faturaasistani.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Gorsel faturalardan metin cikarma arayuzu.
 * Ileride Tesseract veya OpenAI Vision ile degistirilebilir.
 */
public interface OCRService {

    String extractText(MultipartFile file);
}
