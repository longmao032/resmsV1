package com.guang.resmsaiservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 用户意向画像 VO —— 供 AI 大模型消费的结构化画像卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户意向画像（含行为评分、特征聚合、区县均价指数）")
public class UserIntentProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "C端用户ID")
    private Long appUserId;

    @Schema(description = "最近一次活跃时间")
    private String lastActiveTime;

    @Schema(description = "购房紧迫度: HIGH / MEDIUM / LOW")
    private String urgencyLevel;

    @Schema(description = "硬性约束（零容错过滤条件）")
    private HardConstraints hardConstraints;

    @Schema(description = "软偏好（加分属性）")
    private SoftPreferences softPreferences;

    @Schema(description = "场景标签 Top5（高频出现的标签）")
    private List<String> scenarioTags;

    @Schema(description = "单价锚点（万元/㎡）—— UP Engine 联动计算的核心输入")
    private Double unitPriceAnchorWan;

    @Schema(description = "意向得分最高的 Top5 房源")
    private List<IntentHouseItem> topIntentHouses;

    @Schema(description = "当前城市各区基准均价映射（万元/㎡），供 AI 联动计算使用")
    private Map<String, Double> districtPriceIndex;

    // ========== 内嵌结构 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HardConstraints implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "城市")
        private String city;

        @Schema(description = "核心意向区域 Top2")
        private List<String> districts;

        @Schema(description = "预算下限（万元）")
        private Double minPriceWan;

        @Schema(description = "预算上限（万元）")
        private Double maxPriceWan;

        @Schema(description = "面积下限（㎡）")
        private Double minArea;

        @Schema(description = "面积上限（㎡）")
        private Double maxArea;

        @Schema(description = "偏好户型室数列表")
        private List<Integer> layoutRooms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoftPreferences implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "偏好房屋类型: new_house / resale / rental")
        private String preferredHouseType;

        @Schema(description = "偏好装修标准")
        private String preferredDecoration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentHouseItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "房源ID")
        private Integer houseId;

        @Schema(description = "房源标题")
        private String title;

        @Schema(description = "意向得分")
        private Double score;

        @Schema(description = "所属区域")
        private String district;

        @Schema(description = "面积（㎡）")
        private Double area;

        @Schema(description = "总价（万元）")
        private Double priceWan;
    }
}
