package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 房源保存对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "房源保存对象")
public class HouseSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "房源ID（更新时必填）")
    private Integer id;

    @Schema(description = "所属项目ID")
    private Integer projectId;

    @Schema(description = "房源类型：1=新房，2=二手房，3=租房")
    private Byte houseType;

    @Schema(description = "楼栋号")
    private String buildingNo;

    @Schema(description = "单元号")
    private String unitNo;

    @Schema(description = "房号")
    private String roomNo;

    @Schema(description = "建筑面积（㎡）")
    private BigDecimal area;

    @Schema(description = "户型")
    private String layout;

    @Schema(description = "所在楼层")
    private Integer floor;

    @Schema(description = "总楼层")
    private Integer totalFloor;

    @Schema(description = "朝向")
    private String orientation;

    @Schema(description = "装修情况")
    private String decoration;

    @Schema(description = "房源标签")
    private List<String> tags;

    @Schema(description = "单价（分/㎡），新房/二手房传入，= 元/㎡ × 100")
    private Long unitPriceFen;

    @Schema(description = "总价（分），二手房/新房传入，= 万元 × 1_000_000")
    private Long totalPriceFen;

    @Schema(description = "月租（分/月），租房传入，= 元/月 × 100")
    private Integer rentPriceFen;

    // --- 兼容旧版表单提交 ---
    @Schema(description = "旧版：价格")
    private BigDecimal price;

    @Schema(description = "旧版：价格单位")
    private Byte priceUnit;

    @Schema(description = "房源描述")
    private String description;

    @Schema(description = "负责销售ID")
    private Integer salesId;

    @Schema(description = "状态：0=待审核，1=在售，2=已预订，3=已成交，4=下架")
    private Byte status;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "新房扩展信息")
    private NewHouseExtendDTO newHouseExtend;

    @Schema(description = "二手房扩展信息")
    private SecondHouseExtendDTO secondHouseExtend;

    @Schema(description = "租房扩展信息")
    private RentHouseExtendDTO rentHouseExtend;

    @Schema(description = "房源图片列表")
    private List<HouseImageSaveDTO> images;
}

@Data
@Schema(description = "新房扩展信息")
class NewHouseExtendDTO implements Serializable {
    private String preSaleLicenseNo;
    private BigDecimal recordPrice;
    private BigDecimal avgPrice;
    private Integer propertyRightYears;
    private LocalDate estimatedDeliveryDate;
    private String deliveryStandard;
    private String elevatorRatio;
    private BigDecimal actualAreaRate;
}

@Data
@Schema(description = "二手房扩展信息")
class SecondHouseExtendDTO implements Serializable {
    private Integer buildYear;
    private String houseUsage;
    private Byte isOnlyHouse;
    private Byte isFullTwo;
    private Byte isFullFive;
    private Byte mortgageStatus;
    private String propertyDeed;
    private String propertyDeedUrl;
    private LocalDateTime lastTransactionTime;
}

@Data
@Schema(description = "租房扩展信息")
class RentHouseExtendDTO implements Serializable {
    private BigDecimal monthlyRent;
    private Byte rentType;
    private String depositMethod;
    private BigDecimal depositAmount;
    private LocalDate checkInDate;
    private Integer minLeasePeriod;
    private Byte supportShortRent;
    private String appliances;
}
