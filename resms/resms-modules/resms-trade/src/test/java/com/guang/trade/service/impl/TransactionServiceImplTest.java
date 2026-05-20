package com.guang.trade.service.impl;

import com.guang.trade.domain.dto.TransactionSaveDTO;
import com.guang.trade.entity.PaymentPlan;
import com.guang.trade.entity.Transaction;
import com.guang.trade.mapper.PaymentPlanMapper;
import com.guang.trade.mapper.TransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

    @Mock
    private PaymentPlanMapper paymentPlanMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    public void testCreateTransaction_OneTimePayment() {
        TransactionServiceImpl spyService = spy(transactionService);
        doReturn(true).when(spyService).save(any(Transaction.class));

        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setHouseId(1);
        dto.setCustomerId(1);
        dto.setSalesId(1);
        dto.setDealPrice(new BigDecimal("1000000"));
        dto.setDeposit(new BigDecimal("50000"));
        dto.setPaymentType((byte) 1); // 一次性

        Boolean result = spyService.createTransaction(dto);

        assertTrue(result);

        // 验证准确生成了 定金 和 购房尾款 两笔计划
        ArgumentCaptor<PaymentPlan> captor = ArgumentCaptor.forClass(PaymentPlan.class);
        verify(paymentPlanMapper, times(2)).insert(captor.capture());

        List<PaymentPlan> plans = captor.getAllValues();
        assertEquals(2, plans.size());
        
        assertEquals("定金", plans.get(0).getPayName());
        assertEquals(new BigDecimal("50000"), plans.get(0).getReceivableAmount());

        assertEquals("购房尾款", plans.get(1).getPayName());
        assertEquals(new BigDecimal("950000"), plans.get(1).getReceivableAmount());
    }

    @Test
    public void testCreateTransaction_MortgagePayment() {
        TransactionServiceImpl spyService = spy(transactionService);
        doReturn(true).when(spyService).save(any(Transaction.class));

        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setHouseId(1);
        dto.setCustomerId(1);
        dto.setSalesId(1);
        dto.setDealPrice(new BigDecimal("1000000"));
        dto.setDeposit(new BigDecimal("50000"));
        dto.setDownPayment(new BigDecimal("300000"));
        dto.setLoanAmount(new BigDecimal("700000"));
        dto.setPaymentType((byte) 3); // 按揭贷款

        Boolean result = spyService.createTransaction(dto);

        assertTrue(result);

        // 验证准确拆分了 3 笔计划
        ArgumentCaptor<PaymentPlan> captor = ArgumentCaptor.forClass(PaymentPlan.class);
        verify(paymentPlanMapper, times(3)).insert(captor.capture());

        List<PaymentPlan> plans = captor.getAllValues();
        assertEquals(3, plans.size());
        
        assertEquals("定金", plans.get(0).getPayName());
        assertEquals(new BigDecimal("50000"), plans.get(0).getReceivableAmount());

        assertEquals("首付款", plans.get(1).getPayName());
        assertEquals(new BigDecimal("250000"), plans.get(1).getReceivableAmount()); // 300000 - 50000

        assertEquals("银行按揭贷款", plans.get(2).getPayName());
        assertEquals(new BigDecimal("700000"), plans.get(2).getReceivableAmount());
    }
}
