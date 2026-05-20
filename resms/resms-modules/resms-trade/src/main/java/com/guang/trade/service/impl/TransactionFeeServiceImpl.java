package com.guang.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guang.trade.domain.dto.FeeSaveDTO;
import com.guang.trade.entity.TransactionFee;
import com.guang.trade.mapper.TransactionFeeMapper;
import com.guang.trade.service.TransactionFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionFeeServiceImpl implements TransactionFeeService {

    private final TransactionFeeMapper transactionFeeMapper;

    @Override
    public List<TransactionFee> listByTransactionId(Integer transactionId) {
        LambdaQueryWrapper<TransactionFee> qw = new LambdaQueryWrapper<>();
        qw.eq(TransactionFee::getTransactionId, transactionId)
           .orderByAsc(TransactionFee::getCreateTime);
        return transactionFeeMapper.selectList(qw);
    }

    @Override
    public TransactionFee save(FeeSaveDTO saveDTO) {
        TransactionFee fee = BeanUtil.copyProperties(saveDTO, TransactionFee.class);
        fee.setCreateTime(LocalDateTime.now());
        transactionFeeMapper.insert(fee);
        return fee;
    }
}
