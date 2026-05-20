package com.guang.trade.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentOverdueVO {
    private Integer transactionId;
    private String transactionNo;
    private String payName;
    private BigDecimal receivableAmount;
    private LocalDateTime dueDate;
    private Integer salesId;
}
