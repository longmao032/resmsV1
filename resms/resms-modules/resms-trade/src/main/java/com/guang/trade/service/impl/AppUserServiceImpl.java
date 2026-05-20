package com.guang.trade.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.common.exception.ApiException;
import com.guang.common.util.ExcelUtils;
import com.guang.trade.domain.dto.AppUserQueryDTO;
import com.guang.trade.domain.vo.AppUserExportVO;
import com.guang.trade.domain.vo.AppUserStatisticsVO;
import com.guang.trade.entity.AppUser;
import com.guang.trade.entity.Customer;
import com.guang.trade.mapper.AppUserMapper;
import com.guang.trade.mapper.CustomerMapper;
import com.guang.trade.service.AppUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

/**
 * C端移动端用户账号表 服务实现类
 *
 * @author blackDuck
 * @since 2026-05-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    // 注入 CustomerMapper，用于合并时查询线下客户档案
    private final CustomerMapper customerMapper;

    @Override
    public Page<AppUser> pageAppUsers(AppUserQueryDTO queryDTO) {
        Page<AppUser> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getPhone()), AppUser::getPhone, queryDTO.getPhone())
               .like(StrUtil.isNotBlank(queryDTO.getNickname()), AppUser::getNickname, queryDTO.getNickname())
               .eq(queryDTO.getStatus() != null, AppUser::getStatus, queryDTO.getStatus())
               .orderByDesc(AppUser::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public Boolean changeStatus(Long id, Byte status) {
        AppUser appUser = new AppUser();
        appUser.setId(id);
        appUser.setStatus(status);
        return this.updateById(appUser);
    }

    @Override
    public AppUserStatisticsVO getStatistics() {
        AppUserStatisticsVO vo = new AppUserStatisticsVO();

        // 用户总数
        long total = lambdaQuery().eq(AppUser::getIsDeleted, 0).count();
        vo.setTotalCount(total);

        // 本周新增
        LocalDateTime weekStart = LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        long weeklyNew = lambdaQuery()
                .eq(AppUser::getIsDeleted, 0)
                .ge(AppUser::getCreateTime, weekStart)
                .count();
        vo.setWeeklyNewCount(weeklyNew);

        // 正常状态
        long normal = lambdaQuery()
                .eq(AppUser::getIsDeleted, 0)
                .eq(AppUser::getStatus, (byte) 1)
                .count();
        vo.setNormalCount(normal);

        // 封禁状态
        long banned = lambdaQuery()
                .eq(AppUser::getIsDeleted, 0)
                .eq(AppUser::getStatus, (byte) 0)
                .count();
        vo.setBannedCount(banned);

        return vo;
    }

    @Override
    public void exportAppUsers(AppUserQueryDTO queryDTO, HttpServletResponse response) {
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getPhone()), AppUser::getPhone, queryDTO.getPhone())
               .like(StrUtil.isNotBlank(queryDTO.getNickname()), AppUser::getNickname, queryDTO.getNickname())
               .eq(queryDTO.getStatus() != null, AppUser::getStatus, queryDTO.getStatus())
               .eq(AppUser::getIsDeleted, 0)
               .orderByDesc(AppUser::getCreateTime);

        List<AppUser> list = this.list(wrapper);

        List<AppUserExportVO> exportList = list.stream().map(u -> {
            AppUserExportVO vo = new AppUserExportVO();
            vo.setId(u.getId());
            vo.setPhone(u.getPhone());
            vo.setNickname(u.getNickname());
            vo.setWechatOpenid(u.getWechatOpenid());
            vo.setUnionId(u.getUnionId());
            vo.setStatusText(u.getStatus() != null && u.getStatus() == 1 ? "正常" : "封禁");
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        ExcelUtils.exportExcel(response, "C端用户_" + System.currentTimeMillis(), "C端用户", AppUserExportVO.class, exportList);
    }

    /**
     * C端注册（含账号合并逻辑）
     *
     * <p><b>流程说明：</b></p>
     * <pre>
     * [Step 1] 按手机号查 tb_app_user
     *   ├─ 已存在 → 直接使用已有账号（幂等，避免重复注册）
     *   └─ 不存在 → 新建 AppUser 记录
     *
     * [Step 2] 按手机号查 tb_customer（线下录入的客户档案）
     *   ├─ 不存在 → 无需合并，流程结束
     *   ├─ 存在 + app_user_id 已绑定 → 校验是否绑定的是当前账号，异常则报警
     *   └─ 存在 + app_user_id 为空  → 【关键】将 app_user_id 写入客户档案，完成 B/C 端数据打通
     * </pre>
     *
     * <p><b>并发安全：</b>tb_app_user 上 phone 列存在唯一索引，若并发注册同一手机号，
     * 数据库会抛出 DuplicateKeyException，事务回滚后由业务层重试或上层拦截提示"该手机号已注册"。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppUser register(String phone, String password, String nickname, String avatarUrl) {
        if (StrUtil.isBlank(phone)) {
            throw new ApiException("手机号不能为空");
        }

        // =============================================
        // Step 1：幂等注册 —— 手机号已存在则复用账号
        // =============================================
        AppUser appUser = this.getOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone)
        );

        boolean isNewUser = (appUser == null);
        if (isNewUser) {
            appUser = new AppUser();
            appUser.setPhone(phone);
            appUser.setPassword(password);
            appUser.setNickname(StrUtil.isBlank(nickname) ? "用户" + phone.substring(7) : nickname);
            appUser.setAvatarUrl(avatarUrl);
            appUser.setStatus((byte) 1);  // 正常状态
            appUser.setCreateTime(LocalDateTime.now());
            appUser.setUpdateTime(LocalDateTime.now());
            this.save(appUser);
            log.info("[AppUser注册] 新账号创建成功，phone={}, id={}", phone, appUser.getId());
        } else {
            log.info("[AppUser注册] 手机号已存在，复用账号 id={}", appUser.getId());
        }

        // =============================================
        // Step 2：检查是否存在同手机号的线下客户档案
        // =============================================
        mergeWithOfflineCustomer(appUser.getId(), phone);

        return appUser;
    }

    /**
     * 将 AppUser 账号与同手机号的线下客户档案自动关联绑定。
     *
     * <p>幂等设计：若线下档案已绑定当前 appUserId，则跳过；若绑定了其他账号，打日志警告但不中断流程（
     * 这种情况理论上不应发生，但防御性处理）。</p>
     *
     * @param appUserId 当前注册的 C 端账号 ID
     * @param phone     手机号
     */
    private void mergeWithOfflineCustomer(Long appUserId, String phone) {
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getPhone, phone)
                        .eq(Customer::getIsDeleted, 0)
        );

        if (customer == null) {
            // 无线下档案，无需合并
            return;
        }

        if (customer.getAppUserId() != null) {
            if (customer.getAppUserId().equals(appUserId)) {
                // 已绑定当前账号，幂等跳过
                log.info("[账号合并] 客户档案(id={})已绑定当前 AppUser(id={})，跳过", customer.getId(), appUserId);
            } else {
                // 绑定了其他账号：警告，但不强行覆盖（需人工核查）
                log.warn("[账号合并] 客户档案(id={})已绑定其他 AppUser(id={})，当前注册账号 id={}，请人工核查",
                        customer.getId(), customer.getAppUserId(), appUserId);
            }
            return;
        }

        // 【核心】：线下档案 app_user_id 为空 → 写入关联，打通 B/C 端数据
        int rows = customerMapper.update(null,
                new LambdaUpdateWrapper<Customer>()
                        .eq(Customer::getId, customer.getId())
                        .isNull(Customer::getAppUserId)   // 加条件防并发重复写入
                        .set(Customer::getAppUserId, appUserId)
                        .set(Customer::getUpdateTime, LocalDateTime.now())
        );

        if (rows > 0) {
            log.info("[账号合并] 成功将 AppUser(id={}) 关联到客户档案(id={}，phone={})，B/C端数据已打通",
                    appUserId, customer.getId(), phone);
        } else {
            // update rows=0：并发场景下被其他线程抢先绑定，重新查询记录日志
            Customer latest = customerMapper.selectById(customer.getId());
            log.warn("[账号合并] 并发绑定：客户档案(id={})已被 AppUser(id={}) 抢先绑定，当前 appUserId={}",
                    customer.getId(), latest != null ? latest.getAppUserId() : "未知", appUserId);
        }
    }
}

