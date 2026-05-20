package com.guang.finance.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水导出对象
 */
@Data
public class PaymentExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("收据编号")
    @ColumnWidth(22)
    private String receiptNo;

    @ExcelProperty("交易编号")
    @ColumnWidth(22)
    private String transactionNo;

    @ExcelProperty("资金流向")
    @ColumnWidth(12)
    private String flowTypeText;

    @ExcelProperty("款项类型")
    @ColumnWidth(14)
    private String paymentTypeText;

    @ExcelProperty("金额(元)")
    @ColumnWidth(14)
    private BigDecimal amount;

    @ExcelProperty("实收金额(元)")
    @ColumnWidth(14)
    private BigDecimal actualAmount;

    @ExcelProperty("支付方式")
    @ColumnWidth(14)
    private String paymentMethod;

    @ExcelProperty("付款人")
    @ColumnWidth(16)
    private String payerInfo;

    @ExcelProperty("审核状态")
    @ColumnWidth(12)
    private String statusText;

    @ExcelProperty("经办财务")
    @ColumnWidth(14)
    private String financeName;

    @ExcelProperty("审核人")
    @ColumnWidth(14)
    private String auditName;

    @ExcelProperty("发生时间")
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
