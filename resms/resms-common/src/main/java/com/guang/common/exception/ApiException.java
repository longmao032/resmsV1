package com.guang.common.exception;

import com.guang.common.result.IResultCode;

/**
 * 自定义 API 业务异常
 */
public class ApiException extends RuntimeException {

    private final IResultCode errorCode;

    public ApiException(IResultCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(String message) {
        super(message);
        this.errorCode = null;
    }

    public ApiException(IResultCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public IResultCode getErrorCode() {
        return errorCode;
    }
}
