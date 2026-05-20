package com.guang.house.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.house.domain.dto.HouseStatusLogQueryDTO;
import com.guang.house.domain.vo.HouseStatusLogVO;
import com.guang.house.entity.HouseStatusLog;
import com.guang.house.mapper.HouseStatusLogMapper;
import com.guang.house.service.HouseStatusLogService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 房源状态流转日志表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
public class HouseStatusLogServiceImpl extends ServiceImpl<HouseStatusLogMapper, HouseStatusLog> implements HouseStatusLogService {

    @Override
    public Page<HouseStatusLogVO> pageLogs(HouseStatusLogQueryDTO queryDTO) {
        Page<HouseStatusLogVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        QueryWrapper<HouseStatusLog> wrapper = new QueryWrapper<>();
        
        wrapper.eq(queryDTO.getHouseId() != null, "l.house_id", queryDTO.getHouseId())
                .and(StrUtil.isNotBlank(queryDTO.getHouseNo()), w -> 
                    w.like("h.house_no", queryDTO.getHouseNo())
                     .or()
                     .like("h.project_name", queryDTO.getHouseNo()))
                .like(StrUtil.isNotBlank(queryDTO.getOperatorName()), "l.operator_name", queryDTO.getOperatorName())
                .orderByDesc("l.create_time");

        return (Page<HouseStatusLogVO>) baseMapper.selectPageVo(page, wrapper);
    }
}
