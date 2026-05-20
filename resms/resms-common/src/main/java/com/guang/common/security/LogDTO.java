package com.guang.common.security;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志数据传输对象
 */
@Data
public class LogDTO implements Serializable {
    private String module;
    private String businessType;
    private String operationType;
    private String operationDesc;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private Integer userId;
    private String userName;
    private Integer deptId;
    private String ipAddress;
    private String userAgent;
    private Long executeTime;
    private Byte status;
    private String errorMsg;
    private LocalDateTime operationTime;
}
