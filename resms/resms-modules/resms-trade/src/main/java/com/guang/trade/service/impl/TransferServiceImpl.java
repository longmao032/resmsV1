package com.guang.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.guang.common.event.HouseStatusChangeEvent;
import com.guang.common.event.TransactionCompletedEvent;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.SecurityUtils;
import com.guang.trade.domain.dto.TransferDocumentDTO;
import com.guang.trade.domain.dto.TransferSaveDTO;
import com.guang.trade.domain.vo.TransferRecordVO;
import com.guang.trade.entity.TransferDocument;
import com.guang.trade.entity.TransferRecord;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.TransactionMapper;
import com.guang.trade.mapper.TransferDocumentMapper;
import com.guang.trade.mapper.TransferRecordMapper;
import com.guang.trade.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRecordMapper transferRecordMapper;
    private final TransferDocumentMapper transferDocumentMapper;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public TransferRecord getByTransactionId(Integer transactionId) {
        return transferRecordMapper.selectByTransactionId(transactionId);
    }

    @Override
    public TransferRecordVO getVOByTransactionId(Integer transactionId) {
        return transferRecordMapper.selectVOByTransactionId(transactionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferRecord create(TransferSaveDTO saveDTO) {
        TransferRecord record = BeanUtil.copyProperties(saveDTO, TransferRecord.class);
        record.setTransferNo(CodeGeneratorUtil.generateTransferNo());
        record.setOperatorId(SecurityUtils.getUserId());
        record.setStatus((byte) 0);
        record.setCreateTime(LocalDateTime.now());
        transferRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferRecord update(Integer id, TransferSaveDTO saveDTO) {
        TransferRecord record = transferRecordMapper.selectById(id);
        if (record == null) {
            throw new ApiException("过户记录不存在");
        }
        record.setCertificateNo(saveDTO.getCertificateNo());
        record.setRegistrationCenter(saveDTO.getRegistrationCenter());
        record.setTransferDate(saveDTO.getTransferDate());
        record.setRemark(saveDTO.getRemark());
        record.setUpdateTime(LocalDateTime.now());
        transferRecordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDocument(TransferDocumentDTO docDTO) {
        if (transferRecordMapper.selectById(docDTO.getTransferId()) == null) {
            throw new ApiException("过户记录不存在");
        }
        TransferDocument doc = BeanUtil.copyProperties(docDTO, TransferDocument.class);
        doc.setUploadTime(LocalDateTime.now());
        transferDocumentMapper.insert(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferRecord completeTransfer(Integer id) {
        TransferRecord record = transferRecordMapper.selectById(id);
        if (record == null) {
            throw new ApiException("过户记录不存在");
        }
        record.setStatus((byte) 1);
        record.setUpdateTime(LocalDateTime.now());
        transferRecordMapper.updateById(record);

        // 自动推进交易状态
        Transaction transaction = transactionMapper.selectById(record.getTransactionId());
        if (transaction != null) {
            if (transaction.getStatus() == 2) {
                // 按揭流程：已付首付(2) → 已过户(3)
                transaction.setStatus((byte) 3);
                transaction.setTransferTime(record.getTransferDate() != null ? record.getTransferDate() : LocalDateTime.now());
                transaction.setUpdateTime(LocalDateTime.now());
                transactionMapper.updateById(transaction);
            } else if (transaction.getStatus() == 3) {
                // 一次性/分期流程：已具备过户条件(3) → 已完成(4)
                transaction.setStatus((byte) 4);
                transaction.setTransferTime(record.getTransferDate() != null ? record.getTransferDate() : LocalDateTime.now());
                transaction.setUpdateTime(LocalDateTime.now());
                transactionMapper.updateById(transaction);
                // 房源状态：已预订 → 已成交
                eventPublisher.publishEvent(new HouseStatusChangeEvent(
                        this, transaction.getHouseId(), "房源", (byte) 3, (byte) 2,
                        "交易已完成，系统自动转为成交", "System"));
                // 发布交易完成事件，触发佣金计算与通知
                eventPublisher.publishEvent(new TransactionCompletedEvent(
                        this, transaction.getId(), transaction.getHouseId(),
                        transaction.getSalesId(), "房源" + transaction.getHouseId(),
                        transaction.getDealPrice()));
            }
        }

        return record;
    }
}
