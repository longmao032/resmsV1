package com.guang.trade.controller.system;

import com.guang.common.annotation.Log;
import com.guang.common.result.CommonResult;
import com.guang.trade.domain.dto.TransferDocumentDTO;
import com.guang.trade.domain.dto.TransferSaveDTO;
import com.guang.trade.domain.vo.TransferRecordVO;
import com.guang.trade.entity.TransferDocument;
import com.guang.trade.entity.TransferRecord;
import com.guang.trade.mapper.TransferDocumentMapper;
import com.guang.trade.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/trade/v1/transfers")
@Tag(name = "过户管理")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final TransferDocumentMapper transferDocumentMapper;

    @Operation(summary = "查询过户信息")
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAuthority('trade:transfer:query')")
    public CommonResult<TransferRecordVO> getInfo(@PathVariable Integer transactionId) {
        return CommonResult.success(transferService.getVOByTransactionId(transactionId));
    }

    @Operation(summary = "创建过户记录（填写过户信息）")
    @PostMapping
    @PreAuthorize("hasAuthority('trade:transfer:add')")
    @Log(title = "过户管理", businessType = "TRANSFER", operatorType = "SAVE")
    public CommonResult<TransferRecord> create(@Validated @RequestBody TransferSaveDTO saveDTO) {
        return CommonResult.success(transferService.create(saveDTO));
    }

    @Operation(summary = "编辑过户信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('trade:transfer:add')")
    @Log(title = "过户管理", businessType = "TRANSFER", operatorType = "UPDATE")
    public CommonResult<TransferRecord> update(@PathVariable Integer id,
                                               @Validated @RequestBody TransferSaveDTO saveDTO) {
        return CommonResult.success(transferService.update(id, saveDTO));
    }

    @Operation(summary = "确认过户完成")
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('trade:transfer:complete')")
    @Log(title = "过户管理", businessType = "TRANSFER", operatorType = "AUDIT")
    public CommonResult<TransferRecord> complete(@PathVariable Integer id) {
        return CommonResult.success(transferService.completeTransfer(id));
    }

    @Operation(summary = "上传过户文件")
    @PostMapping("/document")
    @PreAuthorize("hasAuthority('trade:transfer:upload')")
    @Log(title = "过户管理", businessType = "TRANSFER", operatorType = "SAVE", description = "上传过户文件")
    public CommonResult<Void> addDocument(@Validated @RequestBody TransferDocumentDTO docDTO) {
        transferService.addDocument(docDTO);
        return CommonResult.success(null);
    }

    @Operation(summary = "查询过户文件列表")
    @GetMapping("/{transferId}/documents")
    @PreAuthorize("hasAuthority('trade:transfer:query')")
    public CommonResult<List<TransferDocument>> listDocuments(@PathVariable Integer transferId) {
        return CommonResult.success(transferDocumentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TransferDocument>()
                        .eq(TransferDocument::getTransferId, transferId)));
    }
}
