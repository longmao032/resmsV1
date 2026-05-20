package com.guang.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.guang.common.exception.ApiException;
import com.guang.trade.domain.dto.LoanApplyDTO;
import com.guang.trade.domain.dto.LoanAuditDTO;
import com.guang.trade.entity.LoanRecord;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.LoanRecordMapper;
import com.guang.trade.mapper.TransactionMapper;
import com.guang.trade.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRecordMapper loanRecordMapper;
    private final TransactionMapper transactionMapper;

    @Override
    public LoanRecord getByTransactionId(Integer transactionId) {
        return loanRecordMapper.selectByTransactionId(transactionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanRecord apply(LoanApplyDTO applyDTO) {
        Transaction tx = transactionMapper.selectById(applyDTO.getTransactionId());
        if (tx == null) throw new ApiException("交易记录不存在");

        LoanRecord record = BeanUtil.copyProperties(applyDTO, LoanRecord.class);
        record.setStatus((byte) 1); // 审核中
        record.setApplicationTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        loanRecordMapper.insert(record);

        // 同步交易表的贷款状态
        tx.setLoanStatus((byte) 1);
        transactionMapper.updateById(tx);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanRecord audit(LoanAuditDTO auditDTO) {
        LoanRecord record = loanRecordMapper.selectById(auditDTO.getId());
        if (record == null) throw new ApiException("贷款记录不存在");

        Byte newStatus = auditDTO.getStatus();
        record.setStatus(newStatus);
        if (auditDTO.getRemark() != null) {
            record.setRemark(auditDTO.getRemark());
        }
        record.setUpdateTime(LocalDateTime.now());

        if (newStatus == 2) { // 已放款
            record.setApprovalTime(LocalDateTime.now());
            record.setDisbursementTime(LocalDateTime.now());
        } else if (newStatus == 3) { // 未通过
            record.setApprovalTime(LocalDateTime.now());
        }
        loanRecordMapper.updateById(record);

        // 同步交易表贷款状态
        Transaction tx = transactionMapper.selectById(record.getTransactionId());
        if (tx != null) {
            tx.setLoanStatus(newStatus);
            transactionMapper.updateById(tx);
        }
        return record;
    }
}
