package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.trade.domain.vo.TransferRecordVO;
import com.guang.trade.entity.TransferRecord;
import org.apache.ibatis.annotations.Select;

public interface TransferRecordMapper extends BaseMapper<TransferRecord> {

    @Select("SELECT * FROM tb_transfer_record WHERE transaction_id = #{transactionId} ORDER BY id DESC LIMIT 1")
    TransferRecord selectByTransactionId(Integer transactionId);

    @Select("SELECT r.*, u.real_name AS operatorName " +
            "FROM tb_transfer_record r " +
            "LEFT JOIN sys_user u ON r.operator_id = u.id " +
            "WHERE r.transaction_id = #{transactionId} ORDER BY r.id DESC LIMIT 1")
    TransferRecordVO selectVOByTransactionId(Integer transactionId);
}
