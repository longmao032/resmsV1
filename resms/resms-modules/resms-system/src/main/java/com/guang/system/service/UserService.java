package com.guang.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.system.domain.dto.PasswordChangeDTO;
import com.guang.system.domain.dto.ProfileEditDTO;
import com.guang.system.domain.dto.UserQueryDTO;
import com.guang.system.domain.dto.UserSaveDTO;
import com.guang.system.domain.vo.UserVO;
import com.guang.system.entity.User;

/**
 * <p>
 * 系统用户表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface UserService extends IService<User> {

    /**
     * 分页查询用户
     */
    Page<UserVO> pageUsers(UserQueryDTO queryDTO);

    /**
     * 保存或更新用户
     */
    boolean saveUser(UserSaveDTO saveDTO);

    /**
     * 删除用户
     */
    boolean deleteUser(Integer id);

    /**
     * 修改用户状态
     */
    boolean updateStatus(Integer id, Byte status);

    /**
     * 重置密码
     */
    boolean resetPassword(Integer id, String password);

    /**
     * 获取当前用户个人信息
     */
    UserVO getProfile(Integer userId);

    /**
     * 更新个人信息
     */
    boolean updateProfile(Integer userId, ProfileEditDTO editDTO);

    /**
     * 修改密码（需校验旧密码）
     */
    boolean changePassword(Integer userId, PasswordChangeDTO dto);

    /**
     * 查询销售人员选项列表（下拉框用，仅返回 id + realName，可选姓名过滤）
     */
    java.util.List<com.guang.system.domain.vo.SalesOptionVO> listSalesOptions(String realName);

    /**
     * 查询指定部门下的用户列表（下拉框用）
     */
    java.util.List<com.guang.system.domain.vo.SalesOptionVO> listByDept(Integer deptId);
}
