package com.guang.integration.controller.common;

import com.guang.common.annotation.Log;
import com.guang.common.enums.FileCategory;
import com.guang.common.result.CommonResult;
import com.guang.integration.service.common.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用文件上传控制器 (由 framework 迁移至 integration)
 *
 * @author blackDuck
 */
@Tag(name = "通用接口", description = "文件上传等通用接口")
@RestController
@RequestMapping("/api/v1/common")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "通用文件上传")
    @PostMapping("/upload")
    @Log(title = "通用文件", businessType = "FILE", operatorType = "SAVE")
    public CommonResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "category", defaultValue = "HOUSE") FileCategory category) {
        String url = fileUploadService.upload(file, category);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return CommonResult.success(result);
    }

    @Operation(summary = "私有文件下载/预览")
    @GetMapping("/download")
    public void download(@RequestParam("path") String path, HttpServletResponse response) {
        fileUploadService.downloadPrivateFile(path, response);
    }
}
