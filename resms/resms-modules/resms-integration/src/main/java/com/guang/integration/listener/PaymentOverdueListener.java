package com.guang.integration.listener;

import com.guang.common.event.PaymentOverdueEvent;
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
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * 财务触发器：当账单逾期时，自动发送通知给销售人员与付款客户
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOverdueListener {

    private final NotificationService notificationService;
    private final TransactionMapper transactionMapper;
    private final CustomerMapper customerMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Async
    @EventListener
    public void onPaymentOverdue(PaymentOverdueEvent event) {
        if (event == null) {
            log.warn("账单逾期事件为 null，跳过处理");
            return;
        }
        log.info("监听到账单逾期事件，开始生成通知。交易单号: {}, 款项: {}", event.getTransactionNo(), event.getPayName());

        Integer transactionId = event.getTransactionId();
        Integer salesId = event.getSalesId();
        if (transactionId == null || salesId == null) {
            log.warn("账单逾期事件核心数据缺失，transactionId: {}, salesId: {}", transactionId, salesId);
            return;
        }

        // 1. 获取模版定义
        NotificationTemplate template = NotificationTemplate.BILL_OVERDUE;

        // 2. 填充模版内容
        // 参数：{0}=交易编号, {1}=款项名称, {2}=逾期金额, {3}=截止日期
        String transactionNo = event.getTransactionNo() != null ? event.getTransactionNo() : "未知单号";
        String payName = event.getPayName() != null ? event.getPayName() : "未知款项";
        String amountStr = event.getReceivableAmount() != null ? event.getReceivableAmount().toString() : "0";
        String dueDateStr = event.getDueDate() != null ? event.getDueDate().format(DATE_FORMATTER) : "未知日期";

        String content = MessageFormat.format(template.getContentPattern(), 
                transactionNo, 
                payName, 
                amountStr,
                dueDateStr);

        // 3. 构造通知发送 DTO
        NotificationSaveDTO saveDTO = new NotificationSaveDTO();
        saveDTO.setTitle(template.getTitle());
        saveDTO.setContent(content);
        saveDTO.setNoticeType(template.getType());
        saveDTO.setReceiverType((byte) 1); // 指定用户
        saveDTO.setReceiverIds(Collections.singletonList(salesId));
        saveDTO.setPriority((byte) 3); // 紧急
        saveDTO.setBusinessType("PAYMENT_OVERDUE");
        saveDTO.setBusinessId(transactionId);
        saveDTO.setRouterPath("/trade/transaction/detail?id=" + transactionId);

        // 4. 调用服务发布给销售
        notificationService.publishNotification(saveDTO);
        log.info("账单逾期通知已下发给销售人员 ID: {}", salesId);

        // 5. 获取 C 端买方客户并推送
        try {
            Long appUserId = getAppUserIdByTransactionId(transactionId);
            if (appUserId != null) {
                NotificationTemplate clientTemplate = NotificationTemplate.CLIENT_BILL_OVERDUE;
                String clientContent = MessageFormat.format(clientTemplate.getContentPattern(),
                        transactionNo,
                        payName,
                        amountStr,
                        dueDateStr);

                NotificationSaveDTO clientSaveDTO = new NotificationSaveDTO();
                clientSaveDTO.setTitle(clientTemplate.getTitle());
                clientSaveDTO.setContent(clientContent);
                clientSaveDTO.setNoticeType(clientTemplate.getType());
                clientSaveDTO.setReceiverType((byte) 1);
                clientSaveDTO.setReceiverIds(Collections.singletonList(appUserId.intValue()));
                clientSaveDTO.setPriority((byte) 3);
                clientSaveDTO.setBusinessType("PAYMENT_OVERDUE");
                clientSaveDTO.setBusinessId(transactionId);
                clientSaveDTO.setRouterPath("/notifications"); // 跳转至 C 端消息中心

                notificationService.publishNotification(clientSaveDTO);
                log.info("账单逾期通知已下发给买方客户 appUserId: {}", appUserId);
            } else {
                log.debug("交易关联的客户未绑定 C 端账号，跳过客户通知发送。交易ID: {}", transactionId);
            }
        } catch (Exception e) {
            log.error("下发账单逾期通知给买方客户失败，交易ID: {}", transactionId, e);
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
