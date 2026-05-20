package com.guang.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 *
 * @author blackDuck
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表的别名
     */
    String deptAlias() default "";

    /**
     * 用户表的别名
     */
    String userAlias() default "";

    /**
     * 部门字段的名称 (在 SQL 对应的表中的列名)
     */
    String deptField() default "dept_id";

    /**
     * 用户字段的名称 (在 SQL 对应的表中的列名)
     */
    String userField() default "create_user_id";

    /**
     * 是否通过关联系统用户表的部门（sys_user.dept_id）进行部门隔离。
     * 适用于业务表自身没有 dept_id 字段，只有负责人字段（如 sales_id / create_user_id）的场景。
     */
    boolean joinUserDept() default false;

    /**
     * 在按负责人隔离时，是否包含负责人为 NULL 的记录。
     * 适用于如客户公海池等无主数据的全局共享与领取场景。
     */
    boolean includeNullUser() default false;
}
