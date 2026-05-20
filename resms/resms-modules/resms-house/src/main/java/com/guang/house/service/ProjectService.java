package com.guang.house.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.house.domain.dto.ProjectQueryDTO;
import com.guang.house.domain.dto.ProjectSaveDTO;
import com.guang.house.entity.Project;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.house.domain.vo.ProjectPageVO;
import com.guang.house.domain.vo.ProjectStatisticsVO;
import com.guang.house.domain.vo.ProjectVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 楼盘项目表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface ProjectService extends IService<Project> {

    /**
     * 分页查询楼盘项目 (支持地理位置搜索)
     */
    Page<ProjectVO> pageProjects(ProjectQueryDTO queryDTO);

    /**
     * 分页查询楼盘项目（含房源聚合数据，C端使用）
     */
    Page<ProjectPageVO> pageProjectsForPortal(ProjectQueryDTO queryDTO);

    /**
     * 获取楼盘详情（含房源聚合数据，C端使用）
     */
    ProjectPageVO getProjectDetailForPortal(Integer id);

    /**
     * 保存项目（含编号生成与坐标转换）
     */
    Boolean saveProject(ProjectSaveDTO saveDTO);

    /**
     * 删除项目
     */
    Boolean deleteProject(Integer id);

    /**
     * 获取项目详情
     */
    ProjectVO getProjectDetail(Integer id);

    /**
     * 获取项目统计概览（顶部看板）
     */
    ProjectStatisticsVO getStatistics();

    /**
     * 导出项目列表
     */
    void exportProjects(ProjectQueryDTO queryDTO, HttpServletResponse response);
}
