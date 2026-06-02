package com.faturaasistani.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaasistani.config.OllamaProperties;
import com.faturaasistani.config.PaddleOcrProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo ortaminda OCR ve Ollama erisimini kontrol eder.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoHealthController {

    private final PaddleOcrProperties paddleOcrProperties;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public DemoHealthController(
            PaddleOcrProperties paddleOcrProperties,
            OllamaProperties ollamaProperties,
            ObjectMapper objectMapper) {
        this.paddleOcrProperties = paddleOcrProperties;
        this.ollamaProperties = ollamaProperties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "local-demo");
        result.put("paddleocr", ping(paddleOcrProperties.healthEndpoint()));
        Map<String, Object> ollama = checkOllama();
        result.put("ollama", ollama);
        result.put("paddleocrUrl", paddleOcrProperties.getBaseUrl());
        result.put("ollamaUrl", ollamaProperties.getBaseUrl());
        result.put("ollamaModel", ollamaProperties.getModel());
        result.put("ollamaModelReady", ollama.get("modelReady"));
        if (Boolean.FALSE.equals(ollama.get("modelReady"))) {
            result.put("hint", "ollama list ile model adini kontrol edin; export OLLAMA_MODEL=<model> veya application.yml");
        }
        String model = ollamaProperties.getModel();
        if (model.contains("qwen3") || model.contains("3.6")) {
            result.put("performanceWarning",
                    "Bu model cok agir (~20GB+ RAM). Demo icin: ollama pull qwen2.5:3b && export OLLAMA_MODEL=qwen2.5:3b");
        }
        return result;
    }

    private Map<String, Object> checkOllama() {
        Map<String, Object> status = new LinkedHashMap<>();
        String tagsUrl = ollamaProperties.getBaseUrl().replaceAll("/$", "") + "/api/tags";
        status.put("url", tagsUrl);
        try {
            String body = restClient.get().uri(tagsUrl).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            status.put("up", true);
            List<String> models = parseModelNames(body);
            status.put("installedModels", models);
            String configured = ollamaProperties.getModel();
            boolean ready = models.stream().anyMatch(m -> m.equals(configured) || m.startsWith(configured + ":"));
            status.put("modelReady", ready);
            if (!ready) {
                status.put("error", "Yapilandirilan model bulunamadi: " + configured);
            }
        } catch (Exception e) {
            status.put("up", false);
            status.put("modelReady", false);
            status.put("error", e.getMessage());
        }
        return status;
    }

    private List<String> parseModelNames(String body) {
        List<String> names = new ArrayList<>();
        try {
            JsonNode models = objectMapper.readTree(body).path("models");
            if (models.isArray()) {
                models.forEach(m -> names.add(m.path("name").asText()));
            }
        } catch (Exception ignored) {
        }
        return names;
    }

    private Map<String, Object> ping(String url) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("url", url);
        try {
            restClient.get().uri(url).accept(MediaType.APPLICATION_JSON).retrieve().toBodilessEntity();
            status.put("up", true);
        } catch (Exception e) {
            status.put("up", false);
            status.put("error", e.getMessage());
        }
        return status;
    }
}
