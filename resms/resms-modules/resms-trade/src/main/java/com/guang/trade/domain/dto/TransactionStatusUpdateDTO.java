package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 交易状态更新参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "交易状态更新参数")
public class TransactionStatusUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "交易ID不能为空")
    @Schema(description = "交易ID")
    private Integer id;

    @NotNull(message = "目标状态不能为空")
    @Schema(description = "交易状态：1=已付定金，2=已付首付，3=已过户，4=已完成，5=已取消")
    private Byte status;

    @Schema(description = "变更原因/备注")
    private String remark;
}
