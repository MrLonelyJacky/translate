package com.docbridge.backend.quota.dto;

public record QuotaResponse(int freeTotal, int freeUsed, int freeRefunded, int freeRemaining) {
}

