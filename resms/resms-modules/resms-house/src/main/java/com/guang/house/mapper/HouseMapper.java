package com.guang.house.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.house.entity.House;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.guang.house.domain.vo.HousePageVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 房源主表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface HouseMapper extends BaseMapper<House> {

    @ResultMap("HouseResultMap")
    @Select("SELECT * FROM tb_house WHERE id = #{id}")
    House selectById(@Param("id") Serializable id);

    /**
     * 分页查询房源 (包含自定义 SQL 字段映射)
     */
    @ResultMap("HousePageVOResultMap")
    @Select("SELECT ${ew.sqlSelect} FROM tb_house ${ew.customSqlSegment}")
    IPage<HousePageVO> selectPageVo(IPage<HousePageVO> page, @Param(Constants.WRAPPER) Wrapper<House> queryWrapper);

    /**
     * 根据用户ID列表查询真实姓名
     */
    @Select("<script>" +
            "SELECT id, real_name, avatar FROM sys_user WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<java.util.Map<String, Object>> selectUserNames(@Param("ids") java.util.Collection<Integer> ids);

    /**
     * 查询所有已成交房源的 total_price_fen 均值
     */
    @Select("SELECT AVG(total_price_fen) FROM tb_house WHERE status = 3 AND is_deleted = 0")
    BigDecimal selectAvgTotalPriceFen();

    /**
     * 查询指定时间前已成交房源的 total_price_fen 均值
     */
    @Select("SELECT AVG(total_price_fen) FROM tb_house WHERE status = 3 AND is_deleted = 0 AND create_time < #{before}")
    BigDecimal selectAvgTotalPriceFenBefore(@Param("before") LocalDateTime before);
}
