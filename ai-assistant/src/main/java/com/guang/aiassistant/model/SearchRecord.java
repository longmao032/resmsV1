package com.guang.aiassistant.model;

import java.util.Map;

/**
 * 单次搜索记录模型
 */
public record SearchRecord(String t, String tool, Map<String, Object> params, int count) {}
