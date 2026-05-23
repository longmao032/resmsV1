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
@Schema(description = "AI 推荐的楼盘/小区卡片")
public class ProjectItemVO {
    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "所在区域")
    private String district;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "开发商")
    private String developer;

    @Schema(description = "在售套数")
    private String houseCount;

    @Schema(description = "价格区间")
    private String priceRange;

    @Schema(description = "面积区间")
    private String areaRange;

    @Schema(description = "可选户型")
    private String layouts;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "AI 推荐理由")
    private String sellingPoint;
}
