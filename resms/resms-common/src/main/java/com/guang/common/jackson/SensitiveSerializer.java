package com.guang.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.guang.common.annotation.Sensitive;
import com.guang.common.enums.SensitiveType;

import java.io.IOException;

/**
 * 脱敏 Jackson 序列化器
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>实现 {@link ContextualSerializer}，在 Jackson 构建序列化上下文时读取字段上的
 *       {@link Sensitive} 注解，获取 {@link SensitiveType}。</li>
 *   <li>在 {@link #serialize} 方法中调用对应枚举的 {@link SensitiveType#apply} 完成脱敏。</li>
 *   <li>原始值仅存在于 JVM 内存中，不会写入任何持久化媒介，满足"数据库明文、接口脱敏"要求。</li>
 * </ol>
 *
 * @author blackDuck
 * @since 2026-05-11
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    /**
     * 当前字段的脱敏类型，由 {@link #createContextual} 注入。
     * 默认 null 表示未配置注解（透传原始值）。
     */
    private SensitiveType type;

    /**
     * 无参构造：Jackson 反射实例化时需要
     */
    public SensitiveSerializer() {
    }

    private SensitiveSerializer(SensitiveType type) {
        this.type = type;
    }

    /**
     * 读取字段上的 {@link Sensitive} 注解，返回绑定了 type 的具体序列化器实例。
     * Jackson 每个字段只调用一次，结果被缓存，性能无损。
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Sensitive annotation = property.getAnnotation(Sensitive.class);
            if (annotation == null) {
                annotation = property.getContextAnnotation(Sensitive.class);
            }
            if (annotation != null) {
                return new SensitiveSerializer(annotation.value());
            }
        }
        // 没有注解：原样输出
        return new SensitiveSerializer(null);
    }

    /**
     * 序列化时执行脱敏。
     * null 值直接写 null，不做额外处理（符合 JSON 规范）。
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        // 未配置 type（注解缺失）：透传原始值
        if (type == null) {
            gen.writeString(value);
            return;
        }
        gen.writeString(type.apply(value));
    }
}
