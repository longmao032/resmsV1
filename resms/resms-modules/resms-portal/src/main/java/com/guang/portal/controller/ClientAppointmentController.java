package com.guang.portal.controller;
 
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.result.CommonResult;
import com.guang.portal.domain.dto.AppointmentSaveDTO;
import com.guang.portal.service.ClientAppointmentService;
import com.guang.trade.domain.vo.FollowUpVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
 
@Tag(name = "C端预约看房")
@RestController
@RequestMapping("/api/portal/v1/user/appointments")
@RequiredArgsConstructor
public class ClientAppointmentController {
 
    private final ClientAppointmentService appointmentService;
 
    @Operation(summary = "提交预约看房")
    @PostMapping
    public CommonResult<Void> create(@Valid @RequestBody AppointmentSaveDTO dto) {
        appointmentService.createAppointment(dto);
        return CommonResult.success(null);
    }
 
    @Operation(summary = "分页获取我的预约")
    @GetMapping
    public CommonResult<Page<FollowUpVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(appointmentService.pageMyAppointments(pageNum, pageSize));
    }
 
    @Operation(summary = "取消预约")
    @DeleteMapping("/{id}")
    public CommonResult<Void> cancel(@PathVariable Integer id) {
        appointmentService.cancelAppointment(id);
        return CommonResult.success(null);
    }
}
