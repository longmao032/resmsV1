package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Collections;
import java.util.List;

/**
 * LLM 推荐理由的结构化输出 — 通过 .entity() 反序列化。
 */
public record ExplanationResult(
        @JsonPropertyDescription("每个房源的推荐理由列表")
        List<HouseExplanation> explanations
) {
    public ExplanationResult {
        explanations = explanations != null ? List.copyOf(explanations) : Collections.emptyList();
    }

    public record HouseExplanation(
            @JsonPropertyDescription("房源ID") String houseId,
            @JsonPropertyDescription("推荐理由，30字以内") String reason
    ) {}
}
