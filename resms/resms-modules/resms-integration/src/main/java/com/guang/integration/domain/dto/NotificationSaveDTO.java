package com.guang.integration.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发布 DTO
 *
 * @author blackDuck
 */
@Data
@Schema(description = "通知发布参数")
public class NotificationSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "标题不能为空")
    @Schema(description = "通知标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "内容格式：1=纯文本，2=HTML，3=Markdown")
    private Byte contentType = 1;

    @NotNull(message = "通知类型不能为空")
    @Schema(description = "通知类型：1=系统通知，2=任务提醒，3=交易提醒，4=审批通知")
    private Byte noticeType;

    @NotNull(message = "接收类型不能为空")
    @Schema(description = "接收类型：1=指定用户，2=部门，3=角色，4=全体")
    private Byte receiverType;

    @Schema(description = "接收者ID列表 (当类型为指定用户、部门、角色时使用)")
    private List<Integer> receiverIds;

    @Schema(description = "优先级：1=紧急，2=重要，3=普通")
    private Byte priority = 3;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "跳转路径")
    private String routerPath;

    @Schema(description = "关联业务类型")
    private String businessType;

    @Schema(description = "关联业务ID")
    private Integer businessId;
}
