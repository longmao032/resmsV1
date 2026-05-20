package com.guang.resmsaiservice.service.impl;

import com.guang.house.entity.House;
import com.guang.house.service.HouseService;
import com.guang.portal.domain.vo.BrowseHistoryItemVO;
import com.guang.portal.domain.vo.FavoriteItemVO;
import com.guang.portal.service.ClientAppointmentService;
import com.guang.portal.service.ClientBrowseHistoryService;
import com.guang.portal.service.ClientFavoriteService;
import com.guang.resmsaiservice.service.UserIntentScoreService;
import com.guang.resmsaiservice.vo.UserIntentProfileVO;
import com.guang.resmsaiservice.vo.UserIntentProfileVO.*;
import com.guang.trade.domain.vo.FollowUpVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户意向评分与画像特征提取 —— 核心算法实现
 * <p>
 * 算法：行为权重（浏览1.0/收藏3.5/预约8.0）× 时间衰减 e^(-0.1t) + 停留加成
 * 特征提取：区域Top2、预算P10-P90、户型众数、单价锚点、场景标签Top5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserIntentScoreServiceImpl implements UserIntentScoreService {

    private static final double WEIGHT_VIEW = 1.0;
    private static final double WEIGHT_FAVORITE = 3.5;
    private static final double WEIGHT_APPOINTMENT = 8.0;
    private static final double DECAY_RATE = 0.1;
    private static final double DURATION_BONUS = 0.5;
    private static final int DURATION_THRESHOLD_SECONDS = 60;
    private static final Pattern ROOM_PATTERN = Pattern.compile("(\\d+)室");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClientBrowseHistoryService browseHistoryService;
    private final ClientFavoriteService favoriteService;
    private final ClientAppointmentService appointmentService;
    private final HouseService houseService;

    @Override
    public UserIntentProfileVO calculateUserProfile(Long appUserId) {
        LocalDateTime now = LocalDateTime.now();

        // ========== 1. 采集行为数据 ==========
        var histories = browseHistoryService.pageHistory(appUserId, 1, 200, null).getRecords();
        var favorites = favoriteService.pageFavorites(appUserId, 1, 200).getRecords();
        var appointments = appointmentService.pageMyAppointments(appUserId, 1, 200).getRecords();

        // ========== 2. 收集所有 houseId，批量加载 House 实体 ==========
        Set<Integer> houseIds = new HashSet<>();
        histories.stream()
                .filter(h -> h.getResourceType() != null && h.getResourceType() == 1)
                .forEach(h -> houseIds.add(h.getResourceId()));
        favorites.forEach(f -> { if (f.getHouseId() != null) houseIds.add(f.getHouseId()); });
        appointments.stream()
                .filter(a -> a.getHouseId() != null)
                .forEach(a -> houseIds.add(a.getHouseId()));

        if (houseIds.isEmpty()) {
            log.info("用户 {} 无有效行为数据，返回空画像", appUserId);
            return buildEmptyProfile(appUserId);
        }

        Map<Integer, House> houseMap = houseService.listByIds(houseIds).stream()
                .collect(Collectors.toMap(House::getId, Function.identity(), (a, b) -> a));

        // ========== 3. 构建统一行为记录 & 计算权重得分 ==========
        List<BehaviorRecord> records = new ArrayList<>();
        Map<Integer, Double> houseScores = new HashMap<>();

        // 浏览
        for (BrowseHistoryItemVO h : histories) {
            if (h.getResourceType() == null || h.getResourceType() != 1) continue;
            House house = houseMap.get(h.getResourceId());
            if (house == null) continue;
            LocalDateTime time = safeParseTime(h.getViewTime());
            int duration = h.getDuration() != null ? h.getDuration() : 0;
            double score = calcScore(WEIGHT_VIEW, time, now, duration);
            records.add(new BehaviorRecord(house, time));
            houseScores.merge(house.getId(), score, Double::sum);
        }

        // 收藏
        for (FavoriteItemVO f : favorites) {
            House house = houseMap.get(f.getHouseId());
            if (house == null) continue;
            LocalDateTime time = safeParseTime(f.getFavoriteTime());
            double score = calcScore(WEIGHT_FAVORITE, time, now, 0);
            records.add(new BehaviorRecord(house, time));
            houseScores.merge(house.getId(), score, Double::sum);
        }

        // 预约
        for (FollowUpVO a : appointments) {
            if (a.getHouseId() == null) continue;
            House house = houseMap.get(a.getHouseId());
            if (house == null) continue;
            LocalDateTime time = a.getCreateTime() != null ? a.getCreateTime() : now;
            double score = calcScore(WEIGHT_APPOINTMENT, time, now, 0);
            records.add(new BehaviorRecord(house, time));
            houseScores.merge(house.getId(), score, Double::sum);
        }

        if (records.isEmpty()) {
            return buildEmptyProfile(appUserId);
        }

        // ========== 4. 特征聚合 ==========
        // 城市
        String city = records.stream()
                .map(r -> r.house.getCity())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 区域 Top2（按意向总得分排序）
        Map<String, Double> districtScores = new HashMap<>();
        for (BehaviorRecord r : records) {
            if (r.house.getDistrict() != null) {
                districtScores.merge(r.house.getDistrict(),
                        houseScores.getOrDefault(r.house.getId(), 0.0), Double::sum);
            }
        }
        List<String> topDistricts = districtScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();

        // 价格区间（P10 / P90）
        List<Double> prices = records.stream()
                .map(r -> housePriceWan(r.house))
                .filter(p -> p != null && p > 0)
                .sorted()
                .toList();
        double minPriceWan = 0, maxPriceWan = 0;
        if (!prices.isEmpty()) {
            int p10 = Math.max(0, (int) Math.ceil(prices.size() * 0.10) - 1);
            int p90 = Math.min(prices.size() - 1, (int) Math.ceil(prices.size() * 0.90) - 1);
            minPriceWan = Math.round(prices.get(p10) * 0.9 * 100.0) / 100.0;
            maxPriceWan = Math.round(prices.get(p90) * 1.05 * 100.0) / 100.0;
        }

        // 面积区间
        List<Double> areas = records.stream()
                .map(r -> r.house.getArea())
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .filter(a -> a > 0)
                .sorted()
                .toList();
        double minArea = 0, maxArea = 0;
        if (!areas.isEmpty()) {
            double avgArea = areas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double stdDev = Math.sqrt(areas.stream().mapToDouble(a -> Math.pow(a - avgArea, 2)).average().orElse(0));
            minArea = Math.max(0, Math.round(avgArea - stdDev));
            maxArea = Math.round(avgArea + stdDev);
        }

        // 户型室数（占比 > 35%）
        Map<Integer, Long> roomCounts = records.stream()
                .map(r -> extractRoomCount(r.house.getLayout()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long totalRoomRecords = roomCounts.values().stream().mapToLong(Long::longValue).sum();
        List<Integer> layoutRooms = roomCounts.entrySet().stream()
                .filter(e -> totalRoomRecords > 0 && (double) e.getValue() / totalRoomRecords > 0.35)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (layoutRooms.isEmpty() && !roomCounts.isEmpty()) {
            layoutRooms = List.of(roomCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(3));
        }

        // 单价锚点 UP_anchor = 平均总价 / 平均面积
        double avgPrice = prices.isEmpty() ? 0 : prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgArea2 = areas.isEmpty() ? 0 : areas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Double unitPriceAnchor = (avgPrice > 0 && avgArea2 > 0) ? Math.round(avgPrice / avgArea2 * 100.0) / 100.0 : null;

        // 软偏好 - 房屋类型
        String preferredHouseType = records.stream()
                .map(r -> r.house.getHouseType())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> switch (e.getKey()) {
                    case 1 -> "new_house";
                    case 2 -> "resale";
                    case 3 -> "rental";
                    default -> "unknown";
                })
                .orElse(null);

        // 软偏好 - 装修标准
        String preferredDecoration = records.stream()
                .map(r -> r.house.getDecoration())
                .filter(Objects::nonNull)
                .filter(d -> !d.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 场景标签 Top5
        Map<String, Long> tagFreq = new HashMap<>();
        for (BehaviorRecord r : records) {
            if (r.house.getTags() != null) {
                for (String tag : r.house.getTags()) {
                    tagFreq.merge(tag, 1L, Long::sum);
                }
            }
        }
        List<String> scenarioTags = tagFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        // 购房紧迫度
        long recentCount = records.stream()
                .filter(r -> r.time != null && ChronoUnit.DAYS.between(r.time, now) <= 3)
                .count();
        String urgency = recentCount >= 5 ? "HIGH" : recentCount >= 2 ? "MEDIUM" : "LOW";

        // 最近活跃时间
        String lastActive = records.stream()
                .map(r -> r.time)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(t -> t.format(DT_FMT))
                .orElse(null);

        // Top5 意向房源
        List<IntentHouseItem> topHouses = houseScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    House h = houseMap.get(e.getKey());
                    if (h == null) return null;
                    return IntentHouseItem.builder()
                            .houseId(h.getId())
                            .title(h.getProjectName() + " " + h.getLayout() + " " + h.getArea() + "㎡")
                            .score(Math.round(e.getValue() * 100.0) / 100.0)
                            .district(h.getDistrict())
                            .area(h.getArea() != null ? h.getArea().doubleValue() : null)
                            .priceWan(housePriceWan(h))
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        // ========== 5. 区县均价指数（方案C：从在售房源聚合） ==========
        Map<String, Double> districtPriceIndex = calculateDistrictPriceIndex(city);

        // ========== 6. 组装画像 ==========
        return UserIntentProfileVO.builder()
                .appUserId(appUserId)
                .lastActiveTime(lastActive)
                .urgencyLevel(urgency)
                .hardConstraints(HardConstraints.builder()
                        .city(city)
                        .districts(topDistricts)
                        .minPriceWan(minPriceWan)
                        .maxPriceWan(maxPriceWan)
                        .minArea(minArea)
                        .maxArea(maxArea)
                        .layoutRooms(layoutRooms)
                        .build())
                .softPreferences(SoftPreferences.builder()
                        .preferredHouseType(preferredHouseType)
                        .preferredDecoration(preferredDecoration)
                        .build())
                .scenarioTags(scenarioTags)
                .unitPriceAnchorWan(unitPriceAnchor)
                .topIntentHouses(topHouses)
                .districtPriceIndex(districtPriceIndex)
                .build();
    }

    // ========== 私有工具方法 ==========

    /**
     * 计算单条行为的加权得分 = (基础分 + 停留加成) × 时间衰减
     */
    private double calcScore(double baseWeight, LocalDateTime behaviorTime, LocalDateTime now, int durationSec) {
        long daysAgo = (behaviorTime != null) ? Math.max(0, ChronoUnit.DAYS.between(behaviorTime, now)) : 30;
        double timeDecay = Math.exp(-DECAY_RATE * daysAgo);
        double bonus = (durationSec > DURATION_THRESHOLD_SECONDS) ? DURATION_BONUS : 0.0;
        return (baseWeight + bonus) * timeDecay;
    }

    /**
     * 从 layout 字符串（如 "3室2厅"）中提取居室数
     */
    private Integer extractRoomCount(String layout) {
        if (layout == null) return null;
        Matcher m = ROOM_PATTERN.matcher(layout);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /**
     * 获取房源总价（万元）
     */
    private Double housePriceWan(House house) {
        if (house.getTotalPriceFen() != null && house.getTotalPriceFen() > 0) {
            return house.getTotalPriceFen() / 1_000_000.0; // 分 → 万元
        }
        // 新房无 totalPriceFen 时，用 unitPriceFen × area 推算
        if (house.getUnitPriceFen() != null && house.getArea() != null) {
            return house.getUnitPriceFen() * house.getArea().doubleValue() / 1_000_000.0;
        }
        return null;
    }

    /**
     * 从在售房源中聚合各区均价指数（万元/㎡）
     */
    private Map<String, Double> calculateDistrictPriceIndex(String city) {
        if (city == null) return Collections.emptyMap();
        List<House> onSaleHouses = houseService.lambdaQuery()
                .eq(House::getCity, city)
                .eq(House::getStatus, (byte) 1)
                .eq(House::getIsDeleted, (byte) 0)
                .isNotNull(House::getUnitPriceFen)
                .list();

        Map<String, Double> result = onSaleHouses.stream()
                .filter(h -> h.getDistrict() != null && h.getUnitPriceFen() != null)
                .collect(Collectors.groupingBy(
                        House::getDistrict,
                        Collectors.averagingDouble(h -> h.getUnitPriceFen() / 1_000_000.0)
                ));

        // 保留两位小数
        result.replaceAll((k, v) -> Math.round(v * 100.0) / 100.0);
        log.info("城市 [{}] 区县均价指数: {}", city, result);
        return result;
    }

    /**
     * 安全解析时间字符串
     */
    private LocalDateTime safeParseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return LocalDateTime.now().minusDays(30);
        try {
            return LocalDateTime.parse(timeStr, DT_FMT);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(timeStr);
            } catch (Exception e2) {
                return LocalDateTime.now().minusDays(30);
            }
        }
    }

    /**
     * 构建空画像（用户无行为数据时）
     */
    private UserIntentProfileVO buildEmptyProfile(Long appUserId) {
        return UserIntentProfileVO.builder()
                .appUserId(appUserId)
                .urgencyLevel("LOW")
                .hardConstraints(HardConstraints.builder().build())
                .softPreferences(SoftPreferences.builder().build())
                .scenarioTags(Collections.emptyList())
                .topIntentHouses(Collections.emptyList())
                .districtPriceIndex(Collections.emptyMap())
                .build();
    }

    /**
     * 统一的行为记录内部类
     */
    private record BehaviorRecord(House house, LocalDateTime time) {}
}
