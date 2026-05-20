package com.guang.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * 脱敏类型枚举
 * <p>每种类型内置对应的脱敏函数，保持零依赖。</p>
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Getter
@RequiredArgsConstructor
public enum SensitiveType {

    /**
     * 身份证号脱敏
     * 规则：保留前 6 位和后 4 位，中间替换为 8 个 *
     * 示例：440301199501011234 → 440301********1234
     */
    ID_CARD(raw -> {
        if (raw == null || raw.length() < 15) return raw;
        // 兼容 15 位旧身份证
        int len = raw.length();
        int keepHead = 6;
        int keepTail = 4;
        int maskLen = len - keepHead - keepTail;
        if (maskLen <= 0) return raw;
        return raw.substring(0, keepHead)
                + "*".repeat(maskLen)
                + raw.substring(len - keepTail);
    }),

    /**
     * 手机号脱敏
     * 规则：保留前 3 位和后 4 位，中间替换为 4 个 *
     * 示例：13800138000 → 138****8000
     */
    PHONE(raw -> {
        if (raw == null || raw.length() < 7) return raw;
        return raw.substring(0, 3) + "****" + raw.substring(raw.length() - 4);
    }),

    /**
     * 姓名脱敏
     * 规则：保留第一个字，其余替换为 *
     * 示例：张三丰 → 张**
     */
    NAME(raw -> {
        if (raw == null || raw.isEmpty()) return raw;
        return raw.charAt(0) + "*".repeat(raw.length() - 1);
    }),

    /**
     * 银行卡号脱敏
     * 规则：保留前 4 位和后 4 位，中间替换为 * 分隔
     * 示例：6225880172345678 → 6225 **** **** 5678
     */
    BANK_CARD(raw -> {
        if (raw == null || raw.length() < 8) return raw;
        return raw.substring(0, 4)
                + " **** **** "
                + raw.substring(raw.length() - 4);
    }),

    /**
     * 电子邮箱脱敏
     * 规则：@ 前的用户名保留首字符和末字符，中间替换为 ***
     * 示例：zhangsan@qq.com → z***n@qq.com
     */
    EMAIL(raw -> {
        if (raw == null || !raw.contains("@")) return raw;
        String[] parts = raw.split("@", 2);
        String user = parts[0];
        String domain = parts[1];
        if (user.length() <= 2) {
            return user.charAt(0) + "***@" + domain;
        }
        return user.charAt(0) + "***" + user.charAt(user.length() - 1) + "@" + domain;
    }),

    /**
     * 通用：全部替换为 *
     */
    ALL(raw -> raw == null ? null : "*".repeat(raw.length()));

    /**
     * 脱敏函数
     */
    private final Function<String, String> desensitizer;

    /**
     * 对输入字符串执行脱敏
     *
     * @param rawValue 明文原始值
     * @return 脱敏后的字符串
     */
    public String apply(String rawValue) {
        return desensitizer.apply(rawValue);
    }
}
