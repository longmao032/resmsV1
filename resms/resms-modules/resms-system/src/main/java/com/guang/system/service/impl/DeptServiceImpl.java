package com.guang.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guang.common.exception.ApiException;
import com.guang.system.domain.dto.DeptQueryDTO;
import com.guang.system.domain.dto.DeptSaveDTO;
import com.guang.system.domain.vo.DeptTreeVO;
import com.guang.system.entity.Dept;
import com.guang.system.mapper.DeptMapper;
import com.guang.system.service.DeptService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 部门表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    @Override
    public List<DeptTreeVO> getDeptTree(DeptQueryDTO queryDTO) {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getDeptName()), Dept::getDeptName, queryDTO.getDeptName())
                .eq(queryDTO.getStatus() != null, Dept::getStatus, queryDTO.getStatus())
                .eq(Dept::getIsDeleted, 0)
                .orderByAsc(Dept::getSortOrder);
        List<Dept> depts = this.list(wrapper);
        return buildDeptTree(depts);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveDept(DeptSaveDTO saveDTO) {
        Dept dept = BeanUtil.copyProperties(saveDTO, Dept.class);
        
        // 1. 维护 ancestors 字段
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            Dept parent = this.getById(dept.getParentId());
            if (parent == null) {
                throw new ApiException("父部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + dept.getParentId());
        } else {
            dept.setAncestors("0");
            dept.setParentId(0);
        }

        // 2. 如果是更新，且父部门变了，需要更新所有子部门的 ancestors
        if (dept.getId() != null) {
            Dept oldDept = this.getById(dept.getId());
            if (!oldDept.getParentId().equals(dept.getParentId())) {
                updateChildAncestors(dept);
            }
        }

        return this.saveOrUpdate(dept);
    }

    @Override
    public Boolean deleteDept(Integer id) {
        // 检查是否有子部门
        long count = this.count(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getParentId, id)
                .eq(Dept::getIsDeleted, 0));
        if (count > 0) {
            throw new ApiException("存在子部门，不允许删除");
        }
        
        Dept dept = new Dept();
        dept.setId(id);
        dept.setIsDeleted((byte) 1);
        return this.updateById(dept);
    }

    /**
     * 构建部门树
     */
    private List<DeptTreeVO> buildDeptTree(List<Dept> depts) {
        if (depts == null || depts.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<DeptTreeVO> vos = depts.stream().map(d -> {
            DeptTreeVO vo = BeanUtil.copyProperties(d, DeptTreeVO.class);
            return vo;
        }).collect(Collectors.toList());

        Map<Integer, List<DeptTreeVO>> childrenMap = vos.stream()
                .collect(Collectors.groupingBy(DeptTreeVO::getParentId));

        vos.forEach(v -> v.setChildren(childrenMap.get(v.getId())));

        return vos.stream()
                .filter(v -> v.getParentId() == null || v.getParentId() == 0 || !childrenMap.containsKey(v.getParentId()))
                .collect(Collectors.toList());
    }

    /**
     * 更新子部门的 ancestors
     */
    private void updateChildAncestors(Dept dept) {
        List<Dept> children = this.list(new LambdaQueryWrapper<Dept>()
                .like(Dept::getAncestors, "," + dept.getId())
                .eq(Dept::getIsDeleted, 0));
        if (children != null && !children.isEmpty()) {
            for (Dept child : children) {
                String newAncestors = dept.getAncestors() + "," + dept.getId();
                // 替换掉旧的路径前缀
                // 注意：这里的逻辑需要非常小心，建议使用递归或更严谨的字符串处理
                // 简化起见，这里假设 ancestors 是从根开始的完整路径
                String oldAncestors = child.getAncestors();
                String targetIdMarker = "," + dept.getId();
                int index = oldAncestors.indexOf(targetIdMarker);
                if (index != -1) {
                    child.setAncestors(dept.getAncestors() + oldAncestors.substring(index));
                }
            }
            this.updateBatchById(children);
        }
    }
}
