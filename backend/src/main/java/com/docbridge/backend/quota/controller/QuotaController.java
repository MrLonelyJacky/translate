package com.docbridge.backend.quota.controller;

import com.docbridge.backend.common.api.ApiResponse;
import com.docbridge.backend.common.web.RequestIdFilter;
import com.docbridge.backend.quota.dto.QuotaResponse;
import com.docbridge.backend.quota.service.QuotaService;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quota")
public class QuotaController {

    private static final long DEV_USER_ID = 1001L;

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping
    public ApiResponse<QuotaResponse> getQuota(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.ok(quotaService.getMyQuota(userId), requestId());
    }

    private String requestId() {
        String id = MDC.get(RequestIdFilter.REQUEST_ID_KEY);
        return id == null ? "" : id;
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return DEV_USER_ID;
        }
        return Long.parseLong(authentication.getName());
    }
}

