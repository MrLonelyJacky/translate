package com.docbridge.backend.auth.controller;

import com.docbridge.backend.auth.dto.WechatLoginRequest;
import com.docbridge.backend.auth.dto.WechatLoginResponse;
import com.docbridge.backend.auth.dto.WechatQrUrlResponse;
import com.docbridge.backend.auth.service.AuthService;
import com.docbridge.backend.common.api.ApiResponse;
import com.docbridge.backend.common.web.RequestIdFilter;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/wechat")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<WechatLoginResponse> login(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(authService.loginByCode(request.code(), request.state()), requestId());
    }

    @GetMapping("/qr-url")
    public ApiResponse<WechatQrUrlResponse> qrUrl() {
        return ApiResponse.ok(authService.createQrLoginUrl(), requestId());
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code, @RequestParam("state") String state) {
        WechatLoginResponse loginResponse = authService.loginByCode(code, state);
        String html = authService.buildCallbackHtml(loginResponse);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
            .body(html);
    }

    private String requestId() {
        String id = MDC.get(RequestIdFilter.REQUEST_ID_KEY);
        return id == null ? "" : id;
    }
}

