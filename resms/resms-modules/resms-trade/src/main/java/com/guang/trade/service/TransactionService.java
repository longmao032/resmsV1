package com.guang.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.dto.TransactionQueryDTO;
import com.guang.trade.domain.dto.TransactionSaveDTO;
import com.guang.trade.domain.dto.TransactionStatusUpdateDTO;
import com.guang.trade.domain.vo.TransactionVO;
import com.guang.trade.entity.Transaction;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 交易信息表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface TransactionService extends IService<Transaction> {

    /**
     * 分页查询交易列表
     */
    Page<TransactionVO> pageTransactions(TransactionQueryDTO queryDTO);

    /**
     * 获取交易详情 (VO)
     */
    TransactionVO getTransactionDetail(Integer id);

    /**
     * 创建交易
     */
    Boolean createTransaction(TransactionSaveDTO saveDTO);

    /**
     * 更新交易状态 (含联动房源状态)
     */
    Boolean updateStatus(TransactionStatusUpdateDTO updateDTO);

    /**
     * 获取指定交易的账单计划列表
     */
    java.util.List<com.guang.trade.entity.PaymentPlan> getPaymentPlans(Integer transactionId);

    /**
     * 检查交易是否被对账锁锁定
     */
    Boolean isTransactionLocked(Integer transactionId);

    /**
     * 取消交易并自动发起退款（为所有已入账的收款创建退款流水）
     */
    Boolean cancelWithRefund(Integer id, String reason);

    /**
     * 导出交易订单
     */
    void exportTransactions(TransactionQueryDTO queryDTO, HttpServletResponse response);
}
