package com.guang.integration.listener;

import com.guang.common.event.LeadAssignedEvent;
import com.guang.integration.constant.NotificationTemplate;
import com.guang.integration.domain.dto.NotificationSaveDTO;
import com.guang.integration.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Collections;

/**
 * 客户触发器：当线索被指派时，自动发送通知给销售人员
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeadAssignedListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onLeadAssigned(LeadAssignedEvent event) {
        log.info("监听到线索指派事件，开始生成通知。客户: {}, 销售ID: {}", event.getCustomerName(), event.getSalesId());

        // 1. 获取模版定义
        NotificationTemplate template = NotificationTemplate.LEAD_ASSIGNED;

        // 2. 填充模版内容
        // 参数：{0}=客户姓名, {1}=联系电话, {2}=线索来源
        String content = MessageFormat.format(template.getContentPattern(), 
                event.getCustomerName(), 
                event.getPhone() != null ? event.getPhone() : "未留电话", 
                event.getSource());

        // 3. 构造通知发送 DTO
        NotificationSaveDTO saveDTO = new NotificationSaveDTO();
        saveDTO.setTitle(template.getTitle());
        saveDTO.setContent(content);
        saveDTO.setNoticeType(template.getType());
        saveDTO.setReceiverType((byte) 1); // 指定用户
        saveDTO.setReceiverIds(Collections.singletonList(event.getSalesId()));
        saveDTO.setPriority((byte) 2); // 重要
        saveDTO.setBusinessType("CUSTOMER_LEAD");
        saveDTO.setBusinessId(Integer.valueOf(event.getCustomerId()));
        saveDTO.setRouterPath("/trade/customer-management/detail?id=" + event.getCustomerId());

        // 4. 调用服务发布
        notificationService.publishNotification(saveDTO);
        
        log.info("线索指派通知已下发给销售人员 ID: {}", event.getSalesId());
    }
}
