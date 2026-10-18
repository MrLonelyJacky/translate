package com.docbridge.backend.processing.translate;

import com.docbridge.backend.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Primary
@Service
public class RoutingTranslatorService implements TranslatorService {

    private final Map<String, TranslatorProviderClient> providers;
    private final boolean enabled;
    private final String provider;
    private final ThreadLocal<String> lastUsedProvider = ThreadLocal.withInitial(() -> "unknown");

    public RoutingTranslatorService(List<TranslatorProviderClient> clients,
                                    @Value("${app.translate.enabled:true}") boolean enabled,
                                    @Value("${app.translate.provider:tencent}") String provider) {
        this.providers = clients.stream().collect(Collectors.toMap(
            c -> c.provider().toLowerCase(Locale.ROOT),
            Function.identity(),
            (left, right) -> right
        ));
        this.enabled = enabled;
        this.provider = normalize(provider, "tencent");
    }

    @Override
    public List<String> translateBatch(List<String> segments) {
        if (!enabled) {
            lastUsedProvider.set("disabled-pass-through");
            return segments;
        }
        TranslatorProviderClient primary = getProviderOrThrow(provider);
        List<String> translated = primary.translateBatch(segments);
        lastUsedProvider.set(primary.provider());
        return translated;
    }

    @Override
    public String configuredProvider() {
        return provider;
    }

    @Override
    public String lastUsedProvider() {
        return lastUsedProvider.get();
    }

    private TranslatorProviderClient getProviderOrThrow(String configuredProvider) {
        TranslatorProviderClient providerClient = providers.get(configuredProvider);
        if (providerClient != null) {
            return providerClient;
        }
        throw new BusinessException("TRANSLATION_CONFIG_ERROR", "不支持的翻译服务提供方: " + configuredProvider, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
