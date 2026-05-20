package com.guang.resmsaiservice.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVo implements Serializable {
    private static final long serialVersionUID = 1L;
       /**
     * 项目ID
     */
    @Schema(description = "项目ID")
    private Integer id;


    /**
     * 项目名称
     */
    @Schema(description = "项目名称")
    private String projectName;

    /**
     * 项目类型：1=新房楼盘，2=二手房小区
     */

    @Schema(description = "项目类型：1=新房楼盘，2=二手房小区")
    private Byte projectType;

    /**
     * 开发商
     */

    @Schema(description = "开发商")
    private String developer;

    /**
     * 物业公司
     */
    @Schema(description = "物业公司")
    private String propertyCompany;

    /**
     * 省
     */
    @Schema(description = "省")
    private String province;

    /**
     * 市
     */
    @Schema(description = "市")
    private String city;

    /**
     * 区
     */
    @Schema(description = "区")
    private String district;

    /**
     * 详细地址
     */
    @Schema(description = "详细地址")
    private String address;

    /**
     * 总户数
     */
    @Schema(description = "总户数")
    private Integer totalHouseholds;

    /**
     * 物业费（元/㎡/月）
     */
    @Schema(description = "物业费（元/㎡/月）")
    private BigDecimal propertyFee;

    /**
     * 容积率
     */
    @Schema(description = "容积率")
    private BigDecimal plotRatio;

    /**
     * 绿化率(%)
     */
    @Schema(description = "绿化率(%)")
    private BigDecimal greeningRate;

    /**
     * 项目标签
     */
    @Schema(description = "项目标签")
    private List<String> tags;

    /**
     * 封面图
     */
    @Schema(description = "封面图")
    private String coverUrl;

    /**
     * 经度（虚拟列）
     */
    @Schema(description = "经度（虚拟列）")
    private BigDecimal longitude;

    /**
     * 纬度（虚拟列）
     */
    @Schema(description = "纬度（虚拟列）")
    private BigDecimal latitude;
    private LocalDateTime updateTime;

    /**
     * 项目下的房源列表
     */
    @Schema(description = "项目下的房源列表")
    private List<HouseAiVo> houses;

}
