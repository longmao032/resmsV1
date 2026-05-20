package com.guang.trade.service;

import com.guang.trade.domain.dto.TransferDocumentDTO;
import com.guang.trade.domain.dto.TransferSaveDTO;
import com.guang.trade.domain.vo.TransferRecordVO;
import com.guang.trade.entity.TransferRecord;

public interface TransferService {

    TransferRecord getByTransactionId(Integer transactionId);

    TransferRecordVO getVOByTransactionId(Integer transactionId);

    TransferRecord create(TransferSaveDTO saveDTO);

    TransferRecord update(Integer id, TransferSaveDTO saveDTO);

    void addDocument(TransferDocumentDTO docDTO);

    TransferRecord completeTransfer(Integer id);
}
