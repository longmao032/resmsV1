package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 交易查询参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "交易查询参数")
public class TransactionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "交易编号")
    private String transactionNo;

    @Schema(description = "房源ID")
    private Integer houseId;

    @Schema(description = "客户ID")
    private Integer customerId;

    @Schema(description = "销售ID")
    private Integer salesId;

    @Schema(description = "交易状态：0=待付定金，1=已付定金，2=已付首付，3=已过户，4=已完成，5=已取消")
    private Byte status;
}
