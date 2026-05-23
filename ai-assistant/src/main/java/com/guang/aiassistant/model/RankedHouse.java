package com.guang.aiassistant.model;

import org.springframework.ai.document.Document;

/**
 * 带评分的房源候选 — 二次排序后的中间产物。
 * 各分项得分归一化至 [0, 1]，compositeScore 为加权总分。
 */
public record RankedHouse(
        Document document,
        double tagScore,
        double personaScore,
        double semanticScore,
        double priceScore,
        double locationScore,
        double compositeScore
) {}
