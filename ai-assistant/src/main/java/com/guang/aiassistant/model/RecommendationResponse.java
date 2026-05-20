package com.guang.aiassistant.model;

import java.util.Collections;
import java.util.List;

public record RecommendationResponse(
    String reply,
    List<HouseItem> recommendations,
    List<ProjectItem> projects,
    List<String> followUp
) {
    public RecommendationResponse {
        recommendations = recommendations != null ? List.copyOf(recommendations) : Collections.emptyList();
        projects = projects != null ? List.copyOf(projects) : Collections.emptyList();
        followUp = followUp != null ? List.copyOf(followUp) : Collections.emptyList();
    }

    public static RecommendationResponse of(String reply, List<HouseItem> recommendations) {
        return new RecommendationResponse(reply, recommendations, List.of(), List.of());
    }

    public static RecommendationResponse withProjects(String reply, List<HouseItem> recommendations, List<ProjectItem> projects) {
        return new RecommendationResponse(reply, recommendations, projects, List.of());
    }
}
