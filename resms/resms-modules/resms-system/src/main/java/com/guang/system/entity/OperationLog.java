package com.guang.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_operation_log")
@Schema(name = "OperationLog", description = "操作日志表")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @Schema(description = "日志ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作模块
     */
    @TableField("module")
    @Schema(description = "操作模块")
    private String module;

    /**
     * 业务类型，如 HOUSE, CUSTOMER, TRANSACTION
     */
    @TableField("business_type")
    @Schema(description = "业务类型，如 HOUSE, CUSTOMER, TRANSACTION")
    private String businessType;

    /**
     * 操作类型，如 ADD, UPDATE, DELETE, EXPORT
     */
    @TableField("operation_type")
    @Schema(description = "操作类型，如 ADD, UPDATE, DELETE, EXPORT")
    private String operationType;

    /**
     * 操作描述
     */
    @Schema(description = "操作描述")
    @TableField("operation_desc")
    private String operationDesc;

    /**
     * 请求方法
     */
    @Schema(description = "请求方法")
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求URL
     */
    @TableField("request_url")
    @Schema(description = "请求URL")
    private String requestUrl;

    /**
     * 请求参数(JSON)
     */
    @TableField("request_params")
    @Schema(description = "请求参数(JSON)")
    private String requestParams;

    /**
     * 响应结果(JSON)
     */
    @TableField("response_result")
    @Schema(description = "响应结果(JSON)")
    private String responseResult;

    /**
     * 操作人ID
     */
    @TableField("user_id")
    @Schema(description = "操作人ID")
    private Integer userId;

    /**
     * 操作人用户名
     */
    @TableField("user_name")
    @Schema(description = "操作人用户名")
    private String userName;

    /**
     * 操作人部门ID
     */
    @TableField("dept_id")
    @Schema(description = "操作人部门ID")
    private Integer deptId;

    /**
     * IP地址
     */
    @TableField("ip_address")
    @Schema(description = "IP地址")
    private String ipAddress;

    /**
     * 浏览器UA
     */
    @TableField("user_agent")
    @Schema(description = "浏览器UA")
    private String userAgent;

    /**
     * 执行耗时(ms)
     */
    @TableField("execute_time")
    @Schema(description = "执行耗时(ms)")
    private Long executeTime;

    /**
     * 状态：0=失败，1=成功
     */
    @TableField("status")
    @Schema(description = "状态：0=失败，1=成功")
    private Byte status;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 风险等级：1=高危，2=中等，3=普通
     */
    @TableField("risk_level")
    @Schema(description = "风险等级：1=高危，2=中等，3=普通")
    private Byte riskLevel;

    /**
     * 操作时间
     */
    @Schema(description = "操作时间")
    @TableField("operation_time")
    private LocalDateTime operationTime;
}
