package com.guang.portal.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.config.FileProperties;
import com.guang.common.exception.ApiException;
import com.guang.common.util.SecurityUtils;
import com.guang.house.entity.House;
import com.guang.house.entity.HouseImage;
import com.guang.house.entity.Project;
import com.guang.house.service.HouseImageService;
import com.guang.house.service.HouseService;
import com.guang.house.service.ProjectService;
import com.guang.portal.domain.dto.BrowseHistoryDTO;
import com.guang.portal.domain.vo.BrowseHistoryItemVO;
import com.guang.portal.service.ClientBrowseHistoryService;
import com.guang.trade.entity.UserBrowseHistory;
import com.guang.trade.service.UserBrowseHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientBrowseHistoryServiceImpl implements ClientBrowseHistoryService {

    private final UserBrowseHistoryService browseHistoryService;
    private final HouseService houseService;
    private final ProjectService projectService;
    private final HouseImageService houseImageService;
    private final FileProperties fileProperties;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<BrowseHistoryItemVO> pageHistory(Integer pageNum, Integer pageSize, Byte resourceType) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }
        return pageHistory(appUserId, pageNum, pageSize, resourceType);
    }

    @Override
    public Page<BrowseHistoryItemVO> pageHistory(Long userId, Integer pageNum, Integer pageSize, Byte resourceType) {
        Page<UserBrowseHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserBrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBrowseHistory::getAppUserId, userId)
                .eq(resourceType != null && resourceType > 0, UserBrowseHistory::getResourceType, resourceType)
                .orderByDesc(UserBrowseHistory::getViewTime);

        Page<UserBrowseHistory> hisPage = browseHistoryService.page(page, wrapper);
        List<UserBrowseHistory> records = hisPage.getRecords();
        if (records.isEmpty()) {
            Page<BrowseHistoryItemVO> empty = new Page<>(hisPage.getCurrent(), hisPage.getSize(), hisPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        List<Integer> houseIds = new ArrayList<>();
        List<Integer> projectIds = new ArrayList<>();

        for (UserBrowseHistory h : records) {
            if (h.getResourceType() == 1) {
                houseIds.add(h.getResourceId());
            } else if (h.getResourceType() == 2) {
                projectIds.add(h.getResourceId());
            }
        }

        final Map<Integer, House> houseMap;
        if (!houseIds.isEmpty()) {
            houseMap = houseService.listByIds(houseIds)
                    .stream()
                    .collect(Collectors.toMap(House::getId, h -> h, (a, b) -> a));
        } else {
            houseMap = Collections.emptyMap();
        }

        final Map<Integer, Project> projectMap;
        if (!projectIds.isEmpty()) {
            projectMap = projectService.listByIds(projectIds)
                    .stream()
                    .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a));
        } else {
            projectMap = Collections.emptyMap();
        }

        final Map<Integer, String> houseCoverMap;
        if (!houseIds.isEmpty()) {
            String imgPrefix = fileProperties.getPrefix();
            List<HouseImage> images = houseImageService.list(new LambdaQueryWrapper<HouseImage>()
                    .in(HouseImage::getHouseId, houseIds)
                    .eq(HouseImage::getIsDefault, 1));
            houseCoverMap = images.stream()
                    .collect(Collectors.toMap(HouseImage::getHouseId,
                            img -> imgPrefix + "/" + img.getFileKey(),
                            (a, b) -> a));
        } else {
            houseCoverMap = Collections.emptyMap();
        }

        String prefix = fileProperties.getPrefix();
        List<BrowseHistoryItemVO> voList = records.stream().map(h -> {
            BrowseHistoryItemVO vo = new BrowseHistoryItemVO();
            vo.setId(h.getId());
            vo.setResourceType(h.getResourceType().intValue());
            vo.setResourceId(h.getResourceId());
            vo.setActionType(h.getActionType());
            vo.setDuration(h.getDuration());
            vo.setViewTime(h.getViewTime() != null ? h.getViewTime().format(DTF) : null);

            if (h.getResourceType() == 1) {
                House house = houseMap.get(h.getResourceId());
                if (house != null) {
                    vo.setResourceTitle(String.format("%s %s %sm²",
                            StrUtil.nullToDefault(house.getProjectName(), ""),
                            StrUtil.nullToDefault(house.getLayout(), ""),
                            house.getArea() != null ? house.getArea().stripTrailingZeros().toPlainString() : ""));
                    vo.setCity(house.getCity());
                    vo.setDistrict(house.getDistrict());
                    vo.setLayout(house.getLayout());
                    vo.setArea(house.getArea());
                    vo.setPriceDesc(buildPriceDesc(house));

                    String cover = houseCoverMap.get(house.getId());
                    if (StrUtil.isNotBlank(cover)) {
                        vo.setResourceCover(cover);
                    }
                }
            } else if (h.getResourceType() == 2) {
                Project project = projectMap.get(h.getResourceId());
                if (project != null) {
                    vo.setResourceTitle(project.getProjectName());
                    vo.setCity(project.getCity());
                    vo.setDistrict(project.getDistrict());
                    vo.setPriceDesc("楼盘项目");

                    String cover = project.getCoverUrl();
                    if (StrUtil.isNotBlank(cover)) {
                        if (cover.startsWith("http") || (prefix != null && cover.startsWith(prefix))) {
                            vo.setResourceCover(cover);
                        } else {
                            vo.setResourceCover(prefix + (cover.startsWith("/") ? "" : "/") + cover);
                        }
                    }
                }
            }

            return vo;
        }).collect(Collectors.toList());

        Page<BrowseHistoryItemVO> result = new Page<>(hisPage.getCurrent(), hisPage.getSize(), hisPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addHistory(BrowseHistoryDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            return;
        }

        if (dto.getResourceType() == 1) {
            if (houseService.getById(dto.getResourceId()) == null) {
                throw new ApiException("房源不存在");
            }
        } else if (dto.getResourceType() == 2) {
            if (projectService.getById(dto.getResourceId()) == null) {
                throw new ApiException("楼盘项目不存在");
            }
        }

        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        UserBrowseHistory existing = browseHistoryService.lambdaQuery()
                .eq(UserBrowseHistory::getAppUserId, appUserId)
                .eq(UserBrowseHistory::getResourceType, dto.getResourceType())
                .eq(UserBrowseHistory::getResourceId, dto.getResourceId())
                .gt(UserBrowseHistory::getViewTime, tenMinutesAgo)
                .last("LIMIT 1")
                .one();

        if (existing != null) {
            existing.setViewTime(LocalDateTime.now());
            if (dto.getDuration() != null && dto.getDuration() > 0) {
                existing.setDuration(dto.getDuration());
            }
            browseHistoryService.updateById(existing);
        } else {
            UserBrowseHistory history = new UserBrowseHistory();
            history.setAppUserId(appUserId);
            history.setResourceType(dto.getResourceType());
            history.setResourceId(dto.getResourceId());
            history.setActionType("view");
            history.setDuration(dto.getDuration() != null ? dto.getDuration() : 0);
            history.setViewTime(LocalDateTime.now());
            browseHistoryService.save(history);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }

        UserBrowseHistory history = browseHistoryService.getById(id);
        if (history == null) {
            throw new ApiException("记录不存在");
        }
        if (!history.getAppUserId().equals(appUserId)) {
            throw new ApiException("无权操作");
        }

        browseHistoryService.removeById(id);
        log.info("[C端浏览] 用户 {} 删除浏览记录(id={})", appUserId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll() {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }

        LambdaQueryWrapper<UserBrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBrowseHistory::getAppUserId, appUserId);
        browseHistoryService.remove(wrapper);
        log.info("[C端浏览] 用户 {} 清空浏览记录", appUserId);
    }

    private String buildPriceDesc(House house) {
        if (house == null) return "";
        Byte type = house.getHouseType();
        if (type == null) return "";

        int t = type.intValue();
        if (t == 1 || t == 2) {
            if (house.getTotalPriceFen() != null) {
                BigDecimal wan = BigDecimal.valueOf(house.getTotalPriceFen())
                        .divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP);
                return wan.stripTrailingZeros().toPlainString() + "万";
            }
        } else if (t == 3) {
            if (house.getRentPriceFen() != null) {
                BigDecimal yuan = BigDecimal.valueOf(house.getRentPriceFen())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                return yuan.stripTrailingZeros().toPlainString() + "元/月";
            }
        }
        return "";
    }
}
