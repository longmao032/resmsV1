package com.guang.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.event.TransactionCompletedEvent;
import com.guang.finance.entity.Commission;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 佣金记录表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface CommissionService extends IService<Commission> {

    /**
     * 根据交易完成事件自动计算并保存佣金
     */
    void calculateAndSave(TransactionCompletedEvent event);

    /**
     * 分页查询佣金记录
     */
    Page<Commission> pageCommissions(Integer pageNum, Integer pageSize, Integer salesId, Byte status);

    /**
     * 确认核算佣金（0→1）
     */
    Boolean calculateCommission(Integer id);

    /**
     * 确认发放佣金
     */
    Boolean issueCommission(Integer id);
}
