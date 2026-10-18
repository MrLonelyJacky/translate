package com.docbridge.backend.processing.translate;

import java.util.List;

public interface TranslatorProviderClient {

    String provider();

    List<String> translateBatch(List<String> segments);
}

