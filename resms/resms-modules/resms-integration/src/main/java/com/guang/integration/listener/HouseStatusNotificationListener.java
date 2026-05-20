package com.guang.integration.listener;

import com.guang.common.event.HouseStatusChangeEvent;
import com.guang.integration.constant.NotificationTemplate;
import com.guang.integration.domain.dto.NotificationSaveDTO;
import com.guang.integration.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 房源触发器：当房源状态变更时，自动发送通知公告
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HouseStatusNotificationListener {

    private final NotificationService notificationService;

    private static final Map<Byte, String> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put((byte) 0, "待审核");
        STATUS_MAP.put((byte) 1, "在售");
        STATUS_MAP.put((byte) 2, "已预订");
        STATUS_MAP.put((byte) 3, "已成交");
        STATUS_MAP.put((byte) 4, "已下架");
    }

    @Async
    @EventListener
    public void onHouseStatusChange(HouseStatusChangeEvent event) {
        log.info("监听到房源状态变更事件，开始生成公告。房源ID: {}, 目标状态: {}", event.getHouseId(), event.getStatus());

        // 1. 获取模版定义
        NotificationTemplate template = NotificationTemplate.HOUSE_STATUS_CHANGE;

        // 2. 填充模版内容
        // 参数：{0}=房源名称, {1}=新状态描述, {2}=原因
        String statusDesc = STATUS_MAP.getOrDefault(event.getStatus(), "未知状态");
        String content = MessageFormat.format(template.getContentPattern(), 
                event.getHouseName(), 
                statusDesc, 
                event.getReason());

        // 3. 构造通知发送 DTO
        NotificationSaveDTO saveDTO = new NotificationSaveDTO();
        saveDTO.setTitle(template.getTitle());
        saveDTO.setContent(content);
        saveDTO.setNoticeType(template.getType());
        saveDTO.setReceiverType((byte) 2); // 全体用户 (或者可以根据需求改为特定项目组)
        saveDTO.setPriority((byte) 1); // 普通
        saveDTO.setBusinessType("HOUSE");
        saveDTO.setBusinessId(Integer.valueOf(event.getHouseId()));
        saveDTO.setRouterPath("/house/house-management/detail?id=" + event.getHouseId());

        // 4. 调用服务发布
        notificationService.publishNotification(saveDTO);
        
        log.info("房源状态变更公告已下发");
    }
}
