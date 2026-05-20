package com.guang.integration.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话详情 VO（包含成员列表和最近消息）
 */
@Data
@Schema(description = "会话详情")
public class SessionVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话类型：1=员工私聊，2=员工群聊，3=销售与C端客户")
    private Byte sessionType;

    @Schema(description = "会话名称")
    private String sessionName;

    @Schema(description = "最后一条消息内容")
    private String lastMessageContent;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "当前用户未读数")
    private Integer unreadCount;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "成员快照列表（头像、昵称等，用于展示）")
    private List<MemberVO> members;

    @Data
    @Schema(description = "成员快照")
    public static class MemberVO {
        @Schema(description = "用户类型：1=员工，2=C端客户")
        private Byte userType;
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "用户名称")
        private String userName;
        @Schema(description = "用户头像")
        private String userAvatar;
    }
}
