package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志查询对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "操作日志查询对象")
public class LogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "操作人用户名")
    private String userName;

    @Schema(description = "状态：0=失败，1=成功")
    private Byte status;

    @Schema(description = "开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
