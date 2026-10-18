package com.docbridge.backend.processing.ocr;

import com.docbridge.backend.processing.model.OcrLine;

import java.util.List;

public interface OcrProviderClient {

    String provider();

    List<OcrLine> extractLines(String sourceType, byte[] content);
}

