package com.guang.house.entity;

import com.baomidou.mybatisplus.annotation.*;
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
 * 房源主表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName(value = "tb_house", autoResultMap = true)
@Schema(name = "House", description = "房源主表")
public class House implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房源ID
     */
    @Schema(description = "房源ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 房源编号
     */
    @TableField("house_no")
    @Schema(description = "房源编号")
    private String houseNo;

    /**
     * 所属项目ID
     */
    @TableField("project_id")
    @Schema(description = "所属项目ID")
    private Integer projectId;

    /**
     * 冗余：项目名称（避免频繁JOIN）
     */
    @TableField("project_name")
    @Schema(description = "冗余：项目名称（避免频繁JOIN）")
    private String projectName;

    /**
     * 房源类型：1=新房，2=二手房，3=租房
     */
    @TableField("house_type")
    @Schema(description = "房源类型：1=新房，2=二手房，3=租房")
    private Byte houseType;

    /**
     * 楼栋号
     */
    @TableField("building_no")
    @Schema(description = "楼栋号")
    private String buildingNo;

    /**
     * 单元号
     */
    @TableField("unit_no")
    @Schema(description = "单元号")
    private String unitNo;

    /**
     * 房号
     */
    @TableField("room_no")
    @Schema(description = "房号")
    private String roomNo;

    /**
     * 冗余：省
     */
    @TableField("province")
    @Schema(description = "冗余：省")
    private String province;

    /**
     * 冗余：市
     */
    @TableField("city")
    @Schema(description = "冗余：市")
    private String city;

    /**
     * 冗余：区
     */
    @TableField("district")
    @Schema(description = "冗余：区")
    private String district;

    /**
     * 建筑面积（㎡）
     */
    @TableField("area")
    @Schema(description = "建筑面积（㎡）")
    private BigDecimal area;

    /**
     * 户型
     */
    @TableField("layout")
    @Schema(description = "户型")
    private String layout;

    /**
     * 所在楼层
     */
    @TableField("floor")
    @Schema(description = "所在楼层")
    private Integer floor;

    /**
     * 总楼层
     */
    @TableField("total_floor")
    @Schema(description = "总楼层")
    private Integer totalFloor;

    /**
     * 朝向
     */
    @TableField("orientation")
    @Schema(description = "朝向")
    private String orientation;

    /**
     * 装修情况
     */
    @TableField("decoration")
    @Schema(description = "装修情况")
    private String decoration;
    
    /**
     * 房源标签
     */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "房源标签")
    private List<String> tags;

    /**
     * 单价（分/㎡）= 元/㎡ × 100
     * 新房和二手房有值，租房为 null
     */
    @TableField("unit_price_fen")
    @Schema(description = "单价（分/㎡），新房/二手房有值，前端展示时 ÷100 得元/㎡")
    private Long unitPriceFen;

    /**
     * 总价（分）= 万元 × 1_000_000
     * 二手房有值，新房可由单价×面积推算，租房为 null
     */
    @TableField("total_price_fen")
    @Schema(description = "总价（分），二手房/新房有值，前端展示时 ÷100 得元，再 ÷10000 得万元")
    private Long totalPriceFen;

    /**
     * 月租（分/月）= 元/月 × 100
     * 仅租房有值，其他类型为 null
     */
    @TableField("rent_price_fen")
    @Schema(description = "月租（分/月），仅租房有值，前端展示时 ÷100 得元/月")
    private Integer rentPriceFen;

    // --- 以下为前端展示兼容字段，不映射数据库 ---

    @TableField(exist = false)
    @Schema(description = "展示用价格（万元或元/月），由后端自动换算")
    @com.fasterxml.jackson.annotation.JsonProperty("price")
    private BigDecimal price;

    @TableField(exist = false)
    @Schema(description = "展示用价格单位：1=元/㎡，2=万元，3=元/月")
    @com.fasterxml.jackson.annotation.JsonProperty("priceUnit")
    private Byte priceUnit;

    @TableField(exist = false)
    @Schema(description = "展示用单价（元/㎡）")
    @com.fasterxml.jackson.annotation.JsonProperty("unitPrice")
    private BigDecimal unitPrice;


    /**
     * 房源描述
     */
    @TableField("description")
    @Schema(description = "房源描述")
    private String description;

    /**
     * 负责销售ID
     */
    @TableField("sales_id")
    @Schema(description = "负责销售ID")
    private Integer salesId;

    /**
     * 状态：0=待审核，1=在售，2=已预订，3=已成交，4=下架
     */
    @TableField("status")
    @Schema(description = "状态：0=待审核，1=在售，2=已预订，3=已成交，4=下架")
    private Byte status;

    /**
     * 是否删除：0=未删除，1=已删除
     */
    @TableField("is_deleted")
    @Schema(description = "是否删除：0=未删除，1=已删除")
    private Byte isDeleted;

    /**
     * 地理坐标（从项目继承或单独设定）
     */
    @TableField(value = "coordinate", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "地理坐标（从项目继承或单独设定）")
    private byte[] coordinate;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
