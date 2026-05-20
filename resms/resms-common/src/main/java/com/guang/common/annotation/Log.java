package com.guang.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /**
     * 模块名称
     */
    String title() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 业务类型
     */
    String businessType() default "OTHER";

    /**
     * 操作类型
     */
    String operatorType() default "MANAGE";

    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应的数据
     */
    boolean isSaveResponseData() default true;
}
