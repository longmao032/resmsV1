package com.guang.resms.controller;

import cn.hutool.core.io.FileUtil;
import com.guang.common.config.FileProperties;
import com.guang.common.enums.FileCategory;
import com.guang.common.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载控制器
 *
 * @author blackDuck
 */
@Tag(name = "通用接口", description = "文件下载等通用接口")
@RestController
@RequestMapping("/api/v1/common/download")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileProperties fileProperties;

    @Operation(summary = "下载私有文件 (如合同)")
    @GetMapping("/private")
    public void downloadPrivate(@RequestParam String path, HttpServletResponse response) throws IOException {
        download(fileProperties.getPrivatePath() + "/" + path, response);
    }

    @Operation(summary = "下载临时文件 (如报表)")
    @GetMapping("/temp")
    public void downloadTemp(@RequestParam String path, HttpServletResponse response) throws IOException {
        download(fileProperties.getTempPath() + "/" + path, response);
    }

    private void download(String absolutePath, HttpServletResponse response) throws IOException {
        File file = new File(absolutePath);
        if (!file.exists()) {
            throw new ApiException("文件不存在");
        }

        String fileName = file.getName();
        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        FileUtil.writeToStream(file, response.getOutputStream());
    }
}
