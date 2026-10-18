package com.docbridge.backend.processing.model;

import java.util.List;

public record RebuildResult(byte[] outputBytes, List<BilingualLine> lines) {
}

