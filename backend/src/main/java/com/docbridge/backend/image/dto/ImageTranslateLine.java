package com.docbridge.backend.image.dto;

public record ImageTranslateLine(int order,
                                 String sourceText,
                                 String translatedText,
                                 Integer left,
                                 Integer top,
                                 Integer right,
                                 Integer bottom) {
}

