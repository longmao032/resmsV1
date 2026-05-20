package com.guang.trade.service;

import com.guang.trade.domain.dto.LoanApplyDTO;
import com.guang.trade.domain.dto.LoanAuditDTO;
import com.guang.trade.entity.LoanRecord;

public interface LoanService {

    LoanRecord getByTransactionId(Integer transactionId);

    LoanRecord apply(LoanApplyDTO applyDTO);

    LoanRecord audit(LoanAuditDTO auditDTO);
}
