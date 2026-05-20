package com.guang.aiassistant.service;

import com.guang.aiassistant.model.RecommendationResponse;

public record ChatResult(String sessionId, RecommendationResponse response) {}
