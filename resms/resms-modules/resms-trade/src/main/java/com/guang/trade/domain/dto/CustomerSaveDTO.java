package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 客户保存参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "客户保存参数")
public class CustomerSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID（修改时必传）")
    private Integer id;

    @Schema(description = "绑定的C端账号ID")
    private Long appUserId;

    @NotBlank(message = "客户姓名不能为空")
    @Schema(description = "客户姓名")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "客户手机号")
    private String phone;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "意向面积")
    private BigDecimal demandArea;

    @Schema(description = "意向价格")
    private BigDecimal demandPrice;

    @Schema(description = "意向户型")
    private String demandLayout;

    @Schema(description = "意向区域")
    private String demandAreaRegion;

    @NotNull(message = "意向等级不能为空")
    @Schema(description = "意向等级：1=高，2=中，3=低")
    private Byte intentionLevel;

    @Schema(description = "对接销售ID")
    private Integer salesId;

    @Schema(description = "客户来源")
    private String source;
}
