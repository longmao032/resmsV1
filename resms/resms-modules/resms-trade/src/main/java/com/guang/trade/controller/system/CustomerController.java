package com.guang.trade.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.CustomerQueryDTO;
import com.guang.trade.domain.dto.CustomerSaveDTO;
import com.guang.trade.domain.vo.CustomerVO;
import com.guang.trade.entity.Customer;
import com.guang.trade.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 客户信息表 前端控制器
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@RestController
@RequestMapping("/api/system/trade/v1/customers")
@Tag(name = "客户管理")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户列表 (VO/脱敏)")
    @GetMapping
    @PreAuthorize("hasAuthority('trade:customer:query')")
    public CommonResult<Page<CustomerVO>> list(CustomerQueryDTO queryDTO) {
        return CommonResult.success(customerService.pageCustomers(queryDTO));
    }

    @Operation(summary = "保存/修改客户")
    @PostMapping
    @PreAuthorize("hasAuthority('trade:customer:save')")
    @Log(title = "客户管理", businessType = "CUSTOMER", operatorType = "SAVE")
    public CommonResult<Boolean> save(@Validated @RequestBody CustomerSaveDTO saveDTO) {
        return CommonResult.success(customerService.saveCustomer(saveDTO));
    }

    @Operation(summary = "查看客户详情（脱敏）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('trade:customer:query')")
    public CommonResult<CustomerVO> getInfo(@PathVariable Integer id) {
        Customer customer = customerService.getById(id);
        if (customer == null) {
            return CommonResult.failed("客户不存在");
        }
        // 通过 CustomerVO 返回，触发 @Sensitive 脱敏注解
        CustomerVO vo = new CustomerVO();
        org.springframework.beans.BeanUtils.copyProperties(customer, vo);
        return CommonResult.success(vo);
    }

    @Operation(summary = "获取明文身份证号（需要额外权限）")
    @GetMapping("/{id}/id-card")
    @PreAuthorize("hasAuthority('trade:customer:view-idcard')")
    public CommonResult<String> getRealIdCard(@PathVariable Integer id) {
        Customer customer = customerService.getById(id);
        return customer != null
                ? CommonResult.success(customer.getIdCard())
                : CommonResult.failed("客户不存在");
    }

    @Operation(summary = "获取明文手机号")
    @GetMapping("/{id}/phone")
    @PreAuthorize("hasAuthority('trade:customer:view-phone')")
    public CommonResult<String> getPhone(@PathVariable Integer id) {
        return CommonResult.success(customerService.getRealPhone(id));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('trade:customer:delete')")
    @Log(title = "客户管理", businessType = "CUSTOMER", operatorType = "DELETE")
    public CommonResult<Boolean> delete(@PathVariable Integer id) {
        return CommonResult.success(customerService.removeById(id));
    }

    @Operation(summary = "领取客户（从公海池分配到当前用户）")
    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('trade:customer:claim')")
    @Log(title = "客户管理", businessType = "CUSTOMER", operatorType = "CLAIM")
    public CommonResult<Boolean> claim(@PathVariable Integer id) {
        Integer currentUserId = com.guang.common.util.SecurityUtils.getUserId();
        if (currentUserId == null) {
            return CommonResult.failed("无法获取当前用户信息");
        }
        return CommonResult.success(customerService.assignCustomer(id, currentUserId));
    }
}
