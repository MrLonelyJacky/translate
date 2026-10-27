package com.docbridge.backend.image.dto;

import java.util.List;

public record ImageOcrResponse(int total, String recognizedText, List<ImageOcrLine> lines) {
}

