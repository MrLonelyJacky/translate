package com.docbridge.backend.quota.dto;

public record QuotaResponse(int ocrTotal,
                            int ocrUsed,
                            int ocrRefunded,
                            int ocrRemaining,
                            int translateTotal,
                            int translateUsed,
                            int translateRefunded,
                            int translateRemaining) {
}
