package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房源查询对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "房源查询对象")
public class HouseQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "所属项目ID")
    private Integer projectId;

    @Schema(description = "房源类型：1=新房，2=二手房，3=租房")
    private Byte houseType;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "单价下限（分/㎡），新房/二手房筛选，对应 idx_search_new / idx_search_second")
    private Long minUnitPriceFen;

    @Schema(description = "单价上限（分/㎡）")
    private Long maxUnitPriceFen;

    @Schema(description = "总价下限（分），二手房/新房筛选，对应 idx_search_second")
    private Long minTotalPriceFen;

    @Schema(description = "总价上限（分）")
    private Long maxTotalPriceFen;

    @Schema(description = "月租下限（分/月），租房筛选，对应 idx_search_rental")
    private Integer minRentPriceFen;

    @Schema(description = "月租上限（分/月）")
    private Integer maxRentPriceFen;

    @Schema(description = "最小面积")
    private BigDecimal minArea;

    @Schema(description = "最大面积")
    private BigDecimal maxArea;

    @Schema(description = "户型")
    private String layout;

    @Schema(description = "状态：1=在售，2=已预订，3=已成交，4=下架")
    private Byte status;

    @Schema(description = "中心点经度")
    private Double longitude;

    @Schema(description = "中心点纬度")
    private Double latitude;

    @Schema(description = "搜索半径 (米)")
    private Double radius;

    @Schema(description = "房源编号")
    private String houseNo;

    @Schema(description = "房号")
    private String roomNo;
}
