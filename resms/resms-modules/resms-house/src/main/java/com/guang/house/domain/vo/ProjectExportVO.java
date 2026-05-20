package com.guang.house.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目导出对象
 *
 * @author blackDuck
 */
@Data
public class ProjectExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("项目编号")
    @ColumnWidth(20)
    private String projectNo;

    @ExcelProperty("项目名称")
    @ColumnWidth(28)
    private String projectName;

    @ExcelProperty("项目类型")
    @ColumnWidth(12)
    private String projectTypeText;

    @ExcelProperty("销售状态")
    @ColumnWidth(12)
    private String statusText;

    @ExcelProperty("地址")
    @ColumnWidth(40)
    private String address;

    @ExcelProperty("开发商")
    @ColumnWidth(20)
    private String developer;

    @ExcelProperty("物业公司")
    @ColumnWidth(20)
    private String propertyCompany;

    @ExcelProperty("总户数")
    @ColumnWidth(12)
    private Integer totalHouseholds;

    @ExcelProperty("容积率")
    @ColumnWidth(10)
    private BigDecimal plotRatio;

    @ExcelProperty("绿化率(%)")
    @ColumnWidth(12)
    private BigDecimal greeningRate;

    @ExcelProperty("物业费(元/㎡/月)")
    @ColumnWidth(16)
    private BigDecimal propertyFee;

    @ExcelProperty("佣金比例(%)")
    @ColumnWidth(14)
    private BigDecimal commissionRate;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
