package com.guang.house.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.house.domain.dto.ProjectLogQueryDTO;
import com.guang.house.domain.vo.ProjectLogVO;
import com.guang.house.entity.ProjectLog;

/**
 * <p>
 * 项目变更日志表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-13
 */
public interface ProjectLogService extends IService<ProjectLog> {

    /**
     * 分页查询项目变更日志
     */
    Page<ProjectLogVO> pageLogs(ProjectLogQueryDTO queryDTO);
}
