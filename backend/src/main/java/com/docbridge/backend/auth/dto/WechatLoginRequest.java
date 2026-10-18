package com.docbridge.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(
    @NotBlank(message = "code 不能为空") String code,
    String state
) {
}

