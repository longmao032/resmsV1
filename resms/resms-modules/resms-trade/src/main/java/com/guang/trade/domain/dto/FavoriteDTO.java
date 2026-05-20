package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 收藏保存参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "收藏保存参数")
public class FavoriteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "目标类型不能为空")
    @Schema(description = "目标类型：1=房源，2=楼盘项目")
    private Byte targetType;

    @NotNull(message = "资源ID不能为空")
    @Schema(description = "资源ID")
    private Integer targetId;
}
