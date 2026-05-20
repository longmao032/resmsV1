package com.guang.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.system.domain.dto.LogQueryDTO;
import com.guang.system.entity.OperationLog;
import com.guang.system.mapper.OperationLogMapper;
import com.guang.system.service.OperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 操作日志表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public Page<OperationLog> pageLogs(LogQueryDTO queryDTO) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getModule()), OperationLog::getModule, queryDTO.getModule())
                .like(StrUtil.isNotBlank(queryDTO.getUserName()), OperationLog::getUserName, queryDTO.getUserName())
                .eq(StrUtil.isNotBlank(queryDTO.getBusinessType()), OperationLog::getBusinessType, queryDTO.getBusinessType())
                .eq(queryDTO.getStatus() != null, OperationLog::getStatus, queryDTO.getStatus())
                .ge(queryDTO.getBeginTime() != null, OperationLog::getOperationTime, queryDTO.getBeginTime())
                .le(queryDTO.getEndTime() != null, OperationLog::getOperationTime, queryDTO.getEndTime())
                .orderByDesc(OperationLog::getOperationTime);
        return this.page(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
    }
}
