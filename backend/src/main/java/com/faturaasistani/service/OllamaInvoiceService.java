package com.faturaasistani.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaasistani.config.OllamaProperties;
import com.faturaasistani.dto.InvoiceExtractionResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Cikarilan fatura metnini lokal Ollama LLM'e gonderip JSON alir.
 */
@Service
public class OllamaInvoiceService {

    private static final String SYSTEM_PROMPT = """
            Sen bir fatura okuma ve veri cikarma asistanisin.
            Verilen fatura metninden onemli bilgileri cikar.
            
            Kurallar:
            - Sadece faturada yazan bilgileri kullan.
            - Bilmedigin veya bulamadigin alanlara null yaz.
            - Tutarlari sayi olarak dondur.
            - Para birimini ayri alan olarak dondur (varsayilan TRY).
            - KDV oranlarini varsa satir bazinda cikar.
            - Cevabi SADECE gecerli JSON olarak ver, baska metin yazma.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Asagidaki JSON semasina uygun tek bir JSON nesnesi don:
            {
              "invoiceNumber": null,
              "invoiceDate": null,
              "supplierName": null,
              "supplierTaxNumber": null,
              "taxOffice": null,
              "customerName": null,
              "customerTaxNumber": null,
              "currency": "TRY",
              "subtotalAmount": null,
              "vatAmount": null,
              "totalAmount": null,
              "lineItems": [],
              "confidence": 0.0,
              "warnings": []
            }
            
            lineItems elemanlari: description, quantity, unitPrice, vatRate, lineTotal
            
            Fatura metni:
            %s
            """;

    private final OllamaProperties ollamaProperties;
    private final InvoiceJsonParser invoiceJsonParser;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OllamaInvoiceService(
            OllamaProperties ollamaProperties,
            InvoiceJsonParser invoiceJsonParser,
            ObjectMapper objectMapper) {
        this.ollamaProperties = ollamaProperties;
        this.invoiceJsonParser = invoiceJsonParser;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(ollamaProperties.getConnectTimeoutMs());
        factory.setReadTimeout(ollamaProperties.getReadTimeoutMs());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public InvoiceExtractionResult extractInvoiceData(String invoiceText) {
        if (invoiceText == null || invoiceText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fatura metni bos.");
        }

        String truncated = truncateForLlm(invoiceText);
        String userPrompt = USER_PROMPT_TEMPLATE.formatted(truncated);
        String jsonContent = callOllama(userPrompt);

        try {
            return invoiceJsonParser.parse(jsonContent);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Ollama cevabi JSON olarak parse edilemedi: " + e.getMessage(), e);
        }
    }

    private String truncateForLlm(String invoiceText) {
        int max = ollamaProperties.getMaxTextChars();
        if (invoiceText.length() <= max) {
            return invoiceText;
        }
        return invoiceText.substring(0, max) + "\n\n[... metin demo hizi icin kisaltildi ...]";
    }

    private String callOllama(String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", ollamaProperties.getModel(),
                "stream", false,
                "format", "json",
                "options", Map.of(
                        "num_predict", ollamaProperties.getMaxTokens(),
                        "temperature", 0.1
                ),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String responseBody = restClient.post()
                    .uri(ollamaProperties.chatEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        String msg = buildOllamaErrorMessage(response.getStatusCode().value(), errorBody);
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, msg);
                    })
                    .body(String.class);

            return parseOllamaContent(responseBody);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Ollama baglantisi kurulamadi (" + ollamaProperties.getBaseUrl()
                            + "). Calisiyor mu? ollama serve — " + e.getMessage(), e);
        }
    }

    private String buildOllamaErrorMessage(int status, String errorBody) {
        String model = ollamaProperties.getModel();
        String hint = "";
        try {
            JsonNode err = objectMapper.readTree(errorBody);
            String ollamaMsg = err.path("error").asText(errorBody);
            if (ollamaMsg.contains("not found")) {
                hint = " Yuklu modeller: ollama list. Hafif model: ollama pull qwen2.5:3b "
                        + "sonra export OLLAMA_MODEL=qwen2.5:3b";
            }
            return "Ollama hatasi (HTTP " + status + ", model=" + model + "): " + ollamaMsg + hint;
        } catch (Exception ignored) {
            return "Ollama hatasi (HTTP " + status + ", model=" + model + "): " + errorBody;
        }
    }

    String parseOllamaContent(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Ollama bos cevap dondurdu");
        }
        return content;
    }
}
