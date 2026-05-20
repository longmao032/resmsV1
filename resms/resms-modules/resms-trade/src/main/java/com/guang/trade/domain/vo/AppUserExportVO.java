package com.guang.trade.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C端用户导出对象
 */
@Data
public class AppUserExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("用户ID")
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty("手机号")
    @ColumnWidth(18)
    private String phone;

    @ExcelProperty("昵称")
    @ColumnWidth(22)
    private String nickname;

    @ExcelProperty("微信OpenID")
    @ColumnWidth(30)
    private String wechatOpenid;

    @ExcelProperty("微信UnionID")
    @ColumnWidth(30)
    private String unionId;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;

    @ExcelProperty("注册时间")
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
