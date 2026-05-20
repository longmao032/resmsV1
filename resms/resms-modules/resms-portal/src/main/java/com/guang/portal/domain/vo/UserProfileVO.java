package com.guang.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "C端用户个人资料")
public class UserProfileVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "性别：0=未知，1=男，2=女")
    private Integer gender;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "注册时间")
    private String createTime;

    @Schema(description = "收藏房源数")
    private Integer favoriteCount;

    @Schema(description = "预约记录数")
    private Integer appointmentCount;

    @Schema(description = "浏览记录数")
    private Integer browseCount;
}
