package com.guang.resms.exception;

import com.guang.common.exception.ApiException;
import com.guang.common.result.CommonResult;
import com.guang.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常 ApiException
     */
    @ExceptionHandler(value = ApiException.class)
    public CommonResult<Object> handleApiException(ApiException e) {
        if (e.getErrorCode() != null) {
            return CommonResult.failed(e.getErrorCode(), e.getMessage());
        }
        return CommonResult.failed(e.getMessage());
    }

    /**
     * 处理方法参数校验异常 (JSR303)
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public CommonResult<Object> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getField() + fieldError.getDefaultMessage();
            }
        }
        return CommonResult.validateFailed(message);
    }

    /**
     * 处理绑定异常 (表单提交校验)
     */
    @ExceptionHandler(value = BindException.class)
    public CommonResult<Object> handleBindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getField() + fieldError.getDefaultMessage();
            }
        }
        return CommonResult.validateFailed(message);
    }

    /**
     * 处理 404 资源不存在异常
     */
    @ExceptionHandler(value = org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public CommonResult<Object> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return CommonResult.failed(ResultCode.NOT_FOUND, "请求的资源不存在: " + e.getResourcePath());
    }

    /**
     * 处理其他未知运行时异常
     */
    @ExceptionHandler(value = RuntimeException.class)
    public CommonResult<Object> handleRuntimeException(RuntimeException e) {
        log.error("系统运行时异常: ", e);
        return CommonResult.failed(ResultCode.FAILED, e.getMessage());
    }

    /**
     * 处理所有不可知异常
     */
    @ExceptionHandler(value = Exception.class)
    public CommonResult<Object> handleException(Exception e) {
        log.error("系统发生未知错误: ", e);
        return CommonResult.failed(ResultCode.FAILED, "系统繁忙，请稍后再试");
    }
}
