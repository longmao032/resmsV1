package com.guang.aiassistant.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一 API 响应封装
 */
@Data
@AllArgsConstructor
public class ApiResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
