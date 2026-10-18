package com.docbridge.backend.auth.dto;

public record WechatLoginResponse(
    String token,
    long expiresIn,
    UserProfile user
) {
    public record UserProfile(Long id, String unionId, String openId, String role) {
    }
}

