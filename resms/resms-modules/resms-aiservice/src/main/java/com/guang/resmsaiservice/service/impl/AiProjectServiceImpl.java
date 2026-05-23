package com.guang.resmsaiservice.service.impl;

import com.guang.common.config.FileProperties;
import com.guang.house.entity.House;
import com.guang.house.entity.HouseImage;
import com.guang.house.entity.Project;
import com.guang.house.service.HouseImageService;
import com.guang.house.service.HouseService;
import com.guang.house.service.ProjectService;
import com.guang.resmsaiservice.service.AiProjectService;
import com.guang.resmsaiservice.vo.HouseAiVo;
import com.guang.resmsaiservice.vo.ProjectVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiProjectServiceImpl implements AiProjectService {

    private final ProjectService projectService;
    private final HouseService houseService;
    private final HouseImageService houseImageService;
    private final FileProperties fileProperties;

    @Override
    public List<ProjectVo> listAllProjectsWithHouses() {
        // 1. 获取所有项目
        List<Project> projects = projectService.list();
        
        // 2. 获取所有房源（过滤已删除和非在售状态）
        List<House> allHouses = houseService.lambdaQuery()
                .eq(House::getStatus, (byte) 1) // 在售
                .eq(House::getIsDeleted, (byte) 0) // 未删除
                .list();

        // 3. 按项目ID分组房源
        Map<Integer, List<House>> housesByProject = allHouses.stream()
                .collect(Collectors.groupingBy(House::getProjectId));

        // 4. 组装层级数据
        return projects.stream().map(project -> {
            ProjectVo pVo = ProjectVo.builder()
                    .id(project.getId())
                    .projectName(project.getProjectName())
                    .projectType(project.getProjectType())
                    .developer(project.getDeveloper())
                    .propertyCompany(project.getPropertyCompany())
                    .province(project.getProvince())
                    .city(project.getCity())
                    .district(project.getDistrict())
                    .address(project.getAddress())
                    .totalHouseholds(project.getTotalHouseholds())
                    .propertyFee(project.getPropertyFee())
                    .plotRatio(project.getPlotRatio())
                    .greeningRate(project.getGreeningRate())
                    .tags(project.getTags())
                    .coverUrl(project.getCoverUrl())
                    .longitude(project.getLongitude())
                    .latitude(project.getLatitude())
                    .updateTime(project.getUpdateTime())
                    .build();

            // 转换房源信息
            List<House> houses = housesByProject.getOrDefault(project.getId(), new ArrayList<>());
            // 批量查询本项目所有房源的默认封面图
            Map<Integer, String> coverUrlMap = Collections.emptyMap();
            if (!houses.isEmpty()) {
                List<Integer> houseIds = houses.stream().map(House::getId).toList();
                List<HouseImage> coverImages = houseImageService.lambdaQuery()
                        .in(HouseImage::getHouseId, houseIds)
                        .eq(HouseImage::getIsDefault, (byte) 1)
                        .eq(HouseImage::getAuditStatus, (byte) 1) // 只取审核通过的
                        .list();
                String prefix = fileProperties.getPrefix();
                coverUrlMap = coverImages.stream()
                        .collect(Collectors.toMap(
                                HouseImage::getHouseId,
                                img -> buildFullUrl(prefix, img.getFileKey()),
                                (a, b) -> a));
            }
            final Map<Integer, String> finalCoverMap = coverUrlMap;

            List<HouseAiVo> hVos = houses.stream().map(house -> {
                String priceText = "";
                if (house.getHouseType() == 3) { // 租房
                    priceText = (house.getRentPriceFen() != null ? house.getRentPriceFen() / 100 : 0) + "元/月";
                } else { // 新房或二手房
                    BigDecimal totalTenThousand = house.getTotalPriceFen() != null ?
                        new BigDecimal(house.getTotalPriceFen()).divide(new BigDecimal(1000000), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    priceText = totalTenThousand + "万元";
                }

                String unitPriceText = (house.getUnitPriceFen() != null ? house.getUnitPriceFen() / 100 : 0) + "元/㎡";

                return HouseAiVo.builder()
                        .id(house.getId())
                        .houseTitle(project.getProjectName() + " " + house.getLayout() + " " + house.getArea() + "㎡")
                        .roomType(house.getLayout())
                        .areaText(house.getArea() + "㎡")
                        .priceText(priceText)
                        .unitPriceText(unitPriceText)
                        .tags(house.getTags())
                        .floorInfo(house.getFloor() + "/" + house.getTotalFloor() + "层")
                        .orientation(house.getOrientation())
                        .houseType(house.getHouseType())
                        .description(house.getDescription())
                        .coverUrl(finalCoverMap.get(house.getId()))
                        .build();
            }).collect(Collectors.toList());

            pVo.setHouses(hVos);
            return pVo;
        }).collect(Collectors.toList());
    }

    /** 拼接完整图片访问 URL：prefix + "/" + fileKey */
    private String buildFullUrl(String prefix, String fileKey) {
        if (fileKey == null || fileKey.isBlank()) return null;
        if (fileKey.startsWith("http")) return fileKey;
        if (fileKey.startsWith(prefix)) return fileKey;
        return prefix + "/" + fileKey;
    }
}
