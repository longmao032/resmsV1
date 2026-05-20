package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 房源审核请求对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "房源审核请求对象")
public class HouseAuditDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "房源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房源ID不能为空")
    private Integer id;

    @Schema(description = "审核状态 (1-通过, 2-驳回)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核状态不能为空")
    private Byte auditStatus;

    @Schema(description = "审核意见/驳回原因")
    private String reason;
}
