package com.guang.common.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;

/**
 * 文件工具类
 *
 * @author blackDuck
 */
public class ResmsFileUtil {

    /**
     * 获取文件扩展名
     */
    public static String getExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return fileName.substring(index + 1);
    }

    /**
     * 生成唯一文件名: UUID + 扩展名
     */
    public static String generateUniqueName(String originalName) {
        String ext = getExtension(originalName);
        return IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(ext) ? "." + ext : "");
    }

    /**
     * 确保目录存在
     */
    public static void mkdir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
