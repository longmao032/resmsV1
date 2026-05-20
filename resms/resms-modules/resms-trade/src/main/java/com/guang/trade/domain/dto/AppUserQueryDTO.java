package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * C端用户查询参数
 *
 * @author blackDuck
 * @since 2026-05-10
 */
@Data
@Schema(description = "C端用户查询参数")
public class AppUserQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态：0=封禁，1=正常")
    private Byte status;
}
