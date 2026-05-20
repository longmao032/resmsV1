package com.guang.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.guang.house.domain.vo.ProjectPageVO;
import com.guang.house.domain.vo.ProjectVO;
import com.guang.house.entity.Project;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 楼盘项目表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 分页查询项目 (包含自定义 SQL 字段映射)
     */
    @Select("SELECT ${ew.sqlSelect} FROM tb_project ${ew.customSqlSegment}")
    IPage<ProjectVO> selectPageVo(IPage<ProjectVO> page, @Param(Constants.WRAPPER) Wrapper<Project> queryWrapper);

    /**
     * 分页查询楼盘（含房源聚合数据）— C端使用
     */
    @ResultMap("ProjectPageVOResultMap")
    @Select("${sql}")
    IPage<ProjectPageVO> selectProjectPageVo(IPage<ProjectPageVO> page, @Param("sql") String sql);

    /**
     * 查询所有未删除项目的平均佣金比例
     */
    @Select("SELECT AVG(commission_rate) FROM tb_project WHERE is_deleted = 0")
    BigDecimal selectAvgCommissionRate();

    /**
     * 查询指定时间前未删除项目的平均佣金比例
     */
    @Select("SELECT AVG(commission_rate) FROM tb_project WHERE is_deleted = 0 AND create_time < #{before}")
    BigDecimal selectAvgCommissionRateBefore(@Param("before") LocalDateTime before);
}
