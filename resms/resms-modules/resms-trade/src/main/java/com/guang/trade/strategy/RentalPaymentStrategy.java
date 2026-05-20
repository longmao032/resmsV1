package com.guang.trade.strategy;

import com.guang.common.event.HouseStatusChangeEvent;
import com.guang.common.event.TransactionCompletedEvent;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租房策略 — 模式 4
 * <pre>
 *   定金 → 押金＋租金付清 → 直接完成（无需过户）
 *   状态流转：0 → 1 → 4
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class RentalPaymentStrategy implements OrderTriggerStrategy {

    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Integer getPaymentMode() {
        return 4;
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

        // 全部结清 → 租房直接完成(4)，无需过户
        if (allSettled) {
            transaction.setStatus((byte) 4);
            transaction.setUpdateTime(LocalDateTime.now());
            transactionMapper.updateById(transaction);
            // 房源状态：已预订 → 已成交
            eventPublisher.publishEvent(new HouseStatusChangeEvent(
                    this, transaction.getHouseId(), "房源", (byte) 3, (byte) 2,
                    "租房交易已完成，系统自动转为成交", "System"));
            eventPublisher.publishEvent(new TransactionCompletedEvent(
                    this, transaction.getId(), transaction.getHouseId(),
                    transaction.getSalesId(), "房源" + transaction.getHouseId(),
                    transaction.getDealPrice()));
        }
    }
}
