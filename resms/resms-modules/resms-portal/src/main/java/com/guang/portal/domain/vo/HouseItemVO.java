package com.guang.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 推荐的房源卡片")
public class HouseItemVO {
    @Schema(description = "房源ID")
    private String houseId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "所在区域")
    private String district;

    @Schema(description = "售价/租金")
    private String price;

    @Schema(description = "户型")
    private String layout;

    @Schema(description = "面积")
    private String area;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "AI 推荐理由")
    private String sellingPoint;
}
