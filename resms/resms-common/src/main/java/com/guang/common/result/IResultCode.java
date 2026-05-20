package com.guang.common.result;

/**
 * 响应码接口
 */
public interface IResultCode {
    /**
     * 获取响应状态码
     */
    Integer getCode();

    /**
     * 获取响应消息
     */
    String getMessage();
}
