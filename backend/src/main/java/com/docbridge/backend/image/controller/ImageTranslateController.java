package com.docbridge.backend.image.controller;

import com.docbridge.backend.common.api.ApiResponse;
import com.docbridge.backend.common.web.RequestIdFilter;
import com.docbridge.backend.image.dto.ImageTranslateResponse;
import com.docbridge.backend.image.service.ImageTranslateService;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/image")
public class ImageTranslateController {

    private static final long DEV_USER_ID = 1001L;

    private final ImageTranslateService imageTranslateService;

    public ImageTranslateController(ImageTranslateService imageTranslateService) {
        this.imageTranslateService = imageTranslateService;
    }

    @PostMapping("/translate")
    public ApiResponse<ImageTranslateResponse> translate(Authentication authentication, @RequestPart("file") MultipartFile file) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.ok(imageTranslateService.translate(userId, file), requestId());
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

