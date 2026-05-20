package com.guang.house.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

/**
 * <p>
 * 楼盘项目表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName(value = "tb_project", autoResultMap = true)
@Schema(name = "Project", description = "楼盘项目表")
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @Schema(description = "项目ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 项目编号
     */
    @TableField("project_no")
    @Schema(description = "项目编号")
    private String projectNo;

    /**
     * 项目名称
     */
    @TableField("project_name")
    @Schema(description = "项目名称")
    private String projectName;

    /**
     * 项目类型：1=新房楼盘，2=二手房小区
     */
    @TableField("project_type")
    @Schema(description = "项目类型：1=新房楼盘，2=二手房小区")
    private Byte projectType;

    /**
     * 开发商
     */
    @TableField("developer")
    @Schema(description = "开发商")
    private String developer;

    /**
     * 物业公司
     */
    @Schema(description = "物业公司")
    @TableField("property_company")
    private String propertyCompany;

    /**
     * 省
     */
    @TableField("province")
    @Schema(description = "省")
    private String province;

    /**
     * 市
     */
    @TableField("city")
    @Schema(description = "市")
    private String city;

    /**
     * 区
     */
    @TableField("district")
    @Schema(description = "区")
    private String district;

    /**
     * 详细地址
     */
    @TableField("address")
    @Schema(description = "详细地址")
    private String address;

    /**
     * 总户数
     */
    @Schema(description = "总户数")
    @TableField("total_households")
    private Integer totalHouseholds;

    /**
     * 物业费（元/㎡/月）
     */
    @TableField("property_fee")
    @Schema(description = "物业费（元/㎡/月）")
    private BigDecimal propertyFee;

    /**
     * 容积率
     */
    @TableField("plot_ratio")
    @Schema(description = "容积率")
    private BigDecimal plotRatio;

    /**
     * 绿化率(%)
     */
    @TableField("greening_rate")
    @Schema(description = "绿化率(%)")
    private BigDecimal greeningRate;

    /**
     * 项目标签
     */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "项目标签")
    private List<String> tags;

    /**
     * 封面图
     */
    @TableField("cover_url")
    @Schema(description = "封面图")
    private String coverUrl;

    /**
     * 地理坐标（POINT类型，SRID=4326，存储经度纬度）
     */
    @TableField(value = "coordinate", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "地理坐标（POINT类型，SRID=4326，存储经度纬度）")
    private byte[] coordinate;

    /**
     * 经度（虚拟列）
     */
    @TableField(value = "longitude", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "经度（虚拟列）")
    private BigDecimal longitude;

    /**
     * 纬度（虚拟列）
     */
    @TableField(value = "latitude", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "纬度（虚拟列）")
    private BigDecimal latitude;

    /**
     * 状态：1=在售，2=售罄，3=待售，4=下架
     */
    @TableField("status")
    @Schema(description = "状态：1=在售，2=售罄，3=待售，4=下架")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 创建人ID
     */
    @TableField("creator_id")
    @Schema(description = "创建人ID")
    private Integer creatorId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 佣金比例（%）
     */
    @TableField("commission_rate")
    @Schema(description = "佣金比例（%）")
    private BigDecimal commissionRate;
}
