package com.guang.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guang.common.util.SecurityUtils;
import com.guang.trade.domain.dto.HistoryDTO;
import com.guang.trade.domain.vo.FootprintVO;
import com.guang.trade.entity.Customer;
import com.guang.trade.entity.UserBrowseHistory;
import com.guang.trade.mapper.CustomerMapper;
import com.guang.trade.mapper.UserBrowseHistoryMapper;
import com.guang.trade.service.UserBrowseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户浏览历史记录表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class UserBrowseHistoryServiceImpl extends ServiceImpl<UserBrowseHistoryMapper, UserBrowseHistory> implements UserBrowseHistoryService {

    private final UserBrowseHistoryMapper browseHistoryMapper;
    private final CustomerMapper customerMapper;

    @Override
    public Page<FootprintVO> pageFootprints(Integer pageNum, Integer pageSize, String customerName, String actionType) {
        Page<FootprintVO> page = new Page<>(pageNum, pageSize);
        return (Page<FootprintVO>) browseHistoryMapper.selectFootprintPage(page, customerName, actionType);
    }

    @Override
    public Boolean recordHistory(HistoryDTO historyDTO) {
        UserBrowseHistory history = new UserBrowseHistory();

        // 管理端手动录入：通过 customerId 查找 app_user_id
        if (historyDTO.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(historyDTO.getCustomerId());
            if (customer != null) {
                history.setAppUserId(customer.getAppUserId());
            }
        }
        // 如果 customerId 为空且有用户上下文，使用当前用户
        if (history.getAppUserId() == null) {
            Integer userId = SecurityUtils.getUserId();
            if (userId == null) return false;
            history.setAppUserId(Long.valueOf(userId));
        }

        history.setResourceType(historyDTO.getResourceType());
        history.setResourceId(historyDTO.getResourceId());
        history.setActionType(historyDTO.getActionType() != null ? historyDTO.getActionType() : "view");
        history.setDuration(historyDTO.getDuration() != null ? historyDTO.getDuration() : 0);
        history.setInterestLevel(historyDTO.getInterestLevel() != null ? historyDTO.getInterestLevel() : 3);
        history.setContent(historyDTO.getContent());
        history.setViewTime(historyDTO.getViewTime() != null ? historyDTO.getViewTime() : LocalDateTime.now());

        return this.save(history);
    }

    @Override
    public Page<UserBrowseHistory> pageMyHistory(Integer pageNum, Integer pageSize, Byte resourceType) {
        Integer userId = SecurityUtils.getUserId();
        Page<UserBrowseHistory> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<UserBrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBrowseHistory::getAppUserId, userId)
                .eq(resourceType != null, UserBrowseHistory::getResourceType, resourceType)
                .orderByDesc(UserBrowseHistory::getViewTime);

        return this.page(page, wrapper);
    }

    @Override
    public Boolean clearMyHistory() {
        Integer userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<UserBrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBrowseHistory::getAppUserId, userId);
        return this.remove(wrapper);
    }
}
