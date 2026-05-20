package com.guang.common.security;

import com.guang.common.annotation.DataScope;

/**
 * 数据权限过滤上下文（使用 ThreadLocal 传递参数）
 *
 * @author blackDuck
 */
public class DataScopeContext {

    private static final ThreadLocal<DataScope> CONTEXT = new ThreadLocal<>();

    public static void set(DataScope dataScope) {
        CONTEXT.set(dataScope);
    }

    public static DataScope get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
