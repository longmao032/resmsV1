package com.guang.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.house.entity.HouseStatusLog;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.guang.house.domain.vo.HouseStatusLogVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 房源状态流转日志表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface HouseStatusLogMapper extends BaseMapper<HouseStatusLog> {

    /**
     * 分页查询日志 (联表查询房源号和项目名)
     */
    @Select("SELECT l.*, h.house_no, h.project_name " +
            "FROM tb_house_status_log l " +
            "LEFT JOIN tb_house h ON l.house_id = h.id " +
            "${ew.customSqlSegment}")
    IPage<HouseStatusLogVO> selectPageVo(IPage<HouseStatusLogVO> page, @Param(Constants.WRAPPER) Wrapper<HouseStatusLog> queryWrapper);
}
