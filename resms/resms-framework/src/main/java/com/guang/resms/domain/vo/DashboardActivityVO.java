package com.guang.resms.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "最新动态")
public class DashboardActivityVO {

    @Schema(description = "动态内容")
    private String content;

    @Schema(description = "时间描述")
    private String timestamp;

    @Schema(description = "操作人")
    private String user;

    @Schema(description = "类型：primary/success/info/warning/danger")
    private String type;
}
