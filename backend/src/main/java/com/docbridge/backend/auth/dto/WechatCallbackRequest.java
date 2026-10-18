package com.docbridge.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatCallbackRequest(
    @NotBlank(message = "code 不能为空") String code,
    @NotBlank(message = "state 不能为空") String state
) {
}

