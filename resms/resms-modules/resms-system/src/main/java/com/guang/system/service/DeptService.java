package com.guang.system.service;

import com.guang.system.domain.dto.DeptQueryDTO;
import com.guang.system.domain.dto.DeptSaveDTO;
import com.guang.system.domain.vo.DeptTreeVO;
import com.guang.system.entity.Dept;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 部门表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface DeptService extends IService<Dept> {

    /**
     * 获取部门树
     */
    List<DeptTreeVO> getDeptTree(DeptQueryDTO queryDTO);

    /**
     * 保存部门
     */
    Boolean saveDept(DeptSaveDTO saveDTO);

    /**
     * 删除部门
     */
    Boolean deleteDept(Integer id);
}
