package com.guang.trade.service;

import com.guang.trade.domain.dto.FeeSaveDTO;
import com.guang.trade.entity.TransactionFee;

import java.util.List;

public interface TransactionFeeService {

    List<TransactionFee> listByTransactionId(Integer transactionId);

    TransactionFee save(FeeSaveDTO saveDTO);
}
