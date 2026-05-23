package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.guang.aiassistant.core.router.IntentType;

/**
 * 无状态意图表单 — 每一轮对话独立提取，不携带任何历史上下文。
 * LLM 的唯一职责是如实填表，所有控制流由 Java 代码接管。
 */
public record UserIntentForm(

        @JsonPropertyDescription("意图分类：CHITCHAT(闲聊/问候/感谢), POLICY(购房政策/贷款/税费咨询), SEARCH(查找/推荐/搜索房源), RESET(重置/清空记忆/重新开始)")
        IntentType intentType,

        @JsonPropertyDescription("用户当前这句话明确提到的城市名，如'深圳'、'广州'、'南宁'。如果用户这句话没有明确说出城市名，必须设为null，绝不要从对话历史推断")
        String mentionedCity,

        @JsonPropertyDescription("用户当前这句话明确提到的区县名，如'南山区'、'福田区'、'青秀区'。如果用户这句话没有明确说出区县名，必须设为null")
        String mentionedDistrict,

        @JsonPropertyDescription("用户当前这句话明确提到的地铁线或站点名，如'1号线'、'车公庙站'。如果用户这句话没有明确提到地铁，必须设为null")
        String mentionedSubway,

        @JsonPropertyDescription("用户当前这句话明确提到的楼盘或小区项目名，如'万科城'、'碧桂园'。如果用户这句话没有明确说出具体楼盘名，必须设为null")
        String mentionedProject,

        @JsonPropertyDescription("用户当前这句话明确提到的房源类型：1=新房, 2=二手房, 3=租房。如果用户这句话没有明确说出类型，必须设为null，绝不要从上下文推断")
        Integer houseType,

        @JsonPropertyDescription("用户当前这句话明确说出的最低预算数字（单位：万元），如'500万预算'则填500。如果用户没有说出具体数字，必须设为null")
        Integer minPrice,

        @JsonPropertyDescription("用户当前这句话明确说出的最高预算数字（单位：万元），如'800万以内'则填800。如果用户没有说出具体数字，必须设为null")
        Integer maxPrice,

        @JsonPropertyDescription("用户这句话中其他模糊的自然语言偏好描述，如'安静适合养狗'、'靠近学校'、'精装修'等。如果用户这句话只有明确的条件没有模糊描述，则设为null")
        String generalQuery

) {}
