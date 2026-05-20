package com.guang.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.system.entity.DeptTarget;
import com.guang.system.mapper.DeptTargetMapper;
import com.guang.system.service.DeptTargetService;
import org.springframework.stereotype.Service;

@Service
public class DeptTargetServiceImpl extends ServiceImpl<DeptTargetMapper, DeptTarget> implements DeptTargetService {
}
