package com.guang.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.guang.common.enums.SensitiveType;
import com.guang.common.jackson.SensitiveSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据脱敏注解
 * <p>标注在 VO 的字段上，Jackson 序列化时自动将明文替换为脱敏文本。</p>
 *
 * <p>示例：</p>
 * <pre>
 *   {@literal @}Sensitive(SensitiveType.ID_CARD)
 *   private String idCard;
 *
 *   {@literal @}Sensitive(SensitiveType.PHONE)
 *   private String phone;
 * </pre>
 *
 * @author blackDuck
 * @since 2026-05-11
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /**
     * 脱敏类型
     */
    SensitiveType value();
}
