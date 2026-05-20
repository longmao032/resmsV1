package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "贷款审批参数")
public class LoanAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "贷款记录ID不能为空")
    @Schema(description = "贷款记录ID")
    private Integer id;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：1=审核通过，2=已放款，3=未通过")
    private Byte status;

    @Schema(description = "备注")
    private String remark;
}
