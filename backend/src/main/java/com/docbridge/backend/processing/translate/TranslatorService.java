package com.docbridge.backend.processing.translate;

import java.util.List;

public interface TranslatorService {

    List<String> translateBatch(List<String> segments);

    default String configuredProvider() {
        return "unknown";
    }

    default String lastUsedProvider() {
        return configuredProvider();
    }
}

