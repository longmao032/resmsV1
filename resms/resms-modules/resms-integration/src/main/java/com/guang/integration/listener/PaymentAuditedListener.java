package com.guang.integration.listener;

import com.guang.common.event.PaymentAuditedEvent;
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
 * 财务触发器：当支付流水审核完成后，自动通知销售人员与付款客户
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAuditedListener {

    private final NotificationService notificationService;
    private final TransactionMapper transactionMapper;
    private final CustomerMapper customerMapper;

    @Async
    @EventListener
    public void onPaymentAudited(PaymentAuditedEvent event) {
        if (event == null) {
            log.warn("支付核销事件为 null，跳过处理");
            return;
        }
        log.info("监听到支付核销事件。状态: {}, 交易ID: {}", event.getStatus(), event.getTransactionId());

        Integer transactionId = event.getTransactionId();
        Integer salesId = event.getSalesId();
        if (transactionId == null || salesId == null) {
            log.warn("支付核销事件核心数据缺失，transactionId: {}, salesId: {}", transactionId, salesId);
            return;
        }

        // 1. 根据状态选择模版
        NotificationTemplate template;
        String content;
        
        String payName = event.getPayName() != null ? event.getPayName() : "未知款项";
        String amountStr = event.getAmount() != null ? event.getAmount().toString() : "0";

        if (event.getStatus() == 1) { // 通过
            template = NotificationTemplate.PAYMENT_SUCCESS;
            content = MessageFormat.format(template.getContentPattern(), 
                    payName, 
                    amountStr);
        } else { // 驳回
            template = NotificationTemplate.PAYMENT_REJECT;
            content = MessageFormat.format(template.getContentPattern(), 
                    payName, 
                    event.getRemark() != null ? event.getRemark() : "无明确原因");
        }

        // 2. 构造通知发送 DTO
        NotificationSaveDTO saveDTO = new NotificationSaveDTO();
        saveDTO.setTitle(template.getTitle());
        saveDTO.setContent(content);
        saveDTO.setNoticeType(template.getType());
        saveDTO.setReceiverType((byte) 1); // 指定用户
        saveDTO.setReceiverIds(Collections.singletonList(salesId));
        saveDTO.setPriority(event.getStatus() == 1 ? (byte) 1 : (byte) 3); // 通过为正常，驳回为紧急
        saveDTO.setBusinessType("PAYMENT");
        saveDTO.setBusinessId(transactionId);
        saveDTO.setRouterPath("/trade/transaction/detail?id=" + transactionId);

        // 3. 调用服务发布给销售
        notificationService.publishNotification(saveDTO);
        log.info("支付核销通知已下发给销售人员 ID: {}", salesId);

        // 4. 获取 C 端买方客户并推送
        try {
            Long appUserId = getAppUserIdByTransactionId(transactionId);
            if (appUserId != null) {
                NotificationTemplate clientTemplate = event.getStatus() == 1
                        ? NotificationTemplate.CLIENT_PAYMENT_SUCCESS
                        : NotificationTemplate.CLIENT_PAYMENT_REJECT;

                String clientContent;
                if (event.getStatus() == 1) {
                    clientContent = MessageFormat.format(clientTemplate.getContentPattern(),
                            payName,
                            amountStr);
                } else {
                    clientContent = MessageFormat.format(clientTemplate.getContentPattern(),
                            payName,
                            event.getRemark() != null ? event.getRemark() : "无明确原因");
                }

                NotificationSaveDTO clientSaveDTO = new NotificationSaveDTO();
                clientSaveDTO.setTitle(clientTemplate.getTitle());
                clientSaveDTO.setContent(clientContent);
                clientSaveDTO.setNoticeType(clientTemplate.getType());
                clientSaveDTO.setReceiverType((byte) 1);
                clientSaveDTO.setReceiverIds(Collections.singletonList(appUserId.intValue()));
                clientSaveDTO.setPriority(event.getStatus() == 1 ? (byte) 1 : (byte) 3);
                clientSaveDTO.setBusinessType("PAYMENT");
                clientSaveDTO.setBusinessId(transactionId);
                clientSaveDTO.setRouterPath("/notifications"); // 跳转至 C 端消息中心

                notificationService.publishNotification(clientSaveDTO);
                log.info("支付核销通知已下发给买方客户 appUserId: {}", appUserId);
            } else {
                log.debug("交易关联的客户未绑定 C 端账号，跳过客户通知发送。交易ID: {}", transactionId);
            }
        } catch (Exception e) {
            log.error("下发支付核销通知给买方客户失败，交易ID: {}", transactionId, e);
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
