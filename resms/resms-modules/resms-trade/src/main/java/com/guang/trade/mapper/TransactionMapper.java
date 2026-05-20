package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.trade.domain.dto.TransactionQueryDTO;
import com.guang.trade.domain.vo.TransactionVO;
import com.guang.trade.entity.Transaction;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 交易信息表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface TransactionMapper extends BaseMapper<Transaction> {

    /**
     * 联表分页查询交易列表
     */
    IPage<TransactionVO> selectTransactionPage(Page<TransactionVO> page, @Param("query") TransactionQueryDTO query);

    /**
     * 导出查询交易列表（不分页）
     */
    java.util.List<TransactionVO> selectTransactionExportList(@Param("query") TransactionQueryDTO query);

    /**
     * 查询交易详情 (含关联信息)
     */
    TransactionVO selectTransactionDetail(@Param("id") Integer id);

    /**
     * 行级锁查询交易记录 (用于对账锁)
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM tb_transaction WHERE id = #{id} FOR UPDATE")
    Transaction selectByIdForUpdate(@Param("id") Integer id);

    /**
     * 查询是否存在待审核的支付流水 (跨表对账锁检查)
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM tb_payment WHERE transaction_id = #{txId} AND payment_status = 0 AND is_deleted = 0")
    int countPendingPayments(@Param("txId") Integer txId);

    /**
     * 查询是否存在已审核通过的支付流水
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM tb_payment WHERE transaction_id = #{txId} AND payment_status = 1 AND is_deleted = 0")
    int countApprovedPayments(@Param("txId") Integer txId);

    /**
     * 查询已审核通过的收款流水（用于取消时自动退款）
     */
    @org.apache.ibatis.annotations.Select("SELECT id, transaction_id, payment_plan_id, payment_type, amount, payment_method, payer_info FROM tb_payment WHERE transaction_id = #{txId} AND payment_status = 1 AND flow_type = 1 AND is_deleted = 0")
    java.util.List<java.util.Map<String, Object>> selectApprovedPayments(@Param("txId") Integer txId);

    /**
     * 插入退款流水
     */
    @org.apache.ibatis.annotations.Insert("INSERT INTO tb_payment (transaction_id, payment_plan_id, payment_type, flow_type, amount, payment_method, payer_info, remark, receipt_no, payment_status, finance_id, create_time, payment_time, is_deleted) VALUES (#{txId}, #{planId}, #{payType}, 2, #{amount}, #{method}, #{payerInfo}, #{remark}, #{receiptNo}, 0, #{userId}, NOW(), NOW(), 0)")
    int insertRefundPayment(@Param("txId") Integer txId, @Param("planId") Integer planId, @Param("payType") Byte payType, @Param("amount") java.math.BigDecimal amount, @Param("method") String method, @Param("payerInfo") String payerInfo, @Param("remark") String remark, @Param("receiptNo") String receiptNo, @Param("userId") Integer userId);
}
