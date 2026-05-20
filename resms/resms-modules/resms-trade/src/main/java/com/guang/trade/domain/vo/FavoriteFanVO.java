package com.guang.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "收藏粉丝视图对象")
public class FavoriteFanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "C端用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "收藏时间")
    private LocalDateTime favoriteTime;
}
