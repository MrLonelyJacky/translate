package com.docbridge.backend.processing.ocr;

import com.docbridge.backend.processing.model.OcrLine;

import java.util.List;

public interface OcrService {

    List<OcrLine> extractLines(String sourceType, byte[] content);

    default String extractText(String sourceType, byte[] content) {
        return extractLines(sourceType, content).stream()
            .map(OcrLine::text)
            .filter(text -> text != null && !text.isBlank())
            .reduce((left, right) -> left + "\n" + right)
            .orElse("");
    }
}

