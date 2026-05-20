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
 * 按揭贷款策略 — 模式 3
 * <pre>
 *   定金 → 首付款 → 银行贷款 → 全部结清 → 创建过户记录
 *   状态流转：0 → 1 → 2 → 3
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class MortgagePaymentStrategy implements OrderTriggerStrategy {

    private final TransactionMapper transactionMapper;
    private final TransferService transferService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Integer getPaymentMode() {
        return 3;
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

        // 首付款核销：已付定金(1) → 已付首付(2)
        if (payType == 2 && transaction.getStatus() == 1) {
            transaction.setStatus((byte) 2);
            transaction.setDownPaymentTime(LocalDateTime.now());
            transaction.setUpdateTime(LocalDateTime.now());
            transactionMapper.updateById(transaction);
        }

        // 全部结清（含银行放款）→ 已具备过户条件，自动创建过户记录
        if (allSettled && transaction.getStatus() == 2) {
            transaction.setStatus((byte) 3);
            transaction.setUpdateTime(LocalDateTime.now());
            transactionMapper.updateById(transaction);
            TransferSaveDTO dto = new TransferSaveDTO();
            dto.setTransactionId(transaction.getId());
            dto.setRemark("系统自动创建：按揭贷款全部结清");
            transferService.create(dto);
        }
    }
}
