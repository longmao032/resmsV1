package com.guang.house.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源导出对象
 *
 * @author blackDuck
 */
@Data
public class HouseExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("房源编号")
    @ColumnWidth(20)
    private String houseNo;

    @ExcelProperty("项目名称")
    @ColumnWidth(25)
    private String projectName;

    @ExcelProperty("房源类型")
    @ColumnWidth(12)
    private String houseTypeText;

    @ExcelProperty("地址")
    @ColumnWidth(40)
    private String address;

    @ExcelProperty("建筑面积(㎡)")
    @ColumnWidth(15)
    private BigDecimal area;

    @ExcelProperty("户型")
    @ColumnWidth(15)
    private String layout;

    @ExcelProperty("楼层")
    @ColumnWidth(12)
    private String floorText;

    @ExcelProperty("房源标签")
    @ColumnWidth(25)
    private String tags;

    @ExcelProperty("总价/租金")
    @ColumnWidth(15)
    private String priceText;

    @ExcelProperty("单价(元/㎡)")
    @ColumnWidth(15)
    private String unitPriceText;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusText;

    @ExcelProperty("发布时间")
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
