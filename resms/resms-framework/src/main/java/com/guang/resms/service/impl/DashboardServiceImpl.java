package com.guang.resms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guang.finance.entity.Commission;
import com.guang.finance.mapper.CommissionMapper;
import com.guang.house.entity.House;
import com.guang.house.mapper.HouseMapper;
import com.guang.resms.domain.vo.DashboardActivityVO;
import com.guang.resms.domain.vo.DashboardStatsVO;
import com.guang.resms.domain.vo.DashboardTrendVO;
import com.guang.resms.service.DashboardService;
import com.guang.system.entity.OperationLog;
import com.guang.system.mapper.OperationLogMapper;
import com.guang.trade.entity.Customer;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.CustomerMapper;
import com.guang.trade.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final HouseMapper houseMapper;
    private final TransactionMapper transactionMapper;
    private final CommissionMapper commissionMapper;
    private final CustomerMapper customerMapper;
    private final OperationLogMapper operationLogMapper;

    @Override
    public DashboardStatsVO getStats() {
        // 本月起始时间
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 新增房源：本月创建的未删除房源
        long newHouses = houseMapper.selectCount(
                new LambdaQueryWrapper<House>()
                        .eq(House::getIsDeleted, 0)
                        .ge(House::getCreateTime, monthStart)
        );

        // 本月订单：本月创建的未删除交易
        long monthlyOrders = transactionMapper.selectCount(
                new LambdaQueryWrapper<Transaction>()
                        .eq(Transaction::getIsDeleted, 0)
                        .ge(Transaction::getCreateTime, monthStart)
        );

        // 佣金总额：已核算/已发放的佣金合计
        BigDecimal totalCommission = Optional.ofNullable(
                commissionMapper.selectList(
                        new LambdaQueryWrapper<Commission>()
                                .eq(Commission::getIsDeleted, 0)
                                .in(Commission::getStatus, 1, 2)
                ).stream()
                        .map(Commission::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        ).orElse(BigDecimal.ZERO);

        // 活跃客户：未删除的客户总数
        long activeClients = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getIsDeleted, 0)
        );

        return new DashboardStatsVO(newHouses, monthlyOrders, totalCommission, activeClients);
    }

    @Override
    public List<DashboardTrendVO> getTrend(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        // 查询指定日期范围内的交易（已成交/已完成，未取消）
        List<Transaction> transactions = transactionMapper.selectList(
                new LambdaQueryWrapper<Transaction>()
                        .eq(Transaction::getIsDeleted, 0)
                        .in(Transaction::getStatus, 2, 3, 4) // 已付首付/已过户/已完成
                        .ge(Transaction::getCreateTime, startDate.atStartOfDay())
                        .lt(Transaction::getCreateTime, today.plusDays(1).atStartOfDay())
        );

        // 按日期分组汇总成交金额
        Map<LocalDate, BigDecimal> dailyMap = new HashMap<>();
        for (Transaction t : transactions) {
            LocalDate date = t.getCreateTime().toLocalDate();
            BigDecimal amount = t.getDealPrice() != null ? t.getDealPrice() : BigDecimal.ZERO;
            dailyMap.merge(date, amount, BigDecimal::add);
        }

        // 填充日期序列，无数据的日期填 0
        List<DashboardTrendVO> trendList = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM-dd"));
            BigDecimal value = dailyMap.getOrDefault(date, BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP); // 以万元为单位
            trendList.add(new DashboardTrendVO(dateStr, value));
        }

        return trendList;
    }

    @Override
    public List<DashboardActivityVO> getActivities() {
        // 获取最近10条操作日志
        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .orderByDesc(OperationLog::getOperationTime)
                        .last("LIMIT 10")
        );

        List<DashboardActivityVO> activities = new ArrayList<>();
        for (OperationLog log : logs) {
            String content = log.getOperationDesc();
            if (content == null || content.isBlank()) {
                content = log.getBusinessType() + "·" + log.getOperationType();
            }

            String user = log.getUserName() != null ? log.getUserName() : "系统";
            String timestamp = formatRelativeTime(log.getOperationTime());
            // 根据风险等级或状态判断类型颜色
            String type = mapActivityType(log);

            activities.add(new DashboardActivityVO(content, timestamp, user, type));
        }

        return activities;
    }

    /**
     * 将操作时间格式化为相对时间描述
     */
    private String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "未知";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(time, now);

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";
        if (hours < 24) return hours + "小时前";
        if (days < 7) return days + "天前";
        return time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }

    /**
     * 根据日志属性映射活动类型
     */
    private String mapActivityType(OperationLog log) {
        if (log.getStatus() != null && log.getStatus() == 0) return "danger";
        if (log.getRiskLevel() != null && log.getRiskLevel() == 1) return "warning";
        if ("SAVE".equals(log.getOperationType()) || "ADD".equals(log.getOperationType())) return "primary";
        if ("UPDATE".equals(log.getOperationType()) || "AUDIT".equals(log.getOperationType())) return "success";
        return "info";
    }
}
