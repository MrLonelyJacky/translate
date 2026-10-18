package com.docbridge.backend.processing.model;

import java.util.List;

public record ParsedDocument(boolean ocrRequired, List<String> segments) {
}

