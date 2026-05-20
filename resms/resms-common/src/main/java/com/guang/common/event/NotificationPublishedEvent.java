package com.guang.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * 通知公告发布事件 — WebSocket 实时推送
 */
@Getter
public class NotificationPublishedEvent extends ApplicationEvent {

    private final Integer notificationId;
    private final String title;
    private final String content;
    private final Byte noticeType;
    private final Set<Integer> targetUserIds;

    public NotificationPublishedEvent(Object source, Integer notificationId, String title,
                                      String content, Byte noticeType, Set<Integer> targetUserIds) {
        super(source);
        this.notificationId = notificationId;
        this.title = title;
        this.content = content;
        this.noticeType = noticeType;
        this.targetUserIds = targetUserIds;
    }
}
