package com.guang.aiassistant.repository;

import com.guang.aiassistant.model.SearchState;

public interface SearchStateRepository {

    /**
     * 初始化表结构与索引
     */
    void initSchema();

    /**
     * 获取完整会话状态
     *
     * @param sessionId 会话ID
     * @return 搜索状态快照
     */
    SearchState getState(String sessionId);

    /**
     * 保存或更新用户画像拉取状态及缓存数据
     */
    void saveOrUpdateProfile(String sessionId, boolean hasData, String personaText, String personaJson);

    /**
     * 追加单条搜索历史记录
     */
    void appendSearchRecord(String sessionId, String recordJson);

    /**
     * 根据会话ID物理清除状态
     */
    void deleteSession(String sessionId);
}
