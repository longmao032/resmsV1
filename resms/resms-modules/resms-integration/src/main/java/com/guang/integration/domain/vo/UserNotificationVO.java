package com.guang.integration.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户通知收件箱 VO
 *
 * @author blackDuck
 */
@Data
@Schema(description = "用户收件箱对象")
public class UserNotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联记录ID")
    private Long id;

    @Schema(description = "通知ID")
    private Integer notificationId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容简述")
    private String content;

    @Schema(description = "通知类型")
    private Byte noticeType;

    @Schema(description = "是否已读：0=未读，1=已读")
    private Byte isRead;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "发送人姓名")
    private String senderName;

    @Schema(description = "跳转路径")
    private String routerPath;
}
