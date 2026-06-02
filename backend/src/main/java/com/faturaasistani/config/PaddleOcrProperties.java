package com.faturaasistani.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.paddleocr")
public class PaddleOcrProperties {

    private String baseUrl = "http://localhost:8866";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 120000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public String ocrEndpoint() {
        return baseUrl.replaceAll("/$", "") + "/ocr";
    }

    public String healthEndpoint() {
        return baseUrl.replaceAll("/$", "") + "/health";
    }
}
