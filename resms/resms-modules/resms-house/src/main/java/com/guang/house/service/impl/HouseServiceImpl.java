package com.guang.house.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.config.FileProperties;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.ExcelUtils;
import com.guang.common.util.SecurityUtils;
import com.guang.common.security.LoginUser;
import com.guang.common.annotation.DataScope;
import com.guang.common.annotation.BolasGuard;
import com.guang.house.domain.dto.HouseAuditDTO;
import com.guang.house.domain.dto.HouseImageSaveDTO;
import com.guang.house.domain.dto.HouseQueryDTO;
import com.alibaba.excel.EasyExcel;
import com.guang.house.domain.dto.HouseSaveDTO;
import com.guang.house.domain.vo.HouseExportVO;
import com.guang.house.domain.vo.HousePageVO;
import com.guang.house.domain.vo.HouseStatisticsVO;
import com.guang.house.domain.vo.HouseVO;
import com.guang.house.entity.*;
import com.guang.house.mapper.HouseMapper;
import com.guang.house.service.*;
import com.guang.common.dto.HouseSyncDTO;
import com.guang.common.event.HouseSyncEvent;
import com.guang.common.event.HouseSyncBatchEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 房源主表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House> implements HouseService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HouseServiceImpl.class);

    private final ProjectService projectService;
    private final NewHouseExtendService newHouseExtendService;
    private final SecondHouseExtendService secondHouseExtendService;
    private final RentHouseExtendService rentHouseExtendService;
    private final HouseStatusLogService houseStatusLogService;
    private final HouseImageService houseImageService;
    private final FileProperties fileProperties;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    @DataScope(userField = "sales_id", joinUserDept = true)
    public Page<HousePageVO> pageHouses(HouseQueryDTO queryDTO) {
        Page<HousePageVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<House> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq(queryDTO.getProjectId() != null, "project_id", queryDTO.getProjectId())
                .eq(queryDTO.getHouseType() != null, "house_type", queryDTO.getHouseType())
                .like(StrUtil.isNotBlank(queryDTO.getCity()), "city", queryDTO.getCity())
                .eq(StrUtil.isNotBlank(queryDTO.getDistrict()), "district", queryDTO.getDistrict())
                .like(StrUtil.isNotBlank(queryDTO.getHouseNo()), "house_no", queryDTO.getHouseNo())
                .like(StrUtil.isNotBlank(queryDTO.getRoomNo()), "room_no", queryDTO.getRoomNo());

        // 1. 单价范围筛选（新房/二手房）——精准命中 idx_search_new / idx_search_second
        wrapper.ge(queryDTO.getMinUnitPriceFen() != null, "unit_price_fen", queryDTO.getMinUnitPriceFen())
                .le(queryDTO.getMaxUnitPriceFen() != null, "unit_price_fen", queryDTO.getMaxUnitPriceFen());

        // 2. 总价范围筛选（二手房/新房）——精准命中 idx_search_second
        wrapper.ge(queryDTO.getMinTotalPriceFen() != null, "total_price_fen", queryDTO.getMinTotalPriceFen())
                .le(queryDTO.getMaxTotalPriceFen() != null, "total_price_fen", queryDTO.getMaxTotalPriceFen());

        // 3. 月租范围筛选（租房）——精准命中 idx_search_rental
        wrapper.ge(queryDTO.getMinRentPriceFen() != null, "rent_price_fen", queryDTO.getMinRentPriceFen())
                .le(queryDTO.getMaxRentPriceFen() != null, "rent_price_fen", queryDTO.getMaxRentPriceFen());

        wrapper.ge(queryDTO.getMinArea() != null, "area", queryDTO.getMinArea())
                .le(queryDTO.getMaxArea() != null, "area", queryDTO.getMaxArea())
                .like(StrUtil.isNotBlank(queryDTO.getLayout()), "layout", queryDTO.getLayout())
                .eq(queryDTO.getStatus() != null, "status", queryDTO.getStatus())
                .eq("is_deleted", 0);

        // 处理地理位置搜索 (ST_Distance_Sphere)
        if (queryDTO.getLongitude() != null && queryDTO.getLatitude() != null) {
            String centerPoint = String.format("ST_GeomFromText('POINT(%f %f)', 4326)", queryDTO.getLatitude(), queryDTO.getLongitude());
            wrapper.select("*", "ST_Distance_Sphere(coordinate, " + centerPoint + ") as distance");
            
            if (queryDTO.getRadius() != null) {
                wrapper.apply("ST_Distance_Sphere(coordinate, " + centerPoint + ") <= {0}", queryDTO.getRadius());
            }
            wrapper.orderByAsc("distance");
        } else {
            wrapper.select("*");
            wrapper.orderByDesc("create_time");
        }

        Page<HousePageVO> resultPage = (Page<HousePageVO>) baseMapper.selectPageVo(page, wrapper);
        List<HousePageVO> records = resultPage.getRecords();
        if (records.isEmpty()) {
            return resultPage;
        }

        // 1. 批量查询封面图
        List<Integer> houseIds = records.stream().map(House::getId).collect(Collectors.toList());
        List<HouseImage> images = houseImageService.list(new LambdaQueryWrapper<HouseImage>()
                .in(HouseImage::getHouseId, houseIds)
                .eq(HouseImage::getIsDefault, 1));
        Map<Integer, String> imageMap = images.stream()
                .collect(Collectors.toMap(HouseImage::getHouseId, HouseImage::getFileKey, (k1, k2) -> k1));

        // 2. 批量查询销售姓名和头像
        Set<Integer> salesIds = records.stream().map(House::getSalesId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, String> salesMap = new HashMap<>();
        Map<Integer, String> salesAvatarMap = new HashMap<>();
        if (!salesIds.isEmpty()) {
            List<Map<String, Object>> userList = baseMapper.selectUserNames(salesIds);
            for (Map<String, Object> user : userList) {
                Integer uid = (Integer) user.get("id");
                salesMap.put(uid, (String) user.get("real_name"));
                salesAvatarMap.put(uid, (String) user.get("avatar"));
            }
        }

        // 3. 填充数据，将 fileKey 转为完整可访问 URL
        for (HousePageVO vo : records) {
            String fileKey = imageMap.get(vo.getId());
            vo.setCoverUrl(getFullUrl(fileKey));
            vo.setSalesName(salesMap.get(vo.getSalesId()));
            vo.setSalesAvatar(getFullUrl(salesAvatarMap.get(vo.getSalesId())));
            // 填充展示用价格字段
            fillDisplayPrices(vo);
        }

        return resultPage;
    }

    @Override
    @DataScope(userField = "sales_id", joinUserDept = true)
    public void exportHouses(HouseQueryDTO queryDTO, HttpServletResponse response) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<House> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq(queryDTO.getProjectId() != null, "project_id", queryDTO.getProjectId())
                .eq(queryDTO.getHouseType() != null, "house_type", queryDTO.getHouseType())
                .like(StrUtil.isNotBlank(queryDTO.getCity()), "city", queryDTO.getCity())
                .eq(StrUtil.isNotBlank(queryDTO.getDistrict()), "district", queryDTO.getDistrict())
                .like(StrUtil.isNotBlank(queryDTO.getHouseNo()), "house_no", queryDTO.getHouseNo())
                .like(StrUtil.isNotBlank(queryDTO.getRoomNo()), "room_no", queryDTO.getRoomNo())
                .ge(queryDTO.getMinUnitPriceFen() != null, "unit_price_fen", queryDTO.getMinUnitPriceFen())
                .le(queryDTO.getMaxUnitPriceFen() != null, "unit_price_fen", queryDTO.getMaxUnitPriceFen())
                .ge(queryDTO.getMinTotalPriceFen() != null, "total_price_fen", queryDTO.getMinTotalPriceFen())
                .le(queryDTO.getMaxTotalPriceFen() != null, "total_price_fen", queryDTO.getMaxTotalPriceFen())
                .ge(queryDTO.getMinRentPriceFen() != null, "rent_price_fen", queryDTO.getMinRentPriceFen())
                .le(queryDTO.getMaxRentPriceFen() != null, "rent_price_fen", queryDTO.getMaxRentPriceFen())
                .ge(queryDTO.getMinArea() != null, "area", queryDTO.getMinArea())
                .le(queryDTO.getMaxArea() != null, "area", queryDTO.getMaxArea())
                .like(StrUtil.isNotBlank(queryDTO.getLayout()), "layout", queryDTO.getLayout())
                .eq(queryDTO.getStatus() != null, "status", queryDTO.getStatus())
                .eq("is_deleted", 0);

        List<House> list = this.list(wrapper);

        List<HouseExportVO> exportList = list.stream().map(house -> {
            HouseExportVO vo = new HouseExportVO();
            BeanUtil.copyProperties(house, vo);

            // 类型转换
            if (house.getHouseType() == 1) vo.setHouseTypeText("新房");
            else if (house.getHouseType() == 2) vo.setHouseTypeText("二手房");
            else if (house.getHouseType() == 3) vo.setHouseTypeText("租房");

            // 状态转换
            String statusText = "未知";
            if (house.getStatus() != null) {
                switch (house.getStatus()) {
                    case 0: statusText = "待审核"; break;
                    case 1: statusText = "在售"; break;
                    case 2: statusText = "已预订"; break;
                    case 3: statusText = "已成交"; break;
                    case 4: statusText = "下架"; break;
                }
            }
            vo.setStatusText(statusText);

            // 楼层显示
            vo.setFloorText(house.getFloor() + "/" + house.getTotalFloor());

            // 价格显示（从分换算为可读单位）
            String priceText = "";
            if (house.getHouseType() != null) {
                if (house.getHouseType() == 1 && house.getTotalPriceFen() != null) {
                    // 新房：展示总价（万元）
                    BigDecimal wan = BigDecimal.valueOf(house.getTotalPriceFen())
                            .divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP);
                    priceText = wan + "万元";
                } else if (house.getHouseType() == 2 && house.getTotalPriceFen() != null) {
                    // 二手房：展示总价（万元）
                    BigDecimal wan = BigDecimal.valueOf(house.getTotalPriceFen())
                            .divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP);
                    priceText = wan + "万元";
                } else if (house.getHouseType() == 3 && house.getRentPriceFen() != null) {
                    // 租房：展示月租（元/月）
                    BigDecimal yuan = BigDecimal.valueOf(house.getRentPriceFen())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    priceText = yuan + "元/月";
                }
            }
            vo.setPriceText(priceText);

            // 单价显示（分/㎡ → 元/㎡）
            if (house.getUnitPriceFen() != null) {
                BigDecimal yuan = BigDecimal.valueOf(house.getUnitPriceFen())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                vo.setUnitPriceText(yuan + "元/㎡");
            }

            // 地址显示
            vo.setAddress((house.getProvince() != null ? house.getProvince() : "") 
                        + (house.getCity() != null ? house.getCity() : "") 
                        + (house.getDistrict() != null ? house.getDistrict() : ""));

            return vo;
        }).collect(Collectors.toList());

        ExcelUtils.exportExcel(response, "房源列表_" + System.currentTimeMillis(), "房源资料", HouseExportVO.class, exportList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BolasGuard(entityClass = House.class, userField = "salesId")
    public Boolean saveHouse(HouseSaveDTO saveDTO) {
        // 1. 校验项目
        Project project = projectService.getById(saveDTO.getProjectId());
        if (project == null) {
            throw new ApiException("所属项目不存在");
        }

        // 2. 指派销售权限校验
        Integer currentUserId = SecurityUtils.getUserId();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 检查是否具有指派权限，如果没有权限且尝试指派他人，则强制纠正为当前用户
        if (loginUser != null && !loginUser.getPermissions().contains("house:house:assign")) {
            if (saveDTO.getSalesId() != null && !saveDTO.getSalesId().equals(currentUserId)) {
                // 如果是更新操作且原本就有销售人员，需要谨慎处理（此处业务设定为：无指派权限者修改房源时，销售人保持不变或强制设为自己）
                // 暂时设定为：无指派权限的用户只能指派给自己
                saveDTO.setSalesId(currentUserId);
            } else if (saveDTO.getSalesId() == null) {
                // 新增时若未传，默认当前用户
                saveDTO.setSalesId(currentUserId);
            }
        }

        House house = BeanUtil.copyProperties(saveDTO, House.class);
        house.setProjectName(project.getProjectName());
        house.setProvince(project.getProvince());
        house.setCity(project.getCity());
        house.setDistrict(project.getDistrict());

        // 2. 生成编号
        if (house.getId() == null) {
            house.setHouseNo(CodeGeneratorUtil.generateHouseNo());
        }

        // 3. 价格字段处理
        // 优先使用新版 Fen 字段，如果缺失则从旧版 price/priceUnit 换算（兼容旧版前端表单）
        if (saveDTO.getUnitPriceFen() == null && saveDTO.getTotalPriceFen() == null && saveDTO.getRentPriceFen() == null) {
            if (saveDTO.getPrice() != null && saveDTO.getPriceUnit() != null) {
                BigDecimal p = saveDTO.getPrice();
                int unit = saveDTO.getPriceUnit().intValue();
                if (unit == 1) { // 元/㎡
                    house.setUnitPriceFen(p.multiply(BigDecimal.valueOf(100)).longValue());
                    // 自动换算总价：总价 = 单价 * 面积
                    if (house.getArea() != null && house.getArea().compareTo(BigDecimal.ZERO) > 0) {
                        house.setTotalPriceFen(p.multiply(BigDecimal.valueOf(100))
                                .multiply(house.getArea()).longValue());
                    }
                } else if (unit == 2) { // 万元
                    house.setTotalPriceFen(p.multiply(BigDecimal.valueOf(1_000_000L)).longValue());
                    if (house.getArea() != null && house.getArea().compareTo(BigDecimal.ZERO) > 0) {
                        house.setUnitPriceFen(p.multiply(BigDecimal.valueOf(1_000_000L))
                                .divide(house.getArea(), 0, RoundingMode.HALF_UP).longValue());
                    }
                } else if (unit == 3) { // 元/月
                    house.setRentPriceFen(p.multiply(BigDecimal.valueOf(100)).intValue());
                }
            }
        }

        // 4. 最终兜底：如果具备单价和面积但总价为空，则补齐总价（针对新房单价录入场景）
        if (house.getTotalPriceFen() == null && house.getUnitPriceFen() != null 
                && house.getArea() != null && house.getArea().compareTo(BigDecimal.ZERO) > 0) {
            house.setTotalPriceFen(BigDecimal.valueOf(house.getUnitPriceFen())
                    .multiply(house.getArea()).longValue());
        }

        // 5. 保存主表
        boolean success = this.saveOrUpdate(house);
        if (!success) return false;

        // 5. 处理图片持久化
        // (1) 如果是更新操作，先清理旧图片关联
        if (saveDTO.getId() != null) {
            houseImageService.remove(new LambdaQueryWrapper<HouseImage>().eq(HouseImage::getHouseId, house.getId()));
        }
        
        // (2) 保存新图片
        if (cn.hutool.core.collection.CollUtil.isNotEmpty(saveDTO.getImages())) {
            String prefix = fileProperties.getPrefix() + "/";
            List<HouseImageSaveDTO> imageDtos = saveDTO.getImages();
            List<HouseImage> images = new ArrayList<>();
            
            for (int i = 0; i < imageDtos.size(); i++) {
                HouseImageSaveDTO imgDto = imageDtos.get(i);
                HouseImage img = new HouseImage();
                img.setHouseId(house.getId());
                
                // 相对路径处理：确保存入数据库的是纯粹的相对路径（不含 /api/profile 等前缀）
                String rawPath = cn.hutool.core.util.StrUtil.isNotBlank(imgDto.getUrl()) ? imgDto.getUrl() : imgDto.getFileKey();
                if (cn.hutool.core.util.StrUtil.isNotBlank(rawPath)) {
                    String cleanPath = rawPath;
                    if (cleanPath.startsWith(prefix)) {
                        cleanPath = cleanPath.substring(prefix.length());
                    } else if (cleanPath.startsWith("/api/profile/")) {
                        cleanPath = cleanPath.substring(13);
                    } else if (cleanPath.startsWith("/profile/")) {
                        cleanPath = cleanPath.substring(9);
                    } else if (cleanPath.startsWith("profile/")) {
                        cleanPath = cleanPath.substring(8);
                    } else if (cleanPath.contains("/v1/common/download?path=")) {
                        // 处理通过下载接口访问的私有文件 URL
                        cleanPath = cleanPath.substring(cleanPath.indexOf("download?path=") + 14);
                        try {
                            cleanPath = java.net.URLDecoder.decode(cleanPath, "UTF-8");
                        } catch (Exception ignored) {}
                    }
                    img.setFileKey(cleanPath);
                }
                
                // 状态与元数据
                Integer userId = SecurityUtils.getUserId();
                img.setSortOrder(i + 1);
                img.setIsDefault(i == 0 ? (byte) 1 : null); // 第一张作为封面(1)，其余为 null 以避开唯一索引冲突
                img.setImageType(imgDto.getImageType() != null ? imgDto.getImageType() : (i == 0 ? (byte) 1 : (byte) 2));
                img.setUploadUserId(userId != null ? userId : 1); 
                img.setAuditStatus((byte) 1);
                img.setCreateTime(LocalDateTime.now());
                images.add(img);
            }
            houseImageService.saveBatch(images);
        }

        // 6. 处理坐标更新
        Double lng = saveDTO.getLongitude();
        Double lat = saveDTO.getLatitude();
        
        // 如果 DTO 中没传，则从项目继承
        if (lng == null || lat == null) {
            lng = project.getLongitude() != null ? project.getLongitude().doubleValue() : null;
            lat = project.getLatitude() != null ? project.getLatitude().doubleValue() : null;
        }

        if (lng != null && lat != null) {
            String wkt = String.format("POINT(%f %f)", lat, lng);
            this.update().setSql("coordinate = ST_GeomFromText('" + wkt + "', 4326)")
                    .eq("id", house.getId())
                    .update();
        }

        // 7. 处理扩展表
        saveExtensions(house.getId(), saveDTO);

        // 发布实时同步事件
        publishHouseSyncEvent(house.getId(), null);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BolasGuard(entityClass = House.class, userField = "salesId")
    public Boolean updateStatus(Integer id, Byte status, Byte expectedStatus, String reason) {
        House house = this.getById(id);
        if (house == null) throw new ApiException("房源不存在");

        Byte oldStatus = house.getStatus();
        
        // 并发控制：使用条件更新（乐观锁）
        boolean success = this.update()
                .set("status", status)
                .set("update_time", LocalDateTime.now())
                .eq("id", id)
                .eq(expectedStatus != null, "status", expectedStatus)
                .update();

        if (!success) {
            if (expectedStatus != null) {
                throw new ApiException("房源状态已变更，操作失败");
            }
            return false;
        }

        // 记录日志
        HouseStatusLog log = new HouseStatusLog();
        log.setHouseId(id);
        log.setFromStatus(oldStatus);
        log.setToStatus(status);
        log.setChangeReason(reason);
        log.setOperatorId(SecurityUtils.getUserId() != null ? SecurityUtils.getUserId() : 0);
        log.setOperatorName(SecurityUtils.getUsername() != null ? SecurityUtils.getUsername() : "System");
        log.setCreateTime(LocalDateTime.now());
        houseStatusLogService.save(log);

        // 发布实时同步事件
        publishHouseSyncEvent(id, null);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BolasGuard(entityClass = House.class, userField = "salesId")
    public Boolean auditHouse(HouseAuditDTO auditDTO) {
        House house = this.getById(auditDTO.getId());
        if (house == null) {
            throw new ApiException("房源不存在");
        }

        // 预定 (2) 和 已成交 (3) 状态不允许手动变更
        if (house.getStatus() == 2 || house.getStatus() == 3) {
            throw new ApiException("已预订或已成交房源不允许手动变更状态");
        }

        Integer oldStatus = Integer.valueOf(house.getStatus());
        // 更新状态
        house.setStatus(auditDTO.getAuditStatus());
        
        boolean success = this.updateById(house);

        if (success) {
            // 记录变更日志
            HouseStatusLog logEntity = new HouseStatusLog();
            logEntity.setHouseId(auditDTO.getId());
            logEntity.setFromStatus(oldStatus.byteValue());
            logEntity.setToStatus(auditDTO.getAuditStatus());
            logEntity.setChangeReason(auditDTO.getReason());
            logEntity.setOperatorId(SecurityUtils.getUserId() != null ? SecurityUtils.getUserId() : 0);
            logEntity.setOperatorName(SecurityUtils.getUsername() != null ? SecurityUtils.getUsername() : "System");
            logEntity.setCreateTime(LocalDateTime.now());
            houseStatusLogService.save(logEntity);
            
            // 发布实时同步事件
            publishHouseSyncEvent(auditDTO.getId(), null);
        }

        return success;
    }

    private void saveExtensions(Integer houseId, HouseSaveDTO saveDTO) {
        if (saveDTO.getHouseType() == 1 && saveDTO.getNewHouseExtend() != null) {
            NewHouseExtend extend = BeanUtil.copyProperties(saveDTO.getNewHouseExtend(), NewHouseExtend.class);
            extend.setHouseId(houseId);
            NewHouseExtend existing = newHouseExtendService.getOne(new LambdaQueryWrapper<NewHouseExtend>().eq(NewHouseExtend::getHouseId, houseId));
            if (existing != null) extend.setId(existing.getId());
            newHouseExtendService.saveOrUpdate(extend);
        } else if (saveDTO.getHouseType() == 2 && saveDTO.getSecondHouseExtend() != null) {
            SecondHouseExtend extend = BeanUtil.copyProperties(saveDTO.getSecondHouseExtend(), SecondHouseExtend.class);
            extend.setHouseId(houseId);
            SecondHouseExtend existing = secondHouseExtendService.getOne(new LambdaQueryWrapper<SecondHouseExtend>().eq(SecondHouseExtend::getHouseId, houseId));
            if (existing != null) extend.setId(existing.getId());
            secondHouseExtendService.saveOrUpdate(extend);
        } else if (saveDTO.getHouseType() == 3 && saveDTO.getRentHouseExtend() != null) {
            RentHouseExtend extend = BeanUtil.copyProperties(saveDTO.getRentHouseExtend(), RentHouseExtend.class);
            extend.setHouseId(houseId);
            RentHouseExtend existing = rentHouseExtendService.getOne(new LambdaQueryWrapper<RentHouseExtend>().eq(RentHouseExtend::getHouseId, houseId));
            if (existing != null) extend.setId(existing.getId());
            rentHouseExtendService.saveOrUpdate(extend);
        }
    }

    @Override
    public HouseVO getHouseDetail(Integer id) {
        House house = this.getById(id);
        if (house == null) return null;
        
        HouseVO vo = new HouseVO();
        vo.setHouse(house);
        
        // 填充展示字段 (同时填充到 house 对象和 vo 对象中，双重保障)
        fillDisplayPrices(house);
        vo.setPrice(house.getPrice());
        vo.setPriceUnit(house.getPriceUnit());
        vo.setUnitPrice(house.getUnitPrice());

        // 填充专属顾问的昵称/真实姓名和头像
        if (house.getSalesId() != null) {
            List<Map<String, Object>> userList = baseMapper.selectUserNames(Collections.singletonList(house.getSalesId()));
            if (userList != null && !userList.isEmpty()) {
                Map<String, Object> user = userList.get(0);
                vo.setSalesName((String) user.get("real_name"));
                vo.setSalesAvatar(getFullUrl((String) user.get("avatar")));
            }
        }
        
        // 填充扩展信息（根据类型）
        if (house.getHouseType() == 1) {
            vo.setNewHouseExtend(newHouseExtendService.getOne(new LambdaQueryWrapper<NewHouseExtend>().eq(NewHouseExtend::getHouseId, id)));
        } else if (house.getHouseType() == 2) {
            vo.setSecondHouseExtend(secondHouseExtendService.getOne(new LambdaQueryWrapper<SecondHouseExtend>().eq(SecondHouseExtend::getHouseId, id)));
        } else if (house.getHouseType() == 3) {
            vo.setRentHouseExtend(rentHouseExtendService.getOne(new LambdaQueryWrapper<RentHouseExtend>().eq(RentHouseExtend::getHouseId, id)));
        }

        // 填充图片
        List<HouseImage> images = houseImageService.list(new LambdaQueryWrapper<HouseImage>()
                .eq(HouseImage::getHouseId, id)
                .orderByAsc(HouseImage::getSortOrder));
        for (HouseImage img : images) {
            String fullUrl = getFullUrl(img.getFileKey());
            if (fullUrl != null) {
                img.setFileKey(fullUrl);
                img.setUrl(fullUrl);
            }
        }
        vo.setImages(images);

        return vo;
    }
    
    private String getFullUrl(String fileKey) {
        if (StrUtil.isBlank(fileKey)) return null;
        if (fileKey.startsWith("http")) return fileKey;
        
        String prefix = fileProperties.getPrefix();
        if (fileKey.startsWith(prefix + "/")) {
            return fileKey;
        }
        if (fileKey.startsWith(prefix)) {
            return fileKey;
        }
        return prefix + "/" + fileKey;
    }

    @Override
    public HouseStatisticsVO getStatistics() {
        HouseStatisticsVO vo = new HouseStatisticsVO();

        LocalDateTime now = LocalDateTime.now();
        // 本月起始
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        // 上月起始
        LocalDateTime prevMonthStart = monthStart.minusMonths(1);
        // 上月结束
        LocalDateTime prevMonthEnd = monthStart;

        // 1. 在线房源（status = 1 在售）
        long onlineCurrent = lambdaQuery().eq(House::getStatus, (byte) 1).eq(House::getIsDeleted, (byte) 0).count();
        long onlinePrev = lambdaQuery().eq(House::getStatus, (byte) 1).eq(House::getIsDeleted, (byte) 0)
                .lt(House::getCreateTime, prevMonthEnd).count();
        vo.setOnlineCount(onlineCurrent);
        vo.setOnlineTrend(calcTrend(onlineCurrent, onlinePrev));

        // 2. 本月新增
        long monthlyNew = lambdaQuery().eq(House::getIsDeleted, (byte) 0)
                .ge(House::getCreateTime, monthStart).count();
        long monthlyNewPrev = lambdaQuery().eq(House::getIsDeleted, (byte) 0)
                .ge(House::getCreateTime, prevMonthStart).lt(House::getCreateTime, prevMonthEnd).count();
        vo.setMonthlyNewCount(monthlyNew);
        vo.setMonthlyNewTrend(calcTrend(monthlyNew, monthlyNewPrev));

        // 3. 待审核（status = 0）
        long pending = lambdaQuery().eq(House::getStatus, (byte) 0).eq(House::getIsDeleted, (byte) 0).count();
        long pendingPrev = lambdaQuery().eq(House::getStatus, (byte) 0).eq(House::getIsDeleted, (byte) 0)
                .lt(House::getCreateTime, prevMonthEnd).count();
        vo.setPendingReviewCount(pending);
        vo.setPendingReviewTrend(calcTrend(pending, pendingPrev));

        // 4. 成交均价（status = 3 已成交，取 total_price_fen 平均值，转为万元）
        BigDecimal avgDeal = baseMapper.selectAvgTotalPriceFen();
        if (avgDeal != null) {
            avgDeal = avgDeal.divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP);
        } else {
            avgDeal = BigDecimal.ZERO;
        }
        vo.setAvgDealPrice(avgDeal);

        // 上月成交均价（用于趋势）
        BigDecimal avgDealPrev = baseMapper.selectAvgTotalPriceFenBefore(prevMonthEnd);
        double avgDealTrend;
        if (avgDealPrev != null && avgDealPrev.compareTo(BigDecimal.ZERO) > 0) {
            avgDealPrev = avgDealPrev.divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP);
            avgDealTrend = avgDeal.subtract(avgDealPrev)
                    .divide(avgDealPrev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        } else {
            avgDealTrend = 0;
        }
        vo.setAvgDealPriceTrend(avgDealTrend);

        return vo;
    }

    /**
     * 计算环比增长比例
     */
    private double calcTrend(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0;
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 将存储的"分"单位价格换算为前端展示用的 Decimal 单位（万元/元等）
     */
    private void fillDisplayPrices(House house) {
        if (house == null) return;
        
        // 1. 单价换算 (分/㎡ -> 元/㎡)
        if (house.getUnitPriceFen() != null) {
            house.setUnitPrice(BigDecimal.valueOf(house.getUnitPriceFen())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        // 2. 根据房源类型确定展示价格和单位
        // 如果 houseType 为 null，尝试通过其他字段推断（防御性设计）
        Byte type = house.getHouseType();
        
        if (type != null) {
            int typeInt = type.intValue();
            if (typeInt == 1 || typeInt == 2) { // 新房或二手房
                house.setPriceUnit((byte) 2); // 默认万元
                if (house.getTotalPriceFen() != null) {
                    house.setPrice(BigDecimal.valueOf(house.getTotalPriceFen())
                            .divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP));
                }
            } else if (typeInt == 3) { // 租房
                house.setPriceUnit((byte) 3); // 默认元/月
                if (house.getRentPriceFen() != null) {
                    house.setPrice(BigDecimal.valueOf(house.getRentPriceFen())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }
            }
        } else {
            // 如果 houseType 确实丢失了，根据分值字段是否有值来尝试推断并填充单位
            if (house.getRentPriceFen() != null) {
                house.setPriceUnit((byte) 3);
                house.setPrice(BigDecimal.valueOf(house.getRentPriceFen()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            } else if (house.getTotalPriceFen() != null) {
                house.setPriceUnit((byte) 2);
                house.setPrice(BigDecimal.valueOf(house.getTotalPriceFen()).divide(BigDecimal.valueOf(1_000_000L), 2, RoundingMode.HALF_UP));
            }
        }
    }

    @Override
    public void publishHouseSyncEvent(Integer houseId, String customAction) {
        try {
            House house = this.getById(houseId);
            if (house == null) return;
            
            HouseSyncDTO dto = new HouseSyncDTO();
            dto.setEventId(UUID.randomUUID().toString());
            dto.setEventTime(System.currentTimeMillis());
            
            String action = customAction;
            if (action == null) {
                action = "SAVE";
                if ((house.getIsDeleted() != null && house.getIsDeleted() == 1) || 
                    (house.getStatus() != null && (house.getStatus() == 2 || house.getStatus() == 3 || house.getStatus() == 4))) {
                    action = "DELETE";
                }
            }
            dto.setAction(action);
            dto.setHouseId(house.getId());
            dto.setProjectId(house.getProjectId());
            dto.setProjectName(house.getProjectName());
            dto.setCity(house.getCity());
            dto.setDistrict(house.getDistrict());
            
            // 查询第一张图作为封面
            LambdaQueryWrapper<HouseImage> queryWrapper = new LambdaQueryWrapper<HouseImage>()
                    .eq(HouseImage::getHouseId, house.getId())
                    .orderByAsc(HouseImage::getSortOrder)
                    .last("LIMIT 1");
            HouseImage coverImage = houseImageService.getOne(queryWrapper);
            if (coverImage != null) {
                dto.setCoverImage(coverImage.getFileKey());
            } else {
                Project project = projectService.getById(house.getProjectId());
                if (project != null) {
                    dto.setCoverImage(project.getCoverUrl());
                }
            }
            
            dto.setHouseType(house.getHouseType());
            dto.setRoomType(house.getLayout());
            
            String priceText = "";
            if (house.getHouseType() == 3) {
                priceText = house.getRentPriceFen() != null ? (house.getRentPriceFen() / 100) + "元/月" : "价格面议";
            } else {
                priceText = house.getTotalPriceFen() != null ? (house.getTotalPriceFen() / 1_000_000.0) + "万" : 
                            (house.getUnitPriceFen() != null ? (house.getUnitPriceFen() / 100) + "元/㎡" : "价格面议");
            }
            dto.setPriceText(priceText);
            
            dto.setAreaText(house.getArea() != null ? house.getArea() + "㎡" : "");
            dto.setOrientation(house.getOrientation());
            
            String floorInfo = (house.getFloor() != null ? "第" + house.getFloor() + "层" : "") 
                             + (house.getTotalFloor() != null ? "/共" + house.getTotalFloor() + "层" : "");
            dto.setFloorInfo(floorInfo);
            dto.setTags(house.getTags());
            dto.setDescription(house.getDescription());
            
            eventPublisher.publishEvent(new HouseSyncEvent(this, dto));
        } catch (Exception e) {
            logger.error("发布房源同步事件发生异常, houseId={}", houseId, e);
        }
    }
}
