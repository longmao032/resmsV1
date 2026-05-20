package com.guang.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * C端用户统计数据
 */
@Data
@Schema(description = "C端用户统计数据")
public class AppUserStatisticsVO {

    @Schema(description = "用户总数")
    private long totalCount;

    @Schema(description = "本周新增")
    private long weeklyNewCount;

    @Schema(description = "正常状态数量")
    private long normalCount;

    @Schema(description = "封禁状态数量")
    private long bannedCount;
}
