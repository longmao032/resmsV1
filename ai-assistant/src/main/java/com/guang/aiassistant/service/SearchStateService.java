package com.guang.aiassistant.service;

import com.guang.aiassistant.model.SearchState;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * 搜索状态与履历留痕服务接口契约 — 纯粹的高可用持久化读写层。
 */
public interface SearchStateService {

    /** 构建搜索上下文字符串（供 LLM/日志使用） */
    String getSearchContext(String sessionId);

    /** 标记画像拉取状态并缓存 persona 文本/JSON */
    void markProfileFetched(String sessionId, boolean hasData,
                            @Nullable String personaText, @Nullable String personaJson);

    /** 记录一次搜索操作 */
    void recordSearch(String sessionId, String tool, Map<String, Object> params, int count);

    /** 清空会话所有状态 */
    void clearSession(String sessionId);

    /** 获取缓存的画像文本 */
    @Nullable
    String getPersonaText(String sessionId);

    /** 获取缓存的画像 JSON */
    @Nullable
    String getPersonaJson(String sessionId);

    /** 从搜索历史中提取最近一次使用的城市 */
    @Nullable
    String getLastCity(String sessionId);

    /** 获取完整会话状态 */
    SearchState getState(String sessionId);
}

