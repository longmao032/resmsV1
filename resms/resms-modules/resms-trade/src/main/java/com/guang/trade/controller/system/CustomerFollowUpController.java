package com.guang.trade.controller.system;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.common.util.SecurityUtils;
import com.guang.trade.domain.dto.AppointmentCancelDTO;
import com.guang.trade.domain.dto.AppointmentCompleteDTO;
import com.guang.trade.domain.dto.AppointmentCreateDTO;
import com.guang.trade.domain.dto.FollowUpSaveDTO;
import com.guang.trade.domain.vo.FollowUpVO;
import com.guang.trade.service.CustomerFollowUpRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/trade/v1")
@Tag(name = "客户跟进与预约管理")
@RequiredArgsConstructor
public class CustomerFollowUpController {

    private final CustomerFollowUpRecordService followUpService;

    // ========== 跟进记录 ==========

    @Operation(summary = "查询客户跟进记录列表")
    @GetMapping("/customers/{customerId}/follow-ups")
    @PreAuthorize("hasAnyAuthority('trade:customer:followup', 'trade:customer:query')")
    public CommonResult<List<FollowUpVO>> listFollowUps(@PathVariable Integer customerId) {
        return CommonResult.success(followUpService.listFollowUpByCustomerId(customerId));
    }

    @Operation(summary = "保存跟进记录")
    @PostMapping("/customers/{customerId}/follow-ups")
    @PreAuthorize("hasAnyAuthority('trade:customer:followup', 'trade:customer:query')")
    @Log(title = "客户跟进", businessType = "CUSTOMER", operatorType = "SAVE")
    public CommonResult<Boolean> saveFollowUp(@PathVariable Integer customerId,
                                               @Validated @RequestBody FollowUpSaveDTO saveDTO) {
        saveDTO.setCustomerId(customerId);
        if (saveDTO.getSalesId() == null) {
            saveDTO.setSalesId(SecurityUtils.getUserId());
        }
        return CommonResult.success(followUpService.saveFollowUp(saveDTO));
    }

    @Operation(summary = "更新跟进记录")
    @PutMapping("/follow-ups/{id}")
    @PreAuthorize("hasAnyAuthority('trade:customer:followup', 'trade:customer:query')")
    @Log(title = "客户跟进", businessType = "CUSTOMER", operatorType = "UPDATE")
    public CommonResult<Boolean> updateFollowUp(@PathVariable Integer id,
                                                 @Validated @RequestBody FollowUpSaveDTO saveDTO) {
        return CommonResult.success(followUpService.updateFollowUp(id, saveDTO));
    }

    // ========== 预约带看 ==========

    @Operation(summary = "查询客户预约列表（status=1 且未过期）")
    @GetMapping("/customers/{customerId}/appointments")
    @PreAuthorize("hasAnyAuthority('trade:customer:appointment', 'trade:customer:query')")
    public CommonResult<List<FollowUpVO>> listAppointments(@PathVariable Integer customerId) {
        return CommonResult.success(followUpService.listAppointments(customerId));
    }

    @Operation(summary = "创建预约带看")
    @PostMapping("/customers/{customerId}/appointments")
    @PreAuthorize("hasAnyAuthority('trade:customer:appointment', 'trade:customer:query')")
    @Log(title = "预约管理", businessType = "CUSTOMER", operatorType = "APPOINTMENT")
    public CommonResult<Boolean> createAppointment(@PathVariable Integer customerId,
                                                    @Valid @RequestBody AppointmentCreateDTO dto) {
        Integer salesId = SecurityUtils.getUserId();
        return CommonResult.success(followUpService.createAppointment(
                customerId, salesId, dto.getViewTime(),
                dto.getHouseId(), dto.getCustomerFeedback(), dto.getNewIntentionLevel()));
    }

    @Operation(summary = "确认完成预约")
    @PutMapping("/appointments/{id}/complete")
    @PreAuthorize("hasAnyAuthority('trade:customer:appointment', 'trade:customer:query')")
    @Log(title = "预约管理", businessType = "CUSTOMER", operatorType = "COMPLETE")
    public CommonResult<Boolean> completeAppointment(@PathVariable Integer id,
                                                      @RequestBody AppointmentCompleteDTO dto) {
        return CommonResult.success(followUpService.completeAppointment(
                id, dto.getCustomerFeedback(), dto.getFollowAdvice(), dto.getNewIntentionLevel()));
    }

    @Operation(summary = "取消预约")
    @PutMapping("/appointments/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('trade:customer:appointment', 'trade:customer:query')")
    @Log(title = "预约管理", businessType = "CUSTOMER", operatorType = "CANCEL")
    public CommonResult<Boolean> cancelAppointment(@PathVariable Integer id,
                                                    @Valid @RequestBody AppointmentCancelDTO dto) {
        return CommonResult.success(followUpService.cancelAppointment(id, dto.getCancelReason()));
    }
}
