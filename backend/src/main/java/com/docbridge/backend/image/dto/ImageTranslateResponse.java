package com.docbridge.backend.image.dto;

import java.util.List;

public record ImageTranslateResponse(int total, List<ImageTranslateLine> lines) {
}

