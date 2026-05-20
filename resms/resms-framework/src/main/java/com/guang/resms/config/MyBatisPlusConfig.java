package com.guang.resms.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis-Plus 全局配置
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.guang.**.mapper") // 扫描所有模块下的 mapper 接口
public class MyBatisPlusConfig {

    /**
     * 配置拦截器插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 0. 数据权限过滤隔离插件 (必须在分页插件之前注册)
        interceptor.addInnerInterceptor(new com.guang.common.security.DataScopeInterceptor());
        
        // 1. 分页插件 (必须指定数据库类型，此处为 MySQL)
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 2. 乐观锁插件 (对应实体类字段需加 @Version 注解)
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
}
