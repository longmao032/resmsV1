package com.guang.finance.task;

import com.guang.common.event.PaymentOverdueEvent;
import com.guang.trade.domain.vo.PaymentOverdueVO;
import com.guang.trade.mapper.PaymentPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 账单逾期检测任务
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOverdueTask {

    private final PaymentPlanMapper paymentPlanMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 每天凌晨 2 点执行逾期检测
     * 实际演示建议设置为更频繁，如 "0 0/5 * * * ?" 每 5 分钟执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkOverduePayments() {
        log.info("开始执行账单逾期检测任务...");

        // 1. 查询所有逾期账单
        List<PaymentOverdueVO> overduePlans = paymentPlanMapper.selectOverduePlans();
        
        if (overduePlans == null || overduePlans.isEmpty()) {
            log.info("未发现逾期账单。");
            return;
        }

        log.info("发现 {} 条逾期账单，准备发送通知。", overduePlans.size());

        // 2. 遍历并发布事件
        for (PaymentOverdueVO vo : overduePlans) {
            try {
                eventPublisher.publishEvent(new PaymentOverdueEvent(
                        this,
                        vo.getTransactionId(),
                        vo.getTransactionNo(),
                        vo.getPayName(),
                        vo.getReceivableAmount(),
                        vo.getDueDate(),
                        vo.getSalesId()
                ));
            } catch (Exception e) {
                log.error("发布账单逾期事件失败。交易单号: {}, 错误: {}", vo.getTransactionNo(), e.getMessage());
            }
        }

        log.info("账单逾期检测任务执行完成。");
    }
}
