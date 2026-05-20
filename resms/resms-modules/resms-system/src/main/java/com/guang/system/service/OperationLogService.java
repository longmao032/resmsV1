package com.guang.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.system.domain.dto.LogQueryDTO;
import com.guang.system.entity.OperationLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 操作日志表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 分页查询操作日志
     */
    Page<OperationLog> pageLogs(LogQueryDTO queryDTO);
}
