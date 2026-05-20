package com.guang.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询参数
 */
@Data
@Schema(description = "用户查询参数")
public class UserQueryDTO {

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "页大小")
    private Integer pageSize = 10;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "状态：0=禁用，1=正常")
    private Byte status;

    @Schema(description = "部门ID")
    private Integer deptId;
}
