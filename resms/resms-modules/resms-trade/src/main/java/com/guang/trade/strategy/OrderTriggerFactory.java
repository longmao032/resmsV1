package com.guang.trade.strategy;

import com.guang.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 交易支付策略工厂 — 根据付款方式自动匹配对应的策略实现。
 * <p>
 * Spring 会将所有 {@link OrderTriggerStrategy} 实现类自动注入，
 * 工厂按 {@code paymentMode} 建立索引，运行时以 O(1) 获取。
 */
@Component
public class OrderTriggerFactory {

    private final Map<Integer, OrderTriggerStrategy> strategyMap;

    public OrderTriggerFactory(List<OrderTriggerStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(OrderTriggerStrategy::getPaymentMode, Function.identity()));
    }

    /**
     * 根据付款方式获取对应的策略
     *
     * @param paymentMode 付款方式：1=一次性 2=分期 3=按揭 4=租房
     * @return 匹配的策略实现
     * @throws ApiException 当未找到匹配策略时
     */
    public OrderTriggerStrategy getStrategy(Integer paymentMode) {
        OrderTriggerStrategy strategy = strategyMap.get(paymentMode);
        if (strategy == null) {
            throw new ApiException("不支持的付款方式: " + paymentMode);
        }
        return strategy;
    }
}
