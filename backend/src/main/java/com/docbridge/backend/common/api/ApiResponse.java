package com.docbridge.backend.common.api;

public record ApiResponse<T>(String code, String message, String requestId, T data) {

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>("OK", "success", requestId, data);
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(code, message, requestId, null);
    }
}

