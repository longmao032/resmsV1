package com.guang.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件配置属性
 *
 * @author blackDuck
 */
@Data
@Component
@ConfigurationProperties(prefix = "resms.file")
public class FileProperties {

    /**
     * 公共资源存储路径
     */
    private String path;

    /**
     * 资源访问前缀
     */
    private String prefix;

    /**
     * 私有资源存储路径
     */
    private String privatePath;

    /**
     * 临时文件存储路径
     */
    private String tempPath;
}
