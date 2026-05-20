package com.guang.trade.strategy;

import com.guang.common.event.HouseStatusChangeEvent;
import com.guang.trade.domain.dto.TransferSaveDTO;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.TransactionMapper;
import com.guang.trade.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次性付款策略 — 模式 1
 * <pre>
 *   定金 → 尾款付清 → 创建过户记录
 *   状态流转：0 → 1 → 3
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class OneTimePaymentStrategy implements OrderTriggerStrategy {

    private final TransactionMapper transactionMapper;
    private final TransferService transferService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Integer getPaymentMode() {
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(Transaction transaction, Byte payType, BigDecimal actualAmount, boolean allSettled) {
        // 定金核销：待付定金(0) → 已付定金(1)
        if (payType == 1 && transaction.getStatus() == 0) {
            transaction.setStatus((byte) 1);
            transaction.setDepositTime(LocalDateTime.now());
            transaction.setUpdateTime(LocalDateTime.now());
            transactionMapper.updateById(transaction);
            eventPublisher.publishEvent(new HouseStatusChangeEvent(
                    this, transaction.getHouseId(), "房源", (byte) 2, (byte) 2,
                    "定金已到账，系统确认预订", "System"));
        }

        // 全部结清 → 已具备过户条件，自动创建过户记录
        if (allSettled && transaction.getStatus() < 3) {
            transaction.setStatus((byte) 3);
            transaction.setUpdateTime(LocalDateTime.now());
            transactionMapper.updateById(transaction);
            createTransferRecord(transaction);
        }
    }

    private void createTransferRecord(Transaction transaction) {
        TransferSaveDTO dto = new TransferSaveDTO();
        dto.setTransactionId(transaction.getId());
        dto.setRemark("系统自动创建：一次性付款全部结清");
        transferService.create(dto);
    }
}
