package com.guang.integration.service.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.guang.common.config.FileProperties;
import com.guang.common.exception.ApiException;
import com.guang.common.enums.FileCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件上传服务 (由 framework 迁移至 integration)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileProperties fileProperties;

    /**
     * 上传文件 (基于分类)
     *
     * @param file     文件
     * @param category 分类
     * @return 访问路径或内部标识
     */
    public String upload(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("文件不能为空");
        }

        // 1. 确定物理存储基准路径
        String basePath = category.isPublic() ? fileProperties.getPath() : fileProperties.getPrivatePath();
        if (category == FileCategory.EXPORT) {
            basePath = fileProperties.getTempPath();
        }

        // 2. 确定相对路径: categorySubDir + yyyy/MM/dd
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = category.getSubDir() + "/" + datePath;
        String absolutePath = basePath + "/" + relativePath;

        File dir = new File(absolutePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 3. 生成文件名
        String originalFilename = file.getOriginalFilename();
        String ext = FileUtil.extName(originalFilename);
        String newFilename = IdUtil.fastSimpleUUID() + "." + ext;

        // 4. 保存文件
        File dest = new File(dir, newFilename).getAbsoluteFile();
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件上传失败，目标路径: {}", dest.getAbsolutePath(), e);
            throw new ApiException("文件保存失败");
        }

        // 5. 返回可直接用于前端展示的 URL（不再让前端做路径拼接/判断）
        String filePath = relativePath + "/" + newFilename;
        if (category.isPublic()) {
            // 公共资源：/api/profile/{相对路径}
            return fileProperties.getPrefix() + "/" + filePath;
        } else {
            // 私有资源：包装为统一的下载接口路径，前端直接用
            return "/api/v1/common/download?path=" + URLEncoder.encode(filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 下载/读取私有文件流
     *
     * @param path     相对路径
     * @param response 响应对象
     */
    public void downloadPrivateFile(String path, HttpServletResponse response) {
        if (StrUtil.isBlank(path)) {
            throw new ApiException("文件路径不能为空");
        }

        // 拼接私有物理路径
        String absolutePath = fileProperties.getPrivatePath() + File.separator + path;
        File file = new File(absolutePath);
        if (!file.exists()) {
            log.error("私有文件不存在: {}", absolutePath);
            throw new ApiException("文件不存在");
        }

        try {
            // 设置响应头，支持图片预览
            String ext = FileUtil.extName(path).toLowerCase();
            String contentType = "application/octet-stream";
            if ("jpg".equals(ext) || "jpeg".equals(ext)) contentType = "image/jpeg";
            else if ("png".equals(ext)) contentType = "image/png";
            else if ("gif".equals(ext)) contentType = "image/gif";
            
            response.setContentType(contentType);
            FileUtil.writeToStream(file, response.getOutputStream());
        } catch (IOException e) {
            log.error("读取私有文件异常: {}", path, e);
            throw new ApiException("读取文件异常");
        }
    }
}
