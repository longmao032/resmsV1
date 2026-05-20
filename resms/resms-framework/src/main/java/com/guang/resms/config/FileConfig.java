package com.guang.resms.config;

import com.guang.common.config.FileProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文件配置类
 *
 * @author blackDuck
 */
@Configuration
@RequiredArgsConstructor
public class FileConfig implements WebMvcConfigurer {

    private final FileProperties fileProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /** 本地文件上传路径 */
        String path = new java.io.File(fileProperties.getPath()).getAbsolutePath();
        registry.addResourceHandler(fileProperties.getPrefix() + "/**")
                .addResourceLocations("file:" + path + "/");
    }
}
