package com.guang.integration.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知展示 VO (管理端)
 *
 * @author blackDuck
 */
@Data
@Schema(description = "通知展示对象")
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "通知类型")
    private Byte noticeType;

    @Schema(description = "接收类型")
    private Byte receiverType;

    @Schema(description = "状态：0=草稿，1=已发送")
    private Byte status;

    @Schema(description = "撤回状态：0=正常，1=已撤回")
    private Byte withdrawStatus;

    @Schema(description = "应接收人数")
    private Integer totalReceiverCount;

    @Schema(description = "已读人数")
    private Integer readCount;

    @Schema(description = "发送人姓名")
    private String senderName;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
