package com.guang.house.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(name = "HouseStatusLogVO", description = "房源状态日志VO")
public class HouseStatusLogVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "房源ID")
    private Integer houseId;

    @Schema(description = "房源编号")
    private String houseNo;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "变更前状态")
    private Byte fromStatus;

    @Schema(description = "变更后状态")
    private Byte toStatus;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "操作人ID")
    private Integer operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作IP")
    private String ipAddress;

    @Schema(description = "变更时间")
    private LocalDateTime createTime;
}
