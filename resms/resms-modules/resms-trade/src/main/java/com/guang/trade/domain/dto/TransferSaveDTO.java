package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "过户保存参数")
public class TransferSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "交易ID不能为空")
    @Schema(description = "交易ID")
    private Integer transactionId;

    @Schema(description = "不动产权证书号")
    private String certificateNo;

    @Schema(description = "不动产登记中心名称")
    private String registrationCenter;

    @Schema(description = "过户日期")
    private LocalDateTime transferDate;

    @Schema(description = "备注")
    private String remark;
}
