package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 楼盘项目查询对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "楼盘项目查询对象")
public class ProjectQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "状态：1=在售，2=售罄，3=待售，4=下架")
    private Byte status;

    @Schema(description = "项目类型：1=新房，2=二手房")
    private Byte projectType;

    @Schema(description = "中心点经度")
    private Double longitude;

    @Schema(description = "中心点纬度")
    private Double latitude;

    @Schema(description = "搜索半径 (米)")
    private Double radius;
}
