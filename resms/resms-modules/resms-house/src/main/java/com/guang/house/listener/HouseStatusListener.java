package com.guang.house.listener;

import com.guang.common.event.HouseStatusChangeEvent;
import com.guang.house.service.HouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 房源状态变更事件监听器
 * 实现业务联动逻辑
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HouseStatusListener {

    private final HouseService houseService;

    /**
     * 监听房源状态变更事件
     */
    @EventListener
    public void handleHouseStatusChange(HouseStatusChangeEvent event) {
        log.info("监听到房源状态变更事件: houseId={}, targetStatus={}, expectedStatus={}", 
                event.getHouseId(), event.getStatus(), event.getExpectedStatus());
        
        try {
            houseService.updateStatus(
                    event.getHouseId(), 
                    event.getStatus(), 
                    event.getExpectedStatus(), 
                    event.getReason()
            );
            log.info("房源状态变更处理成功: houseId={}", event.getHouseId());
        } catch (Exception e) {
            log.error("房源状态变更处理失败: houseId={}, error={}", event.getHouseId(), e.getMessage());
            // 注意：如果是异步监听，异常不会抛给发布者。如果是同步监听，异常会触发发布者事务回滚。
            throw e;
        }
    }
}
