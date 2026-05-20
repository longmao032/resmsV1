package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 楼盘项目保存对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "楼盘项目保存对象")
public class ProjectSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID（更新时必填）")
    private Integer id;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类型：1=新房楼盘，2=二手房小区")
    private Byte projectType;

    @Schema(description = "开发商")
    private String developer;

    @Schema(description = "物业公司")
    private String propertyCompany;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "总户数")
    private Integer totalHouseholds;

    @Schema(description = "物业费（元/㎡/月）")
    private BigDecimal propertyFee;

    @Schema(description = "容积率")
    private BigDecimal plotRatio;

    @Schema(description = "绿化率(%)")
    private BigDecimal greeningRate;

    @Schema(description = "项目标签")
    private List<String> tags;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "状态：1=在售，2=售罄，3=待售，4=下架")
    private Byte status;

    @Schema(description = "佣金比例（%）")
    private BigDecimal commissionRate;
}
