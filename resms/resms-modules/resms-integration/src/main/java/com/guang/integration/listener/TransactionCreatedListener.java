package com.guang.integration.listener;

import com.guang.common.event.TransactionCreatedEvent;
import com.guang.integration.constant.NotificationTemplate;
import com.guang.integration.domain.dto.NotificationSaveDTO;
import com.guang.integration.service.NotificationService;
import com.guang.trade.entity.Customer;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.CustomerMapper;
import com.guang.trade.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Collections;

/**
 * 交易触发器：当交易创建时，自动发送通知公告给销售人员与买方客户
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCreatedListener {

    private final NotificationService notificationService;
    private final TransactionMapper transactionMapper;
    private final CustomerMapper customerMapper;

    @Async
    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        if (event == null) {
            log.warn("交易创建事件为 null，跳过处理");
            return;
        }
        log.info("监听到交易创建事件，开始生成通知。交易ID: {}", event.getTransactionId());

        Integer transactionId = event.getTransactionId();
        Integer ownerId = event.getOwnerId();
        if (transactionId == null || ownerId == null) {
            log.warn("交易创建事件核心数据缺失，transactionId: {}, ownerId: {}", transactionId, ownerId);
            return;
        }

        // 1. 获取模版定义
        NotificationTemplate template = NotificationTemplate.TRANSACTION_CREATED;

        // 2. 填充模版内容
        String houseName = event.getHouseName() != null ? event.getHouseName() : "未知房源";
        String dealPriceStr = event.getDealPrice() != null ? event.getDealPrice().toString() : "0";
        String content = MessageFormat.format(template.getContentPattern(), 
                houseName, 
                transactionId.toString(), 
                dealPriceStr);

        // 3. 构造通知发送 DTO
        NotificationSaveDTO saveDTO = new NotificationSaveDTO();
        saveDTO.setTitle(template.getTitle());
        saveDTO.setContent(content);
        saveDTO.setNoticeType(template.getType());
        saveDTO.setReceiverType((byte) 1); // 指定用户
        saveDTO.setReceiverIds(Collections.singletonList(ownerId));
        saveDTO.setPriority((byte) 2); // 重要
        saveDTO.setBusinessType("TRANSACTION");
        saveDTO.setBusinessId(transactionId);
        saveDTO.setRouterPath("/trade/transaction/detail?id=" + transactionId);

        // 4. 调用服务发布给销售
        notificationService.publishNotification(saveDTO);
        log.info("交易创建通知已下发给销售人员 ID: {}", ownerId);

        // 5. 获取 C 端买方客户并推送
        try {
            Long appUserId = getAppUserIdByTransactionId(transactionId);
            if (appUserId != null) {
                NotificationTemplate clientTemplate = NotificationTemplate.CLIENT_TRANSACTION_CREATED;
                String clientContent = MessageFormat.format(clientTemplate.getContentPattern(),
                        houseName,
                        transactionId.toString(),
                        dealPriceStr);

                NotificationSaveDTO clientSaveDTO = new NotificationSaveDTO();
                clientSaveDTO.setTitle(clientTemplate.getTitle());
                clientSaveDTO.setContent(clientContent);
                clientSaveDTO.setNoticeType(clientTemplate.getType());
                clientSaveDTO.setReceiverType((byte) 1);
                clientSaveDTO.setReceiverIds(Collections.singletonList(appUserId.intValue()));
                clientSaveDTO.setPriority((byte) 2);
                clientSaveDTO.setBusinessType("TRANSACTION");
                clientSaveDTO.setBusinessId(transactionId);
                clientSaveDTO.setRouterPath("/notifications"); // 跳转至 C 端消息中心

                notificationService.publishNotification(clientSaveDTO);
                log.info("交易创建通知已下发给买方客户 appUserId: {}", appUserId);
            } else {
                log.debug("交易关联的客户未绑定 C 端账号，跳过客户通知发送。交易ID: {}", transactionId);
            }
        } catch (Exception e) {
            log.error("下发交易创建通知给买方客户失败，交易ID: {}", transactionId, e);
        }
    }

    private Long getAppUserIdByTransactionId(Integer transactionId) {
        if (transactionId == null) return null;
        Transaction tx = transactionMapper.selectById(transactionId);
        if (tx == null || tx.getCustomerId() == null) return null;
        Customer customer = customerMapper.selectById(tx.getCustomerId());
        return customer != null ? customer.getAppUserId() : null;
    }
}
