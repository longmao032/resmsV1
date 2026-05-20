package com.guang.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.system.domain.vo.SalesOptionVO;
import com.guang.system.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 系统用户表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询销售人员选项列表（用于下拉框，不含分页，可选姓名筛选）
     */
    List<SalesOptionVO> selectSalesOptions(@Param("realName") String realName);

    /**
     * 查询指定部门下的用户列表（用于下拉框）
     */
    List<SalesOptionVO> selectByDept(@Param("deptId") Integer deptId);
}
