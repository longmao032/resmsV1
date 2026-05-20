package com.guang.common.annotation;

import java.lang.annotation.*;

/**
 * 水平越权写操作防护守卫 (BolasGuard)
 *
 * @author blackDuck
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BolasGuard {

    /**
     * 校验实体的类对象（如 House.class, Transaction.class）
     */
    Class<?> entityClass();

    /**
     * 请求中用来标识实体主键 ID 的参数名（支持从 DTO 对象属性中自动解析提取）
     */
    String idParamName() default "id";

    /**
     * 该实体中负责关联负责人/所有者的字段名称 (在 Java 实体中的属性名，默认为 salesId 或 createUserId)
     */
    String userField() default "salesId";

    /**
     * 是否需要通过用户表关联穿透部门进行隔离校验
     */
    boolean joinUserDept() default false;

    /**
     * 在校验负责人权限时，是否放行负责人为 NULL 的记录（适用于公海客户的修改/指派/领取场景）
     */
    boolean includeNullUser() default false;
}
