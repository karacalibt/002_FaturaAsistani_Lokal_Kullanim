package com.faturaasistani.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Desteklenen dosya turlerini kontrol eder.
 */
public final class FileTypeUtil {

    public enum FileType {
        PDF, IMAGE, UNSUPPORTED
    }

    private FileTypeUtil() {
    }

    public static FileType detect(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) {
            return FileType.UNSUPPORTED;
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return FileType.PDF;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
            return FileType.IMAGE;
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("pdf")) {
                return FileType.PDF;
            }
            if (contentType.contains("jpeg") || contentType.contains("jpg") || contentType.contains("png")) {
                return FileType.IMAGE;
            }
        }
        return FileType.UNSUPPORTED;
    }

    public static void validateSupported(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosya bos veya gonderilmedi.");
        }
        if (detect(file) == FileType.UNSUPPORTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Desteklenmeyen dosya turu. Sadece PDF, JPG ve PNG kabul edilir.");
        }
    }
}
