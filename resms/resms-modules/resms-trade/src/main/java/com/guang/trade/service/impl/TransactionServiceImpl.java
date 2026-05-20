package com.guang.trade.service.impl;

import com.guang.common.event.TransactionCompletedEvent;
import com.guang.common.event.TransactionCancelledEvent;
import com.guang.common.config.FileProperties;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.event.HouseStatusChangeEvent;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.SecurityUtils;
import com.guang.common.security.LoginUser;
import com.guang.common.annotation.DataScope;
import com.guang.common.annotation.BolasGuard;
import com.guang.trade.domain.dto.TransactionQueryDTO;
import com.guang.trade.domain.dto.TransactionSaveDTO;
import com.guang.trade.domain.dto.TransactionStatusUpdateDTO;
import com.guang.trade.domain.vo.TransactionExportVO;
import com.guang.trade.domain.vo.TransactionVO;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.TransactionMapper;
import com.guang.trade.service.TransactionService;
import com.guang.trade.entity.TransferRecord;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import com.guang.trade.entity.PaymentPlan;
import com.guang.trade.mapper.PaymentPlanMapper;
import com.guang.trade.domain.dto.PaymentPlanDTO;
import com.guang.house.service.HouseService;
import com.guang.common.util.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
/**
 * <p>
 * 交易信息表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl extends ServiceImpl<TransactionMapper, Transaction> implements TransactionService {

    private final ApplicationEventPublisher eventPublisher;
    private final FileProperties fileProperties;

    @Override
    @DataScope(userField = "sales_id", joinUserDept = true)
    public Page<TransactionVO> pageTransactions(TransactionQueryDTO queryDTO) {
        Page<TransactionVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<TransactionVO> result = (Page<TransactionVO>) baseMapper.selectTransactionPage(page, queryDTO);
        // 补全封面图 URL 前缀
        if (result.getRecords() != null) {
            result.getRecords().forEach(this::fillCoverImage);
        }
        return result;
    }

    @Override
    public TransactionVO getTransactionDetail(Integer id) {
        TransactionVO vo = baseMapper.selectTransactionDetail(id);
        fillCoverImage(vo);
        return vo;
    }

    /** 将纯相对路径的封面图补全为完整可访问 URL */
    private void fillCoverImage(TransactionVO vo) {
        if (vo == null || vo.getHouse() == null) return;
        String cover = vo.getHouse().getCoverImage();
        if (cover == null || cover.startsWith("http") || cover.startsWith("/api")) return;
        vo.getHouse().setCoverImage(fileProperties.getPrefix() + "/" + cover);
    }

    private final PaymentPlanMapper paymentPlanMapper;
    private final com.guang.trade.mapper.CustomerMapper customerMapper;
    private final com.guang.trade.mapper.TransferRecordMapper transferRecordMapper;
    private final HouseService houseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createTransaction(TransactionSaveDTO saveDTO) {
        // 房源前置校验与原子锁房：仅在房源为"在售"(1)状态时更新为"已预订"(2)
        // 使用条件更新（乐观锁）防止并发下单，若更新行数为0则状态已被其他事务变更
        boolean houseLocked = houseService.lambdaUpdate()
                .set(com.guang.house.entity.House::getStatus, (byte) 2)
                .set(com.guang.house.entity.House::getUpdateTime, LocalDateTime.now())
                .eq(com.guang.house.entity.House::getId, saveDTO.getHouseId())
                .eq(com.guang.house.entity.House::getStatus, (byte) 1)
                .eq(com.guang.house.entity.House::getIsDeleted, 0)
                .update();
        if (!houseLocked) {
            throw new ApiException("房源不在在售状态，已被预订或下架，无法创建交易");
        }

        Transaction transaction = BeanUtil.copyProperties(saveDTO, Transaction.class);
        transaction.setTransactionNo(CodeGeneratorUtil.generateTransactionNo());
        transaction.setStatus((byte) 0); // 待付定金
        transaction.setActualPaidAmount(BigDecimal.ZERO);

        // 填充冗余字段
        if (saveDTO.getCustomerId() != null) {
            com.guang.trade.entity.Customer customer = customerMapper.selectById(saveDTO.getCustomerId());
            if (customer != null) {
                transaction.setCustomerName(customer.getRealName());
                transaction.setCustomerPhone(customer.getPhone());
            }
        }

        // 指派销售权限校验
        Integer currentUserId = SecurityUtils.getUserId();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 检查是否具有指派订单销售权限，如果没有权限且尝试指派他人，则强制纠正为当前用户
        if (loginUser != null && !loginUser.getPermissions().contains("trade:order:assign")) {
            if (saveDTO.getSalesId() != null && !saveDTO.getSalesId().equals(currentUserId)) {
                transaction.setSalesId(currentUserId);
            } else if (saveDTO.getSalesId() == null) {
                transaction.setSalesId(currentUserId);
            }
        }
        
        // 1. 保存交易主表
        boolean success = this.save(transaction);
        if (!success) {
            return false;
        }

                // 2. 自动生成应收账单计划 (tb_payment_plan)
        buildPaymentPlans(transaction, saveDTO);
        // 3. 发布交易创建事件 (用于触发通知公告)
        TransactionVO detail = this.getTransactionDetail(transaction.getId());
        if (detail != null) {
            eventPublisher.publishEvent(new com.guang.common.event.TransactionCreatedEvent(
                    this, 
                    detail.getId(), 
                    detail.getSales() != null ? detail.getSales().getId() : null, 
                    detail.getHouse() != null ? detail.getHouse().getProjectName() : "未知房源", 
                    detail.getDealPrice()
            ));
        }

        return true;
    }

    /**
     * 根据付款方式自动生成应收账单计划
     */
    private void buildPaymentPlans(Transaction transaction, TransactionSaveDTO saveDTO) {
        Byte paymentType = saveDTO.getPaymentType();
        if (paymentType == null) {
            throw new ApiException("付款方式不能为空");
        }

        LocalDateTime now = LocalDateTime.now();

        if (paymentType == 1) {
            // 一次性
            PaymentPlan plan0 = createPaymentPlan(transaction.getId(), 0, "定金", transaction.getDeposit(), now);
            paymentPlanMapper.insert(plan0);

            BigDecimal rest = transaction.getDealPrice().subtract(transaction.getDeposit());
            PaymentPlan plan1 = createPaymentPlan(transaction.getId(), 1, "购房尾款", rest, now.plusDays(7));
            paymentPlanMapper.insert(plan1);

        } else if (paymentType == 2) {
            // 分期
            PaymentPlan plan0 = createPaymentPlan(transaction.getId(), 0, "定金", transaction.getDeposit(), now);
            paymentPlanMapper.insert(plan0);

            List<PaymentPlanDTO> planList = saveDTO.getPaymentPlanList();
            if (planList == null || planList.isEmpty()) {
                throw new ApiException("分期付款方式必须传入分期计划列表");
            }
            int stage = 1;
            for (PaymentPlanDTO dto : planList) {
                PaymentPlan plan = createPaymentPlan(transaction.getId(), stage++, dto.getPayName(), dto.getReceivableAmount(), dto.getDueDate());
                paymentPlanMapper.insert(plan);
            }
        } else if (paymentType == 3) {
            // 按揭
            PaymentPlan plan0 = createPaymentPlan(transaction.getId(), 0, "定金", transaction.getDeposit(), now);
            paymentPlanMapper.insert(plan0);

            if (transaction.getDownPayment() == null) {
                throw new ApiException("按揭贷款方式首付款不能为空");
            }
            BigDecimal restDown = transaction.getDownPayment().subtract(transaction.getDeposit());
            PaymentPlan plan1 = createPaymentPlan(transaction.getId(), 1, "首付款", restDown, now.plusDays(7));
            paymentPlanMapper.insert(plan1);

            if (transaction.getLoanAmount() == null) {
                throw new ApiException("按揭贷款方式贷款金额不能为空");
            }
            PaymentPlan plan2 = createPaymentPlan(transaction.getId(), 2, "银行按揭贷款", transaction.getLoanAmount(), now.plusDays(30));
            paymentPlanMapper.insert(plan2);

        } else if (paymentType == 4) {
            validateRentalTransaction(saveDTO);
            createRentalPaymentPlans(transaction.getId(), saveDTO.getPaymentPlanList());
        } else {
            throw new ApiException("未知的付款方式");
        }
    }

    private PaymentPlan createPaymentPlan(Integer txId, Integer stage, String payName, BigDecimal amount, LocalDateTime dueDate) {
        PaymentPlan plan = new PaymentPlan();
        plan.setTransactionId(txId);
        plan.setStage(stage);
        plan.setPayName(payName);
        plan.setReceivableAmount(amount != null ? amount : BigDecimal.ZERO);
        plan.setPaidAmount(BigDecimal.ZERO);
        plan.setDueDate(dueDate != null ? dueDate : LocalDateTime.now().plusDays(30));
        plan.setStatus((byte) 0); // 待付款
        return plan;
    }

    /**
     * 创建租房支付计划：押金 + 各期租金，由前端计算后传入 paymentPlanList
     */
    private void createRentalPaymentPlans(Integer txId, List<PaymentPlanDTO> planList) {
        if (planList == null || planList.isEmpty()) {
            throw new ApiException("租房模式必须传入押金和租金计划");
        }
        int stage = 0;
        for (PaymentPlanDTO dto : planList) {
            PaymentPlan plan = createPaymentPlan(txId, stage++, dto.getPayName(), dto.getReceivableAmount(), dto.getDueDate());
            paymentPlanMapper.insert(plan);
        }
    }

    /**
     * 租房模式校验：dealPrice 应与支付计划汇总一致，deposit 应为 0
     */
    private void validateRentalTransaction(TransactionSaveDTO saveDTO) {
        if (saveDTO.getDeposit() != null && saveDTO.getDeposit().compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiException("租房模式下定金金额必须为 0");
        }
        List<PaymentPlanDTO> planList = saveDTO.getPaymentPlanList();
        if (planList != null && saveDTO.getDealPrice() != null) {
            BigDecimal planSum = planList.stream()
                    .map(PaymentPlanDTO::getReceivableAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (planSum.compareTo(BigDecimal.ZERO) > 0 && planSum.compareTo(saveDTO.getDealPrice()) != 0) {
                throw new ApiException("租房成交总价与付款计划汇总不一致");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BolasGuard(entityClass = Transaction.class, userField = "salesId")
    public Boolean updateStatus(TransactionStatusUpdateDTO updateDTO) {
        // 1. 对账锁校验：如果存在待审核的支付流水，锁定交易状态，不允许修改
        int pendingCount = baseMapper.countPendingPayments(updateDTO.getId());
        if (pendingCount > 0) {
            throw new ApiException("该交易有支付流水正在财务审核中，请等待对账完成后再操作");
        }

        Transaction transaction = this.getById(updateDTO.getId());
        if (transaction == null) {
            throw new ApiException("交易记录不存在");
        }

        Byte oldStatus = transaction.getStatus();
        Byte newStatus = updateDTO.getStatus();

        if (oldStatus.equals(newStatus)) {
            return true;
        }

        // 取消订单校验
        if (newStatus == 5 && oldStatus > 0) {
            int approved = baseMapper.countApprovedPayments(updateDTO.getId());
            if (approved > 0) {
                throw new ApiException("该交易已有 " + approved + " 笔已入账的收款，不能直接取消。请先发起退款处理");
            }
        }

        // 过户前置校验：租房模式跳过，买卖模式必须有已完成过户记录
        checkTransferPrerequisite(newStatus, transaction);

        transaction.setStatus(newStatus);
        boolean success = this.updateById(transaction);
        if (!success) {
            return false;
        }

        // 联动房源状态
        TransactionVO detail = this.getTransactionDetail(transaction.getId());
        String houseName = (detail != null && detail.getHouse() != null) ? detail.getHouse().getProjectName() : "未知房源";

        if (newStatus == 1) {
            publishHouseStatusChange(transaction.getHouseId(), houseName, (byte) 2, (byte) 2, "交易已付定金，系统确认预订");
        } else if (newStatus == 4) {
            publishHouseStatusChange(transaction.getHouseId(), houseName, (byte) 3, (byte) 2, "交易已完成，系统自动转为成交");
            // 发布交易完成事件，触发佣金计算与通知
            eventPublisher.publishEvent(new TransactionCompletedEvent(
                    this, transaction.getId(), transaction.getHouseId(), transaction.getSalesId(), houseName, transaction.getDealPrice()
            ));
        } else if (newStatus == 5) {
            publishHouseStatusChange(transaction.getHouseId(), houseName, (byte) 1, null, "交易已取消，房源重新释放");
            // 发布交易取消事件，联动财务模块处理账单和流水，并触发通知
            eventPublisher.publishEvent(new TransactionCancelledEvent(
                    this, transaction.getId(), transaction.getSalesId(), houseName, "交易已取消"
            ));
            // 同步作废关联的过户记录
            voidTransferRecord(transaction.getId());
        }

        return true;
    }

    private void publishHouseStatusChange(Integer houseId, String houseName, Byte targetStatus, Byte expectedStatus, String reason) {
        eventPublisher.publishEvent(new HouseStatusChangeEvent(
                this, houseId, houseName, targetStatus, expectedStatus, reason, "System"
        ));
    }

    /**
     * 过户前置校验：租房模式跳过，买卖模式必须有已完成过户记录
     */
    private void checkTransferPrerequisite(Byte newStatus, Transaction transaction) {
        if (newStatus == 3 && transaction.getPaymentType() != null && transaction.getPaymentType() != 4) {
            TransferRecord tr = transferRecordMapper.selectByTransactionId(transaction.getId());
            if (tr == null || tr.getStatus() != 1) {
                throw new ApiException("请先完成过户信息录入并确认过户完成");
            }
            transaction.setTransferTime(tr.getTransferDate() != null ? tr.getTransferDate() : LocalDateTime.now());
        }
    }

    @Override
    public List<PaymentPlan> getPaymentPlans(Integer transactionId) {
        LambdaQueryWrapper<PaymentPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentPlan::getTransactionId, transactionId)
                    .orderByAsc(PaymentPlan::getStage);
        return paymentPlanMapper.selectList(queryWrapper);
    }

    @Override
    public Boolean isTransactionLocked(Integer transactionId) {
        return baseMapper.countPendingPayments(transactionId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BolasGuard(entityClass = Transaction.class, userField = "salesId")
    public Boolean cancelWithRefund(Integer id, String reason) {
        // 对账锁校验：存在待审核流水时阻止操作
        int pendingCount = baseMapper.countPendingPayments(id);
        if (pendingCount > 0) {
            throw new ApiException("该交易有支付流水正在财务审核中，请等待对账完成后再操作");
        }

        Transaction transaction = this.getById(id);
        if (transaction == null) throw new ApiException("交易记录不存在");
        if (transaction.getStatus() >= 3) throw new ApiException("已过户或完成的交易无法取消");

        // 为所有已入账的收款创建退款流水
        java.util.List<java.util.Map<String, Object>> payments = baseMapper.selectApprovedPayments(id);
        Integer userId = SecurityUtils.getUserId();

        // 使用交易编号后缀作为退款收据编号前缀的一部分，保持关联可追溯
        String txNo = transaction.getTransactionNo() != null ? transaction.getTransactionNo() : String.valueOf(id);

        for (java.util.Map<String, Object> p : payments) {
            baseMapper.insertRefundPayment(
                    id,
                    (Integer) p.get("payment_plan_id"),
                    ((Integer) p.get("payment_type")).byteValue(),
                    (java.math.BigDecimal) p.get("amount"),
                    (String) p.get("payment_method"),
                    (String) p.get("payer_info"),
                    "交易取消自动退款：" + (reason != null ? reason : ""),
                    CodeGeneratorUtil.generateReceiptNo(),
                    userId
            );
        }

        // 发布取消事件（联动取消账单计划 + 发送通知）
        TransactionVO detail = this.getTransactionDetail(id);
        String houseName = (detail != null && detail.getHouse() != null) ? detail.getHouse().getProjectName() : "未知房源";
        publishHouseStatusChange(transaction.getHouseId(), houseName, (byte) 1, null, "交易已取消，房源重新释放");
        eventPublisher.publishEvent(new TransactionCancelledEvent(
                this, id, transaction.getSalesId(), houseName, reason != null ? reason : "交易取消"
        ));

        // 同步作废关联的过户记录
        voidTransferRecord(id);

        return this.update().set("status", (byte) 5)
                .set("update_time", java.time.LocalDateTime.now())
                .eq("id", id)
                .update();
    }

    @Override
    @DataScope(userField = "sales_id", joinUserDept = true)
    public void exportTransactions(TransactionQueryDTO queryDTO, HttpServletResponse response) {
        List<TransactionVO> list = baseMapper.selectTransactionExportList(queryDTO);

        List<TransactionExportVO> exportList = list.stream().map(tx -> {
            TransactionExportVO vo = new TransactionExportVO();
            vo.setTransactionNo(tx.getTransactionNo());
            vo.setDealPrice(tx.getDealPrice());
            vo.setDeposit(tx.getDeposit());
            vo.setActualPaidAmount(tx.getActualPaidAmount());
            vo.setCreateTime(tx.getCreateTime());

            // 房源信息
            if (tx.getHouse() != null) {
                vo.setProjectName(tx.getHouse().getProjectName());
                vo.setHouseNo(tx.getHouse().getHouseNo());
                vo.setLayout(tx.getHouse().getLayout());
                vo.setArea(tx.getHouse().getArea());
            }

            // 客户信息
            if (tx.getCustomer() != null) {
                vo.setCustomerName(tx.getCustomer().getRealName());
                vo.setCustomerPhone(tx.getCustomer().getPhone());
            }

            // 销售信息
            if (tx.getSales() != null) {
                vo.setSalesName(tx.getSales().getRealName());
            }

            // 付款方式
            if (tx.getPaymentType() != null) {
                String typeText;
                switch (tx.getPaymentType()) {
                    case 1:  typeText = "一次性付款"; break;
                    case 2:  typeText = "分期付款"; break;
                    case 3:  typeText = "按揭贷款"; break;
                    case 4:  typeText = "租房"; break;
                    default: typeText = "未知"; break;
                }
                vo.setPaymentTypeText(typeText);
            }

            // 交易状态
            if (tx.getStatus() != null) {
                String statusText;
                if (tx.getPaymentType() != null && tx.getPaymentType() == 4) {
                    switch (tx.getStatus()) {
                        case 0:  statusText = "待付款"; break;
                        case 1:  statusText = "已付押金"; break;
                        case 4:  statusText = "已完成"; break;
                        case 5:  statusText = "已取消"; break;
                        default: statusText = "未知"; break;
                    }
                } else {
                    switch (tx.getStatus()) {
                        case 0:  statusText = "待付定金"; break;
                        case 1:  statusText = "已付定金"; break;
                        case 2:  statusText = "已付首付"; break;
                        case 3:  statusText = "已过户"; break;
                        case 4:  statusText = "已完成"; break;
                        case 5:  statusText = "已取消"; break;
                        default: statusText = "未知"; break;
                    }
                }
                vo.setStatusText(statusText);
            }

            return vo;
        }).collect(Collectors.toList());

        ExcelUtils.exportExcel(response, "交易订单_" + System.currentTimeMillis(), "交易订单", TransactionExportVO.class, exportList);
    }

    /**
     * 将关联的待过户记录标记为已作废（状态 2）
     */
    private void voidTransferRecord(Integer transactionId) {
        TransferRecord tr = transferRecordMapper.selectByTransactionId(transactionId);
        if (tr != null && tr.getStatus() == 0) {
            tr.setStatus((byte) 2);
            tr.setUpdateTime(LocalDateTime.now());
            transferRecordMapper.updateById(tr);
        }
    }
}
