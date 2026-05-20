package com.guang.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guang.trade.domain.dto.AppUserQueryDTO;
import com.guang.trade.domain.vo.AppUserStatisticsVO;
import com.guang.trade.entity.AppUser;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * C端移动端用户账号表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-10
 */
public interface AppUserService extends IService<AppUser> {

    /**
     * 分页查询C端用户
     *
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    Page<AppUser> pageAppUsers(AppUserQueryDTO queryDTO);

    /**
     * 修改用户状态
     *
     * @param id 用户ID
     * @param status 状态
     * @return 是否成功
     */
    Boolean changeStatus(Long id, Byte status);

    /**
     * 获取用户统计数据
     */
    AppUserStatisticsVO getStatistics();

    /**
     * 导出C端用户
     */
    void exportAppUsers(AppUserQueryDTO queryDTO, HttpServletResponse response);

    /**
     * C端 App 注册（含账号合并逻辑）
     * <p>
     * 执行顺序：
     * 1. 检查 tb_app_user 中是否已有该手机号账号（手机号唯一，幂等注册）
     * 2. 若不存在则创建新 AppUser
     * 3. 无论新建还是已有，检查 tb_customer 中同手机号的线下客户档案
     * 4. 若线下档案存在且 app_user_id 为空，则自动关联绑定（打通 B/C 端数据）
     * </p>
     *
     * @param phone    手机号（必填）
     * @param password 密码（可选，小程序模式可不传）
     * @param nickname 昵称（可选）
     * @param avatarUrl 头像（可选）
     * @return 注册/登录后的 AppUser 对象
     */
    AppUser register(String phone, String password, String nickname, String avatarUrl);
}
