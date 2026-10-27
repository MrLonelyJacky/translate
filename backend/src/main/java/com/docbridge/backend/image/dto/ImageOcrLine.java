package com.docbridge.backend.image.dto;

public record ImageOcrLine(int order,
                           String sourceText,
                           Integer left,
                           Integer top,
                           Integer right,
                           Integer bottom) {
}

