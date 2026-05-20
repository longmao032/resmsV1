package com.guang.house.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.house.domain.dto.ProjectLogQueryDTO;
import com.guang.house.domain.vo.ProjectLogVO;
import com.guang.house.entity.ProjectLog;
import com.guang.house.mapper.ProjectLogMapper;
import com.guang.house.service.ProjectLogService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 项目变更日志表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-13
 */
@Service
public class ProjectLogServiceImpl extends ServiceImpl<ProjectLogMapper, ProjectLog> implements ProjectLogService {

    @Override
    public Page<ProjectLogVO> pageLogs(ProjectLogQueryDTO queryDTO) {
        Page<ProjectLog> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        QueryWrapper<ProjectLog> wrapper = new QueryWrapper<>();

        wrapper.eq(queryDTO.getProjectId() != null, "project_id", queryDTO.getProjectId())
                .orderByDesc("create_time");

        Page<ProjectLog> result = this.page(page, wrapper);
        Page<ProjectLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(BeanUtil.copyToList(result.getRecords(), ProjectLogVO.class));
        return voPage;
    }
}
