package com.guang.resms.task;

import cn.hutool.core.io.FileUtil;
import com.guang.common.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;

/**
 * 文件清理定时任务
 *
 * @author blackDuck
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanTask {

    private final FileProperties fileProperties;

    /**
     * 每天凌晨 1 点执行，清理 24 小时前的临时导出文件
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanTempFiles() {
        log.info("开始执行临时文件清理任务...");
        String tempPath = fileProperties.getTempPath();
        File tempDir = new File(tempPath);
        
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            return;
        }

        // 获取当前时间 24 小时前的时间戳
        long expireTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        
        cleanRecursive(tempDir, expireTime);
        log.info("临时文件清理任务执行完毕。");
    }

    private void cleanRecursive(File dir, long expireTime) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                cleanRecursive(file, expireTime);
                // 如果文件夹为空，也可以删除
                if (file.listFiles() == null || file.listFiles().length == 0) {
                    file.delete();
                }
            } else {
                if (file.lastModified() < expireTime) {
                    log.info("删除过期临时文件: {}", file.getAbsolutePath());
                    file.delete();
                }
            }
        }
    }
}
