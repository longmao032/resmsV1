package com.guang.house.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.house.entity.SyncFailedLog;

/**
 * 实时数据同步失败日志 服务类
 */
public interface SyncFailedLogService extends IService<SyncFailedLog> {

    /**
     * 保存失败日志记录
     */
    void saveLog(String eventType, Integer businessId, Object payload, String errorMsg);
}
