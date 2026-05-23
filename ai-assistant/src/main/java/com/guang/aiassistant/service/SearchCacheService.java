package com.guang.aiassistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guang.aiassistant.model.RankedHouse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

/**
 * 搜索结果 Redis 缓存 — 避免重复向量查询和二次排序。
 * Key = 搜索参数的 SHA-256 摘要，TTL = 8 分钟。
 */
@Slf4j
@Service
public class SearchCacheService {

    private static final String KEY_PREFIX = "estate:search:";
    private static final Duration TTL = Duration.ofMinutes(8);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SearchCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<RankedHouse>> get(String city, String district, Integer houseType,
                                            Integer minPrice, Integer maxPrice,
                                            String layout, List<String> preferences) {
        String key = buildKey(city, district, houseType, minPrice, maxPrice, layout, preferences);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) return Optional.empty();
            List<Map<String, Object>> raw = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            List<RankedHouse> result = raw.stream().map(this::toRankedHouse).filter(Objects::nonNull).toList();
            if (result.isEmpty()) return Optional.empty();
            log.info("搜索缓存命中: key={}, results={}", key.substring(0, Math.min(20, key.length())), result.size());
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("搜索缓存读取失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String city, String district, Integer houseType,
                    Integer minPrice, Integer maxPrice,
                    String layout, List<String> preferences,
                    List<RankedHouse> results) {
        String key = buildKey(city, district, houseType, minPrice, maxPrice, layout, preferences);
        try {
            List<Map<String, Object>> serializable = results.stream().map(this::fromRankedHouse).toList();
            String json = objectMapper.writeValueAsString(serializable);
            redisTemplate.opsForValue().set(key, json, TTL);
            log.info("搜索缓存写入: key={}, results={}", key.substring(0, Math.min(20, key.length())), results.size());
        } catch (Exception e) {
            log.warn("搜索缓存写入失败: {}", e.getMessage());
        }
    }

    public void invalidateAll() {
        try {
            Set<String> keys = new HashSet<>();
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                var cursor = connection.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match(KEY_PREFIX + "*")
                                .count(100)
                                .build());
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
                cursor.close();
                return null;
            });
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("搜索缓存已清除: {} 个 key", keys.size());
            }
        } catch (Exception e) {
            log.warn("搜索缓存清除失败: {}", e.getMessage());
        }
    }

    // ==================== 序列化 ====================

    private Map<String, Object> fromRankedHouse(RankedHouse rh) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("document", rh.document().getMetadata());
        map.put("content", rh.document().getText());
        map.put("tagScore", rh.tagScore());
        map.put("personaScore", rh.personaScore());
        map.put("semanticScore", rh.semanticScore());
        map.put("priceScore", rh.priceScore());
        map.put("locationScore", rh.locationScore());
        map.put("compositeScore", rh.compositeScore());
        return map;
    }

    @SuppressWarnings("unchecked")
    private RankedHouse toRankedHouse(Map<String, Object> map) {
        try {
            Map<String, Object> meta = (Map<String, Object>) map.get("document");
            String content = (String) map.get("content");
            Document doc = new Document(content != null ? content : "", meta != null ? meta : Map.of());
            return new RankedHouse(doc,
                    ((Number) map.getOrDefault("tagScore", 0)).doubleValue(),
                    ((Number) map.getOrDefault("personaScore", 0)).doubleValue(),
                    ((Number) map.getOrDefault("semanticScore", 0)).doubleValue(),
                    ((Number) map.getOrDefault("priceScore", 0)).doubleValue(),
                    ((Number) map.getOrDefault("locationScore", 0)).doubleValue(),
                    ((Number) map.getOrDefault("compositeScore", 0)).doubleValue());
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Key 生成 ====================

    private String buildKey(String city, String district, Integer houseType,
                            Integer minPrice, Integer maxPrice,
                            String layout, List<String> preferences) {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(s(city)).add(s(district)).add(String.valueOf(houseType))
                .add(String.valueOf(minPrice)).add(String.valueOf(maxPrice)).add(s(layout));
        if (preferences != null) {
            preferences.stream().sorted().forEach(joiner::add);
        }
        return KEY_PREFIX + sha256(joiner.toString()).substring(0, 16);
    }

    private static String s(String v) { return v != null ? v : ""; }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
