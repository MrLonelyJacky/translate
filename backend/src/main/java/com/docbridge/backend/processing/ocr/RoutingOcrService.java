package com.docbridge.backend.processing.ocr;

import com.docbridge.backend.common.exception.BusinessException;
import com.docbridge.backend.processing.model.OcrLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Primary
@Service
public class RoutingOcrService implements OcrService {

    private final Map<String, OcrProviderClient> providers;
    private final String provider;

    public RoutingOcrService(List<OcrProviderClient> clients,
                             @Value("${app.ocr.provider:tencent}") String provider) {
        this.providers = clients.stream().collect(Collectors.toMap(
            c -> c.provider().toLowerCase(Locale.ROOT),
            Function.identity(),
            (left, right) -> right
        ));
        this.provider = normalize(provider, "tencent");
    }

    @Override
    public List<OcrLine> extractLines(String sourceType, byte[] content) {
        OcrProviderClient primary = getProviderOrThrow(provider);
        return primary.extractLines(sourceType, content);
    }

    private OcrProviderClient getProviderOrThrow(String configuredProvider) {
        OcrProviderClient providerClient = providers.get(configuredProvider);
        if (providerClient != null) {
            return providerClient;
        }
        throw new BusinessException("OCR_CONFIG_ERROR", "不支持的 OCR 服务提供方: " + configuredProvider, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
