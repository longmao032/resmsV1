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
 *
 * <p>分层加权策略：
 * <ul>
 *   <li>高意向层（收藏+预约）→ 决定画像主方向：区域、预算、类型</li>
 *   <li>低意向层（浏览）→ 仅提供参考修正</li>
 * </ul>
 *
 * <p>抗极端值：价格/面积用截断均值 ± MAD，不受豪宅浏览污染。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserIntentScoreServiceImpl implements UserIntentScoreService {

    // ==================== 行为权重常量 ====================
    private static final double WEIGHT_VIEW_SHORT = 0.3;   // 浏览 <10秒：误点/划过
    private static final double WEIGHT_VIEW_NORMAL = 1.0;   // 浏览 10-60秒：正常浏览
    private static final double WEIGHT_VIEW_DEEP = 2.0;     // 浏览 60-180秒：深度阅读
    private static final double WEIGHT_VIEW_LONG = 3.0;     // 浏览 >180秒：极度感兴趣
    private static final double WEIGHT_FAVORITE = 3.5;      // 收藏
    private static final double WEIGHT_APPOINTMENT = 8.0;   // 预约
    private static final double WEIGHT_COMPLETED = 12.0;    // 预约完成（真实转化）
    private static final double WEIGHT_CANCELLED = 1.0;     // 预约取消（失去兴趣）

    private static final double DECAY_RATE = 0.1;
    private static final int DURATION_SHORT = 10;
    private static final int DURATION_DEEP = 60;
    private static final int DURATION_LONG = 180;
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

        // ========== 3. 构建统一行为记录 & 计算加权得分 ==========
        List<BehaviorRecord> records = new ArrayList<>();
        Map<Integer, Double> houseScores = new HashMap<>();

        // 浏览（按时长细分权重）
        for (BrowseHistoryItemVO h : histories) {
            if (h.getResourceType() == null || h.getResourceType() != 1) continue;
            House house = houseMap.get(h.getResourceId());
            if (house == null) continue;
            LocalDateTime time = safeParseTime(h.getViewTime());
            int duration = h.getDuration() != null ? h.getDuration() : 0;
            double weight = viewWeight(duration);
            double score = calcScore(weight, time, now);
            records.add(new BehaviorRecord(house, time, BehaviorType.VIEW, duration));
            houseScores.merge(house.getId(), score, Double::sum);
        }

        // 收藏
        for (FavoriteItemVO f : favorites) {
            House house = houseMap.get(f.getHouseId());
            if (house == null) continue;
            LocalDateTime time = safeParseTime(f.getFavoriteTime());
            double score = calcScore(WEIGHT_FAVORITE, time, now);
            records.add(new BehaviorRecord(house, time, BehaviorType.FAVORITE, 0));
            houseScores.merge(house.getId(), score, Double::sum);
        }

        // 预约（区分状态）
        for (FollowUpVO a : appointments) {
            if (a.getHouseId() == null) continue;
            House house = houseMap.get(a.getHouseId());
            if (house == null) continue;
            LocalDateTime time = a.getCreateTime() != null ? a.getCreateTime() : now;
            double weight = appointmentWeight(a);
            BehaviorType type = appointmentBehaviorType(a);
            double score = calcScore(weight, time, now);
            records.add(new BehaviorRecord(house, time, type, 0));
            houseScores.merge(house.getId(), score, Double::sum);
        }

        if (records.isEmpty()) {
            return buildEmptyProfile(appUserId);
        }

        // 分层：高意向（收藏+预约）和低意向（浏览）
        List<BehaviorRecord> highIntent = records.stream()
                .filter(r -> r.type != BehaviorType.VIEW)
                .toList();
        List<BehaviorRecord> allRecords = records;
        // 优先用高意向层，不足时回退到全部
        List<BehaviorRecord> primaryRecords = highIntent.isEmpty() ? allRecords : highIntent;

        // ========== 4. 特征聚合 ==========
        // 城市（全量投票）
        String city = allRecords.stream()
                .map(r -> r.house.getCity())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 区域 Top2（行为加权投票：收藏/预约 ×3，浏览 ×1）
        List<String> topDistricts = extractTopDistricts(allRecords);

        // 价格区间（截断均值 + MAD，仅用高意向层）
        double[] priceRange = extractPriceRange(primaryRecords);
        double minPriceWan = priceRange[0];
        double maxPriceWan = priceRange[1];

        // 面积区间（截断均值 + MAD，仅用高意向层）
        double[] areaRange = extractAreaRange(primaryRecords);
        double minArea = areaRange[0];
        double maxArea = areaRange[1];

        // 户型室数（占比 > 35%，仅用高意向层）
        List<Integer> layoutRooms = extractLayoutRooms(primaryRecords);

        // 单价锚点（仅用高意向层的价格和面积）
        Double unitPriceAnchor = extractUnitPriceAnchor(primaryRecords);

        // 软偏好 - 房屋类型（加权投票）
        String preferredHouseType = extractPreferredHouseType(allRecords);

        // 软偏好 - 装修标准（高意向层优先）
        String preferredDecoration = extractDecoration(primaryRecords);

        // 场景标签 Top5（高意向层优先）
        List<String> scenarioTags = extractScenarioTags(primaryRecords);

        // 购房紧迫度
        String urgency = extractUrgency(allRecords, now);

        // 最近活跃时间
        String lastActive = allRecords.stream()
                .map(r -> r.time)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(t -> t.format(DT_FMT))
                .orElse(null);

        // Top5 意向房源
        List<IntentHouseItem> topHouses = extractTopHouses(houseScores, houseMap);

        // ========== 5. 区县均价指数 ==========
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

    // ==================== 行为权重计算 ====================

    /** 按浏览时长细分权重 */
    private double viewWeight(int durationSec) {
        if (durationSec < DURATION_SHORT) return WEIGHT_VIEW_SHORT;
        if (durationSec < DURATION_DEEP) return WEIGHT_VIEW_NORMAL;
        if (durationSec < DURATION_LONG) return WEIGHT_VIEW_DEEP;
        return WEIGHT_VIEW_LONG;
    }

    /** 预约权重按状态区分：0=待确认, 1=已预约, 2=已完成, 3=已取消 */
    private double appointmentWeight(FollowUpVO a) {
        Byte status = a.getStatus();
        if (status == null) return WEIGHT_APPOINTMENT;
        return switch (status) {
            case 2 -> WEIGHT_COMPLETED;   // 已完成
            case 3 -> WEIGHT_CANCELLED;   // 已取消
            default -> WEIGHT_APPOINTMENT; // 待确认/已预约
        };
    }

    private BehaviorType appointmentBehaviorType(FollowUpVO a) {
        Byte status = a.getStatus();
        if (status != null && status == 2) return BehaviorType.COMPLETED;
        if (status != null && status == 3) return BehaviorType.CANCELLED;
        return BehaviorType.APPOINTMENT;
    }

    // ==================== 区域提取（行为加权投票） ====================

    private List<String> extractTopDistricts(List<BehaviorRecord> records) {
        Map<String, Double> districtVotes = new HashMap<>();
        for (BehaviorRecord r : records) {
            if (r.house.getDistrict() == null) continue;
            // 高意向行为票权 ×3
            double vote = (r.type == BehaviorType.VIEW) ? 1.0 : 3.0;
            districtVotes.merge(r.house.getDistrict(), vote, Double::sum);
        }
        return districtVotes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();
    }

    // ==================== 价格区间（截断均值 + MAD） ====================

    private double[] extractPriceRange(List<BehaviorRecord> records) {
        List<Double> prices = records.stream()
                .map(r -> housePriceWan(r.house))
                .filter(p -> p != null && p > 0)
                .sorted()
                .toList();

        if (prices.isEmpty()) return new double[]{0, 0};
        if (prices.size() == 1) {
            double p = prices.get(0);
            return new double[]{Math.round(p * 0.8), Math.round(p * 1.2)};
        }

        double trimmedMean = trimmedMean(prices, 0.10);
        double mad = medianAbsoluteDeviation(prices, trimmedMean);
        // MAD=0 时（数据过于集中），用均值的 15% 作为浮动
        double spread = mad > 0 ? mad * 1.5 : trimmedMean * 0.15;
        double min = Math.max(0, Math.round((trimmedMean - spread) * 100.0) / 100.0);
        double max = Math.round((trimmedMean + spread) * 100.0) / 100.0;

        log.info("价格计算: trimmedMean={}, MAD={}, range={}-{}, 原始数据量={}",
                String.format("%.0f", trimmedMean), String.format("%.0f", mad),
                String.format("%.0f", min), String.format("%.0f", max), prices.size());
        return new double[]{min, max};
    }

    // ==================== 面积区间（截断均值 + MAD） ====================

    private double[] extractAreaRange(List<BehaviorRecord> records) {
        List<Double> areas = records.stream()
                .map(r -> r.house.getArea())
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .filter(a -> a > 0)
                .sorted()
                .toList();

        if (areas.isEmpty()) return new double[]{0, 0};
        if (areas.size() == 1) {
            double a = areas.get(0);
            return new double[]{Math.round(a * 0.8), Math.round(a * 1.2)};
        }

        double trimmedMean = trimmedMean(areas, 0.10);
        double mad = medianAbsoluteDeviation(areas, trimmedMean);
        double spread = mad > 0 ? mad * 1.5 : trimmedMean * 0.15;
        double min = Math.max(0, Math.round(trimmedMean - spread));
        double max = Math.round(trimmedMean + spread);

        log.info("面积计算: trimmedMean={}, MAD={}, range={}-{}, 原始数据量={}",
                String.format("%.0f", trimmedMean), String.format("%.0f", mad), min, max, areas.size());
        return new double[]{min, max};
    }

    // ==================== 户型提取 ====================

    private List<Integer> extractLayoutRooms(List<BehaviorRecord> records) {
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
        return layoutRooms;
    }

    // ==================== 单价锚点（仅高意向层） ====================

    private Double extractUnitPriceAnchor(List<BehaviorRecord> records) {
        List<Double> prices = records.stream()
                .map(r -> housePriceWan(r.house))
                .filter(p -> p != null && p > 0)
                .toList();
        List<Double> areas = records.stream()
                .map(r -> r.house.getArea())
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .filter(a -> a > 0)
                .toList();

        if (prices.isEmpty() || areas.isEmpty()) return null;
        double avgPrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgArea = areas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return (avgPrice > 0 && avgArea > 0) ? Math.round(avgPrice / avgArea * 100.0) / 100.0 : null;
    }

    // ==================== 房屋类型偏好（加权投票） ====================

    private String extractPreferredHouseType(List<BehaviorRecord> records) {
        Map<Byte, Double> typeScores = new HashMap<>();
        for (BehaviorRecord r : records) {
            Byte type = r.house.getHouseType();
            if (type == null) continue;
            double weight = switch (r.type) {
                case VIEW -> 1.0;
                case FAVORITE -> 3.5;
                case APPOINTMENT -> 8.0;
                case COMPLETED -> 12.0;
                case CANCELLED -> 0.5;
            };
            typeScores.merge(type, weight, Double::sum);
        }
        return typeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> switch (e.getKey()) {
                    case 1 -> "new_house";
                    case 2 -> "resale";
                    case 3 -> "rental";
                    default -> "unknown";
                })
                .orElse(null);
    }

    // ==================== 装修标准（高意向优先） ====================

    private String extractDecoration(List<BehaviorRecord> records) {
        return records.stream()
                .map(r -> r.house.getDecoration())
                .filter(Objects::nonNull)
                .filter(d -> !d.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ==================== 场景标签（高意向优先） ====================

    private List<String> extractScenarioTags(List<BehaviorRecord> records) {
        Map<String, Long> tagFreq = new HashMap<>();
        for (BehaviorRecord r : records) {
            if (r.house.getTags() != null) {
                for (String tag : r.house.getTags()) {
                    tagFreq.merge(tag, 1L, Long::sum);
                }
            }
        }
        return tagFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    // ==================== 紧迫度 ====================

    private String extractUrgency(List<BehaviorRecord> records, LocalDateTime now) {
        long recentCount = records.stream()
                .filter(r -> r.time != null && ChronoUnit.DAYS.between(r.time, now) <= 3)
                .count();
        // 高意向行为加权：收藏/预约算 2 次
        long weightedRecent = records.stream()
                .filter(r -> r.time != null && ChronoUnit.DAYS.between(r.time, now) <= 3)
                .mapToLong(r -> r.type == BehaviorType.VIEW ? 1 : 2)
                .sum();
        return weightedRecent >= 5 ? "HIGH" : weightedRecent >= 2 ? "MEDIUM" : "LOW";
    }

    // ==================== Top5 意向房源 ====================

    private List<IntentHouseItem> extractTopHouses(Map<Integer, Double> houseScores, Map<Integer, House> houseMap) {
        return houseScores.entrySet().stream()
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
    }

    // ==================== 统计工具方法 ====================

    /**
     * 截断均值：去掉首尾 trimRatio 比例后取均值。
     * 例如 trimRatio=0.10 → 去掉最高 10% 和最低 10%，用中间 80% 的均值。
     */
    private double trimmedMean(List<Double> sorted, double trimRatio) {
        int n = sorted.size();
        int trim = (int) Math.floor(n * trimRatio);
        if (n - 2 * trim <= 0) {
            // 数据太少，退化为普通均值
            return sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }
        return sorted.subList(trim, n - trim).stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0);
    }

    /**
     * 中位绝对偏差（MAD）：median(|xi - median(x)|)。
     * 比标准差对离群值更鲁棒。
     */
    private double medianAbsoluteDeviation(List<Double> sorted, double center) {
        List<Double> deviations = sorted.stream()
                .map(v -> Math.abs(v - center))
                .sorted()
                .toList();
        return median(deviations);
    }

    private double median(List<Double> sorted) {
        int n = sorted.size();
        if (n == 0) return 0;
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    // ==================== 原有工具方法 ====================

    private double calcScore(double baseWeight, LocalDateTime behaviorTime, LocalDateTime now) {
        long daysAgo = (behaviorTime != null) ? Math.max(0, ChronoUnit.DAYS.between(behaviorTime, now)) : 30;
        double timeDecay = Math.exp(-DECAY_RATE * daysAgo);
        return baseWeight * timeDecay;
    }

    private Integer extractRoomCount(String layout) {
        if (layout == null) return null;
        Matcher m = ROOM_PATTERN.matcher(layout);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private Double housePriceWan(House house) {
        if (house.getTotalPriceFen() != null && house.getTotalPriceFen() > 0) {
            return house.getTotalPriceFen() / 1_000_000.0;
        }
        if (house.getUnitPriceFen() != null && house.getArea() != null) {
            return house.getUnitPriceFen() * house.getArea().doubleValue() / 1_000_000.0;
        }
        return null;
    }

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

        result.replaceAll((k, v) -> Math.round(v * 100.0) / 100.0);
        log.info("城市 [{}] 区县均价指数: {}", city, result);
        return result;
    }

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

    // ==================== 内部类型 ====================

    private enum BehaviorType { VIEW, FAVORITE, APPOINTMENT, COMPLETED, CANCELLED }

    private record BehaviorRecord(House house, LocalDateTime time, BehaviorType type, int duration) {}
}
