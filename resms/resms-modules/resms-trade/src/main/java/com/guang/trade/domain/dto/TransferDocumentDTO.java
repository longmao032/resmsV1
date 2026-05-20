package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "过户文件上传参数")
public class TransferDocumentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "过户记录ID不能为空")
    @Schema(description = "过户记录ID")
    private Integer transferId;

    @NotNull(message = "文件类型不能为空")
    @Schema(description = "文件类型：tax_receipt/new_deed/app_form/other")
    private String docType;

    @Schema(description = "文件原名")
    private String docName;

    @NotNull(message = "文件路径不能为空")
    @Schema(description = "文件存储路径")
    private String fileUrl;
}
