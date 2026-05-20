package com.guang.trade.domain.vo;

import com.guang.common.annotation.Sensitive;
import com.guang.common.enums.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户视图对象
 */
@Data
@Schema(description = "客户视图对象")
public class CustomerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID")
    private Integer id;

    @Schema(description = "绑定的C端账号ID")
    private Long appUserId;

    @Schema(description = "客户编号")
    private String customerNo;

    @Schema(description = "客户姓名")
    private String realName;

    @Sensitive(SensitiveType.PHONE)
    @Schema(description = "客户电话（脱敏）")
    private String phone;

    @Sensitive(SensitiveType.ID_CARD)
    @Schema(description = "身份证号（脱敏），明文请调用 /phone 接口")
    private String idCard;

    @Schema(description = "意向面积")
    private BigDecimal demandArea;

    @Schema(description = "意向价格")
    private BigDecimal demandPrice;

    @Schema(description = "意向户型")
    private String demandLayout;

    @Schema(description = "意向区域")
    private String demandAreaRegion;

    @Schema(description = "意向等级：1=高，2=中，3=低")
    private Byte intentionLevel;

    @Schema(description = "客户来源")
    private String source;

    @Schema(description = "负责销售ID")
    private Integer salesId;

    @Schema(description = "负责销售姓名")
    private String salesName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
