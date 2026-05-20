package com.guang.trade.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单导出对象
 */
@Data
public class TransactionExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("交易编号")
    @ColumnWidth(22)
    private String transactionNo;

    @ExcelProperty("项目名称")
    @ColumnWidth(25)
    private String projectName;

    @ExcelProperty("房号")
    @ColumnWidth(14)
    private String houseNo;

    @ExcelProperty("户型")
    @ColumnWidth(12)
    private String layout;

    @ExcelProperty("建筑面积(㎡)")
    @ColumnWidth(14)
    private BigDecimal area;

    @ExcelProperty("客户姓名")
    @ColumnWidth(14)
    private String customerName;

    @ExcelProperty("客户电话")
    @ColumnWidth(16)
    private String customerPhone;

    @ExcelProperty("成交金额(元)")
    @ColumnWidth(16)
    private BigDecimal dealPrice;

    @ExcelProperty("定金(元)")
    @ColumnWidth(14)
    private BigDecimal deposit;

    @ExcelProperty("已收金额(元)")
    @ColumnWidth(14)
    private BigDecimal actualPaidAmount;

    @ExcelProperty("付款方式")
    @ColumnWidth(14)
    private String paymentTypeText;

    @ExcelProperty("交易状态")
    @ColumnWidth(14)
    private String statusText;

    @ExcelProperty("负责销售")
    @ColumnWidth(14)
    private String salesName;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
