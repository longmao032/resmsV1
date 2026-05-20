package com.guang.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guang.trade.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * C端移动端用户账号表 Mapper 接口
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-10
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {

}
