package com.docbridge.backend.auth.dto;

public record WechatQrUrlResponse(String authUrl, String state, long expiresInSeconds) {
}

