package com.faturaasistani.store;

import com.faturaasistani.dto.InvoiceUploadResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVP icin bellekte fatura kaydi (veritabani yok).
 */
@Component
public class InvoiceMemoryStore {

    private final Map<String, InvoiceUploadResponse> storage = new ConcurrentHashMap<>();

    public InvoiceUploadResponse save(InvoiceUploadResponse response) {
        String id = UUID.randomUUID().toString();
        response.setId(id);
        storage.put(id, response);
        return response;
    }

    public List<InvoiceUploadResponse> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(InvoiceUploadResponse::getUploadedAt).reversed())
                .toList();
    }

    public Optional<InvoiceUploadResponse> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public boolean delete(String id) {
        return storage.remove(id) != null;
    }

    public List<InvoiceUploadResponse> snapshot() {
        return new ArrayList<>(findAll());
    }
}
