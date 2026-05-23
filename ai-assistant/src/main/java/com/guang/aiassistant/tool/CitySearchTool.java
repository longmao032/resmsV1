package com.guang.aiassistant.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CitySearchTool {

    private static final Logger log = LoggerFactory.getLogger(CitySearchTool.class);

    private static final Map<String, CityData> CITIES = new LinkedHashMap<>();

    @PostConstruct
    void loadCities() {
        try (InputStream in = new ClassPathResource("static/city.json").getInputStream()) {
            Map<String, CityData> data = new ObjectMapper().readValue(in,
                    new TypeReference<Map<String, CityData>>() {});
            CITIES.putAll(data);
            log.info("已加载 {} 个城市/地区数据", CITIES.size());
        } catch (Exception e) {
            log.error("加载 city.json 失败", e);
        }
    }

    public String searchCities(
            String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "请输入城市名关键词。";
        }
        String kw = keyword.trim();
        List<Map.Entry<String, CityData>> matches = CITIES.entrySet().stream()
                .filter(e -> matchesCity(e.getKey(), e.getValue(), kw))
                .limit(20)
                .toList();

        if (matches.isEmpty()) {
            return "未找到匹配的城市，请尝试其他关键词。";
        }
        return matches.stream()
                .map(e -> formatCityResult(e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n---\n"));
    }

    public String listDistricts(
            String city) {
        if (city == null || city.isBlank()) {
            return "请输入城市名称。";
        }
        String inputName = city.trim();
        String cityName = inputName;
        CityData data = CITIES.get(cityName);
        if (data == null) {
            // 模糊匹配
            cityName = CITIES.keySet().stream()
                    .filter(k -> k.contains(inputName) || inputName.contains(k))
                    .findFirst().orElse(null);
            if (cityName == null) {
                return String.format("未找到城市'%s'，请使用 searchCities 先检索城市。", inputName);
            }
            data = CITIES.get(cityName);
        }
        if (data.districts().isEmpty()) {
            return String.format("%s暂无区县数据。", cityName);
        }
        return String.format("%s下辖区县（%d个）：\n%s",
                cityName, data.districts().size(), String.join("\n", data.districts()));
    }

    public String validateCityDistrict(
            String city,
            String district) {
        if (city == null || city.isBlank()) {
            return "请输入城市名称。";
        }
        String kw = city.trim();
        Map.Entry<String, CityData> match = CITIES.entrySet().stream()
                .filter(e -> matchesCity(e.getKey(), e.getValue(), kw))
                .findFirst().orElse(null);
        if (match == null) {
            return String.format("未找到匹配'%s'的城市。", kw);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("城市：%s\n", match.getKey()));
        result.append(String.format("下辖区县数：%d\n", match.getValue().districts().size()));

        if (district != null && !district.isBlank()) {
            String dkw = district.trim();
            List<String> matchedDistricts = match.getValue().districts().stream()
                    .filter(d -> d.contains(dkw) || dkw.contains(d))
                    .toList();
            if (matchedDistricts.isEmpty()) {
                result.append(String.format("区县'%s'未匹配，请检查名称或列出区县确认。", dkw));
            } else {
                result.append(String.format("匹配区县：%s", String.join("、", matchedDistricts)));
            }
        }
        return result.toString();
    }

    private boolean matchesCity(String name, CityData data, String keyword) {
        if (name.contains(keyword) || keyword.contains(name)) return true;
        if (data.aliases() == null) return false;
        return data.aliases().values().stream().anyMatch(a -> a.contains(keyword));
    }

    private String formatCityResult(String name, CityData data) {
        return String.format("【%s】下辖区县（%d个）：%s",
                name, data.districts().size(), String.join("、", data.districts()));
    }

    /**
     * 城市数据 — 对应 city.json 中每个 city 的结构
     */
    public record CityData(
            List<String> districts,
            Map<String, String> aliases
    ) {
        public CityData {
            districts = districts != null ? Collections.unmodifiableList(districts) : Collections.emptyList();
            aliases = aliases != null ? Collections.unmodifiableMap(aliases) : Collections.emptyMap();
        }
    }
}
