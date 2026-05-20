package com.guang.aiassistant.model;

/**
 * 楼盘/小区级结构化数据契约 — 前端 ProjectCard 组件的渲染数据源。
 */
public record ProjectItem(
        String projectId,
        String projectName,
        String district,
        String address,
        String developer,
        String houseCount,
        String priceRange,
        String areaRange,
        String layouts,
        String tags,
        String coverUrl,
        String sellingPoint
) {}
