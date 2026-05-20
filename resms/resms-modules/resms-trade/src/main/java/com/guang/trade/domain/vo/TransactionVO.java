package com.guang.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单视图对象
 */
@Data
@Schema(description = "交易订单视图对象")
public class TransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "交易ID")
    private Integer id;

    @Schema(description = "交易编号")
    private String transactionNo;

    @Schema(description = "成交价格（元）")
    private BigDecimal dealPrice;

    @Schema(description = "定金金额（元）")
    private BigDecimal deposit;

    @Schema(description = "首付款金额（元）")
    private BigDecimal downPayment;

    @Schema(description = "贷款金额（元）")
    private BigDecimal loanAmount;

    @Schema(description = "付款方式：1=一次性，2=分期，3=按揭，4=租房")
    private Byte paymentType;

    @Schema(description = "已收总金额")
    private BigDecimal actualPaidAmount;

    @Schema(description = "交易状态：0=待付定金，1=已付定金，2=已付首付，3=已过户，4=已完成，5=已取消")
    private Byte status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "房源信息")
    private HouseInfo house;

    @Schema(description = "客户信息")
    private CustomerInfo customer;

    @Schema(description = "销售信息")
    private SalesInfo sales;

    @Data
    public static class HouseInfo {
        private Integer id;
        private String houseNo;
        private String projectName;
        private String layout;
        private BigDecimal area;
        private String coverImage;
    }

    @Data
    public static class CustomerInfo {
        private Integer id;
        private String realName;
        private String phone;
    }

    @Data
    public static class SalesInfo {
        private Integer id;
        private String realName;
    }
}
