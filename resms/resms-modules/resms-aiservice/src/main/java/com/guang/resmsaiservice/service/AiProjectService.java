package com.guang.resmsaiservice.service;

import com.guang.resmsaiservice.vo.ProjectVo;
import java.util.List;

/**
 * AI 房产数据服务接口
 */
public interface AiProjectService {

    /**
     * 获取所有楼盘及房源信息的层级结构
     * 专门为 AI 知识库或大模型查询设计
     */
    List<ProjectVo> listAllProjectsWithHouses();
}
