package com.docbridge.backend.image.controller;

import com.docbridge.backend.common.api.ApiResponse;
import com.docbridge.backend.common.web.RequestIdFilter;
import com.docbridge.backend.image.dto.ImageTranslateResponse;
import com.docbridge.backend.image.service.ImageTranslateService;
import com.docbridge.backend.processing.translate.dto.TextTranslateRequest;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/text")
public class TextTranslateController {

    private static final long DEV_USER_ID = 1001L;

    private final ImageTranslateService imageTranslateService;

    public TextTranslateController(ImageTranslateService imageTranslateService) {
        this.imageTranslateService = imageTranslateService;
    }

    @PostMapping("/translate")
    public ApiResponse<ImageTranslateResponse> translate(Authentication authentication, @RequestBody TextTranslateRequest request) {
        Long userId = resolveUserId(authentication);
        String text = request == null ? null : request.text();
        return ApiResponse.ok(imageTranslateService.translateText(userId, text), requestId());
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

