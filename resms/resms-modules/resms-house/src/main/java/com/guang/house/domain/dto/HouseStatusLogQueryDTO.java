package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

@Data
@Schema(name = "HouseStatusLogQueryDTO", description = "房源状态日志查询DTO")
public class HouseStatusLogQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "房源ID")
    private Integer houseId;

    @Schema(description = "房源编号或项目名称")
    private String houseNo;

    @Schema(description = "操作人姓名")
    private String operatorName;
}
