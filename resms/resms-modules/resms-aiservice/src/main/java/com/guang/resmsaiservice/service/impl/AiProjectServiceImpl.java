package com.guang.resmsaiservice.service.impl;

import com.guang.house.entity.House;
import com.guang.house.entity.Project;
import com.guang.house.service.HouseService;
import com.guang.house.service.ProjectService;
import com.guang.resmsaiservice.service.AiProjectService;
import com.guang.resmsaiservice.vo.HouseAiVo;
import com.guang.resmsaiservice.vo.ProjectVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiProjectServiceImpl implements AiProjectService {

    private final ProjectService projectService;
    private final HouseService houseService;

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
                        .build();
            }).collect(Collectors.toList());

            pVo.setHouses(hVos);
            return pVo;
        }).collect(Collectors.toList());
    }
}
