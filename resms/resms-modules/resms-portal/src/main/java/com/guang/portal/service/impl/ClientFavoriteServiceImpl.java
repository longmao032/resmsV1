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
import com.guang.house.domain.vo.ProjectPageVO;
import com.guang.house.service.HouseImageService;
import com.guang.house.service.HouseService;
import com.guang.house.service.ProjectService;
import com.guang.portal.domain.dto.FavoriteDTO;
import com.guang.portal.domain.vo.FavoriteItemVO;
import com.guang.portal.service.ClientFavoriteService;
import com.guang.trade.entity.UserFavorite;
import com.guang.trade.service.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientFavoriteServiceImpl implements ClientFavoriteService {

    private final UserFavoriteService userFavoriteService;
    private final HouseService houseService;
    private final HouseImageService houseImageService;
    private final ProjectService projectService;
    private final FileProperties fileProperties;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<FavoriteItemVO> pageFavorites(Integer pageNum, Integer pageSize) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }
        return pageFavorites(appUserId, pageNum, pageSize);
    }

    @Override
    public Page<FavoriteItemVO> pageFavorites(Long userId, Integer pageNum, Integer pageSize) {
        log.info("查询收藏列表: appUserId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);

        Page<UserFavorite> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getAppUserId, userId)
                .in(UserFavorite::getTargetType, 1, 2)
                .orderByDesc(UserFavorite::getCreateTime);

        Page<UserFavorite> favPage = userFavoriteService.page(page, wrapper);
        List<UserFavorite> records = favPage.getRecords();
        log.info("收藏列表查询结果: total={}, records.size={}", favPage.getTotal(), records.size());
        if (records.isEmpty()) {
            Page<FavoriteItemVO> empty = new Page<>(favPage.getCurrent(), favPage.getSize(), favPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        List<Integer> houseIds = records.stream()
                .filter(r -> r.getTargetType() == 1)
                .map(UserFavorite::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Integer, House> houseMap = houseIds.isEmpty() ? Collections.emptyMap() :
                houseService.listByIds(houseIds)
                        .stream()
                        .collect(Collectors.toMap(House::getId, h -> h, (a, b) -> a));

        List<Integer> projectIds = records.stream()
                .filter(r -> r.getTargetType() == 2)
                .map(UserFavorite::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Integer, ProjectPageVO> projectMap = projectIds.isEmpty() ? Collections.emptyMap() :
                projectIds.stream()
                        .map(projectService::getProjectDetailForPortal)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(ProjectPageVO::getId, p -> p, (a, b) -> a));

        List<HouseImage> images = houseIds.isEmpty() ? Collections.emptyList() :
                houseImageService.list(new LambdaQueryWrapper<HouseImage>()
                        .in(HouseImage::getHouseId, houseIds)
                        .eq(HouseImage::getIsDefault, 1));
        Map<Integer, String> coverMap = images.stream()
                .collect(Collectors.toMap(HouseImage::getHouseId,
                        img -> fileProperties.getPrefix() + "/" + img.getFileKey(),
                        (a, b) -> a));

        String prefix = fileProperties.getPrefix();
        List<FavoriteItemVO> voList = records.stream()
                .map(fav -> {
                    if (fav.getTargetType() == 1) {
                        House house = houseMap.get(fav.getTargetId());
                        if (house == null) return null;

                        FavoriteItemVO vo = new FavoriteItemVO();
                        vo.setId(fav.getId());
                        vo.setHouseId(house.getId());
                        vo.setHouseNo(house.getHouseNo());
                        vo.setProjectName(house.getProjectName());
                        vo.setHouseType(house.getHouseType() != null ? house.getHouseType().intValue() : null);
                        vo.setCity(house.getCity());
                        vo.setDistrict(house.getDistrict());
                        vo.setArea(house.getArea());
                        vo.setLayout(house.getLayout());

                        String coverKey = coverMap.get(house.getId());
                        if (StrUtil.isNotBlank(coverKey)) {
                            vo.setCoverUrl(coverKey);
                        }

                        fillPrice(vo, house);

                        vo.setFavoriteTime(fav.getCreateTime() != null
                                ? fav.getCreateTime().format(DTF) : null);

                        return vo;
                    } else if (fav.getTargetType() == 2) {
                        ProjectPageVO project = projectMap.get(fav.getTargetId());
                        if (project == null) return null;

                        FavoriteItemVO vo = new FavoriteItemVO();
                        vo.setId(fav.getId());
                        vo.setHouseId(project.getId());
                        vo.setProjectName(project.getProjectName());
                        vo.setHouseType(4);
                        vo.setCity(project.getCity());
                        vo.setDistrict(project.getDistrict());
                        vo.setArea(project.getMinArea());
                        vo.setLayout(project.getLayoutSummary());
                        String cover = project.getCoverUrl();
                        if (StrUtil.isNotBlank(cover)) {
                            if (cover.startsWith("http") || (prefix != null && cover.startsWith(prefix))) {
                                vo.setCoverUrl(cover);
                            } else {
                                vo.setCoverUrl(prefix + (cover.startsWith("/") ? "" : "/") + cover);
                            }
                        }

                        if (project.getAvgPrice() != null) {
                            vo.setTotalPrice(project.getAvgPrice());
                            vo.setPriceUnit("元/㎡");
                        }

                        vo.setFavoriteTime(fav.getCreateTime() != null
                                ? fav.getCreateTime().format(DTF) : null);
                        return vo;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Page<FavoriteItemVO> result = new Page<>(favPage.getCurrent(), favPage.getSize(), favPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(FavoriteDTO dto) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }
        if (dto.getTargetType() == null) {
            throw new ApiException("收藏类型不能为空");
        }
        if (dto.getTargetId() == null) {
            throw new ApiException("收藏目标ID不能为空");
        }

        if (dto.getTargetType() == 1) {
            House house = houseService.getById(dto.getTargetId());
            if (house == null) {
                throw new ApiException("房源不存在");
            }
        } else if (dto.getTargetType() == 2) {
            Project project = projectService.getById(dto.getTargetId());
            if (project == null) {
                throw new ApiException("楼盘项目不存在");
            }
        }

        long count = userFavoriteService.lambdaQuery()
                .eq(UserFavorite::getAppUserId, appUserId)
                .eq(UserFavorite::getTargetType, dto.getTargetType())
                .eq(UserFavorite::getTargetId, dto.getTargetId())
                .count();
        if (count > 0) {
            return;
        }

        UserFavorite fav = new UserFavorite();
        fav.setAppUserId(appUserId);
        fav.setTargetType(dto.getTargetType());
        fav.setTargetId(dto.getTargetId());
        fav.setCreateTime(LocalDateTime.now());
        userFavoriteService.save(fav);

        log.info("[C端收藏] 用户 {} 收藏 {}(id={})", appUserId,
                dto.getTargetType() == 1 ? "房源" : "项目", dto.getTargetId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavoriteById(Integer id) {
        Long appUserId = SecurityUtils.getAppUserId();
        if (appUserId == null) {
            throw new ApiException("未登录");
        }

        UserFavorite fav = userFavoriteService.getById(id);
        if (fav == null) {
            throw new ApiException("收藏记录不存在");
        }
        if (!fav.getAppUserId().equals(appUserId)) {
            throw new ApiException("无权操作");
        }

        userFavoriteService.removeById(id);
        log.info("[C端收藏] 用户 {} 取消收藏记录(id={})", appUserId, id);
    }

    private void fillPrice(FavoriteItemVO vo, House house) {
        Byte type = house.getHouseType();

        if (type != null) {
            int t = type.intValue();
            if (t == 1 || t == 2) {
                vo.setPriceUnit("元/套");
                if (house.getTotalPriceFen() != null) {
                    BigDecimal wan = BigDecimal.valueOf(house.getTotalPriceFen())
                            .divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP);
                    vo.setTotalPrice(wan);
                }
                if (house.getUnitPriceFen() != null) {
                    BigDecimal yuanPerM2 = BigDecimal.valueOf(house.getUnitPriceFen())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    vo.setUnitPrice(yuanPerM2);
                }
            } else if (t == 3) {
                vo.setPriceUnit("元/月");
                if (house.getRentPriceFen() != null) {
                    BigDecimal yuan = BigDecimal.valueOf(house.getRentPriceFen())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    vo.setTotalPrice(yuan);
                }
            }
        }
    }
}
