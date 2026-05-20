package com.guang.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "过户记录视图对象")
public class TransferRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Integer id;

    @Schema(description = "关联交易ID")
    private Integer transactionId;

    @Schema(description = "过户编号")
    private String transferNo;

    @Schema(description = "不动产权证书号")
    private String certificateNo;

    @Schema(description = "不动产登记中心名称")
    private String registrationCenter;

    @Schema(description = "过户日期")
    private LocalDateTime transferDate;

    @Schema(description = "经办人ID")
    private Integer operatorId;

    @Schema(description = "经办人姓名")
    private String operatorName;

    @Schema(description = "状态：0=待过户，1=已完成")
    private Byte status;

    @Schema(description = "备注")
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
