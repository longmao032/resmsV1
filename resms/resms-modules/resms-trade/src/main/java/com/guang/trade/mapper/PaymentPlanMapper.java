package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.trade.entity.PaymentPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 交易付款应收账单计划表 Mapper 接口
 * </p>
 *
 * @author antigravity
 * @since 2026-05-10
 */
import com.guang.trade.domain.vo.PaymentOverdueVO;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * <p>
 * 交易付款应收账单计划表 Mapper 接口
 * </p>
 *
 * @author antigravity
 * @since 2026-05-10
 */
@Mapper
public interface PaymentPlanMapper extends BaseMapper<PaymentPlan> {

    /**
     * 查询已逾期且未结清的账单计划
     */
    @Select("SELECT " +
            "p.transaction_id as transactionId, " +
            "t.transaction_no as transactionNo, " +
            "p.pay_name as payName, " +
            "p.receivable_amount as receivableAmount, " +
            "p.due_date as dueDate, " +
            "t.sales_id as salesId " +
            "FROM tb_payment_plan p " +
            "LEFT JOIN tb_transaction t ON p.transaction_id = t.id " +
            "WHERE p.status IN (0, 1) " + // 0=待付款, 1=部分付款
            "AND p.due_date < NOW() " +
            "AND p.is_deleted = 0 " +
            "AND t.is_deleted = 0 " +
            "AND t.status != 5") // 5=已取消
    List<PaymentOverdueVO> selectOverduePlans();
}
