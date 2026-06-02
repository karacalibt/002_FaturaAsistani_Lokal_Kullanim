package com.faturaasistani.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaasistani.dto.InvoiceExtractionResult;
import com.faturaasistani.dto.InvoiceLineItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM'den gelen JSON metnini InvoiceExtractionResult'a cevirir.
 */
@Component
public class InvoiceJsonParser {

    private final ObjectMapper objectMapper;

    public InvoiceJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public InvoiceExtractionResult parse(String jsonContent) throws Exception {
        String cleaned = stripMarkdownCodeFence(jsonContent);
        JsonNode node = objectMapper.readTree(cleaned);
        return mapToResult(node);
    }

    public String stripMarkdownCodeFence(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private InvoiceExtractionResult mapToResult(JsonNode node) {
        InvoiceExtractionResult result = new InvoiceExtractionResult();
        result.setInvoiceNumber(textOrNull(node, "invoiceNumber"));
        result.setInvoiceDate(textOrNull(node, "invoiceDate"));
        result.setSupplierName(textOrNull(node, "supplierName"));
        result.setSupplierTaxNumber(textOrNull(node, "supplierTaxNumber"));
        result.setTaxOffice(textOrNull(node, "taxOffice"));
        result.setCustomerName(textOrNull(node, "customerName"));
        result.setCustomerTaxNumber(textOrNull(node, "customerTaxNumber"));
        result.setCurrency(textOrNull(node, "currency"));
        if (result.getCurrency() == null) {
            result.setCurrency("TRY");
        }
        result.setSubtotalAmount(decimalOrNull(node, "subtotalAmount"));
        result.setVatAmount(decimalOrNull(node, "vatAmount"));
        result.setTotalAmount(decimalOrNull(node, "totalAmount"));
        if (node.has("confidence") && !node.get("confidence").isNull()) {
            result.setConfidence(node.get("confidence").asDouble());
        }
        if (node.has("warnings") && node.get("warnings").isArray()) {
            List<String> warnings = new ArrayList<>();
            node.get("warnings").forEach(w -> warnings.add(w.asText()));
            result.setWarnings(warnings);
        }
        result.setLineItems(mapLineItems(node.path("lineItems")));
        return result;
    }

    private List<InvoiceLineItem> mapLineItems(JsonNode lineItemsNode) {
        List<InvoiceLineItem> items = new ArrayList<>();
        if (!lineItemsNode.isArray()) {
            return items;
        }
        for (JsonNode itemNode : lineItemsNode) {
            InvoiceLineItem item = new InvoiceLineItem();
            item.setDescription(textOrNull(itemNode, "description"));
            item.setQuantity(decimalOrNull(itemNode, "quantity"));
            item.setUnitPrice(decimalOrNull(itemNode, "unitPrice"));
            item.setVatRate(decimalOrNull(itemNode, "vatRate"));
            item.setLineTotal(decimalOrNull(itemNode, "lineTotal"));
            items.add(item);
        }
        return items;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        try {
            String text = value.asText().replace(",", ".").replaceAll("[^0-9.\\-]", "");
            if (text.isBlank()) {
                return null;
            }
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
