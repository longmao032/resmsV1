package com.guang.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.event.TransactionCompletedEvent;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.SecurityUtils;
import com.guang.common.annotation.DataScope;
import com.guang.finance.entity.Commission;
import com.guang.finance.entity.Payment;
import com.guang.finance.mapper.CommissionMapper;
import com.guang.finance.mapper.PaymentMapper;
import com.guang.finance.service.CommissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.house.entity.House;
import com.guang.house.entity.Project;
import com.guang.house.service.HouseService;
import com.guang.house.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 佣金记录表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl extends ServiceImpl<CommissionMapper, Commission> implements CommissionService {

    private final HouseService houseService;
    private final ProjectService projectService;
    private final PaymentMapper paymentMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateAndSave(TransactionCompletedEvent event) {
        log.info("开始计算佣金: transactionId={}, dealPrice={}", event.getTransactionId(), event.getDealPrice());

        House house = houseService.getById(event.getHouseId());
        if (house == null) {
            log.warn("未找到关联房源，取消计算: houseId={}", event.getHouseId());
            return;
        }

        Project project = projectService.getById(house.getProjectId());
        if (project == null || project.getCommissionRate() == null) {
            log.warn("项目未配置佣金比例，跳过计算: projectId={}", house.getProjectId());
            return;
        }

        BigDecimal rate = project.getCommissionRate();
        BigDecimal commissionAmount = event.getDealPrice()
                .multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        Commission commission = new Commission();
        commission.setTransactionId(event.getTransactionId());
        commission.setSalesId(event.getSalesId());
        commission.setAmount(commissionAmount);
        commission.setCommissionRate(rate);
        commission.setStatus((byte) 0); // 待核算
        commission.setCreateTime(LocalDateTime.now());

        this.save(commission);
        log.info("佣金记录已生成: amount={}, salesId={}", commissionAmount, event.getSalesId());
    }

    @Override
    @DataScope(userField = "sales_id", joinUserDept = true)
    public Page<Commission> pageCommissions(Integer pageNum, Integer pageSize, Integer salesId, Byte status) {
        Page<Commission> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(salesId != null, Commission::getSalesId, salesId)
                .eq(status != null, Commission::getStatus, status)
                .orderByDesc(Commission::getCreateTime);
        Page<Commission> result = this.page(page, wrapper);
        enrichCommissions(result.getRecords());
        return result;
    }

    @Override
    public Commission getById(Serializable id) {
        Commission commission = super.getById(id);
        if (commission != null) {
            enrichCommissions(Collections.singletonList(commission));
        }
        return commission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean calculateCommission(Integer id) {
        Commission commission = super.getById(id);
        if (commission == null) {
            throw new ApiException("记录不存在");
        }
        if (commission.getStatus() != 0) {
            throw new ApiException("仅待核算的佣金可核算");
        }

        commission.setStatus((byte) 1); // 已核算
        commission.setCalculateTime(LocalDateTime.now());
        commission.setFinanceId(SecurityUtils.getUserId());
        commission.setUpdateTime(LocalDateTime.now());
        return this.updateById(commission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean issueCommission(Integer id) {
        Commission commission = super.getById(id);
        if (commission == null) {
            throw new ApiException("记录不存在");
        }
        if (commission.getStatus() != 1) {
            throw new ApiException("仅已核算的佣金可发放");
        }

        commission.setStatus((byte) 2); // 已发放
        commission.setIssueTime(LocalDateTime.now());
        commission.setUpdateTime(LocalDateTime.now());

        Payment payment = new Payment();
        payment.setTransactionId(commission.getTransactionId());
        payment.setPaymentType((byte) 6);  // 6=佣金支出
        payment.setFlowType((byte) 3);     // 3=支出
        payment.setAmount(commission.getAmount());
        payment.setPaymentStatus((byte) 1); // 直接有效
        payment.setReceiptNo(CodeGeneratorUtil.generateReceiptNo());
        payment.setFinanceId(SecurityUtils.getUserId());
        payment.setPaymentMethod("内部转账");
        payment.setPayerInfo("佣金发放-销售ID:" + commission.getSalesId());
        payment.setRemark(commission.getRemark());
        payment.setPaymentTime(LocalDateTime.now());
        payment.setCreateTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        return this.updateById(commission);
    }

    /**
     * 批量补充展示字段：销售姓名、财务姓名、交易编号
     */
    private void enrichCommissions(List<Commission> list) {
        if (list == null || list.isEmpty()) return;

        // 收集所有用户 ID
        Set<Integer> userIds = new HashSet<>();
        for (Commission c : list) {
            if (c.getSalesId() != null) userIds.add(c.getSalesId());
            if (c.getFinanceId() != null) userIds.add(c.getFinanceId());
        }

        // 批量查询用户姓名
        Map<Integer, String> userNames = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            String ids = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            userNames = jdbcTemplate.query("SELECT id, real_name FROM sys_user WHERE id IN (" + ids + ")", rs -> {
                Map<Integer, String> map = new HashMap<>();
                while (rs.next()) map.put(rs.getInt("id"), rs.getString("real_name"));
                return map;
            });
        }

        // 收集所有交易 ID
        Set<Integer> txIds = new HashSet<>();
        for (Commission c : list) {
            if (c.getTransactionId() != null) txIds.add(c.getTransactionId());
        }

        // 批量查询交易编号
        Map<Integer, String> txNos = Collections.emptyMap();
        if (!txIds.isEmpty()) {
            String ids = txIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            txNos = jdbcTemplate.query("SELECT id, transaction_no FROM tb_transaction WHERE id IN (" + ids + ")", rs -> {
                Map<Integer, String> map = new HashMap<>();
                while (rs.next()) map.put(rs.getInt("id"), rs.getString("transaction_no"));
                return map;
            });
        }

        // 填充
        for (Commission c : list) {
            c.setSalesName(userNames.getOrDefault(c.getSalesId(), null));
            c.setFinanceName(userNames.getOrDefault(c.getFinanceId(), null));
            c.setTransactionNo(txNos.getOrDefault(c.getTransactionId(), null));
        }
    }
}
