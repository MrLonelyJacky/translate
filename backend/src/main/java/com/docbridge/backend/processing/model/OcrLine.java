package com.docbridge.backend.processing.model;

public record OcrLine(int order, String text, Integer left, Integer top, Integer right, Integer bottom) {
}

