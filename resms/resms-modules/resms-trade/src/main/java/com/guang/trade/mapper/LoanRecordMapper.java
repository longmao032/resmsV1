package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.trade.entity.LoanRecord;
import org.apache.ibatis.annotations.Select;

public interface LoanRecordMapper extends BaseMapper<LoanRecord> {

    @Select("SELECT * FROM tb_loan_record WHERE transaction_id = #{transactionId} ORDER BY id DESC LIMIT 1")
    LoanRecord selectByTransactionId(Integer transactionId);
}
