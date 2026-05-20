package com.guang.portal.service.impl;

import com.guang.portal.domain.vo.AiChatVO;
import com.guang.portal.domain.vo.HouseItemVO;
import com.guang.portal.service.AiAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final RestClient restClient;

    public AiAssistantServiceImpl(@Value("${ai.assistant.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiChatVO chat(String userId, String message, String sessionId, String city) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("userId", userId);
        body.put("message", message);
        if (sessionId != null && !sessionId.isBlank()) {
            body.put("sessionId", sessionId);
        }
        if (city != null && !city.isBlank()) {
            body.put("city", city);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (result == null) {
            throw new RuntimeException("AI 助手服务返回为空");
        }

        int code = (int) result.getOrDefault("code", 500);
        if (code != 200) {
            String msg = (String) result.getOrDefault("message", "AI 助手服务异常");
            throw new RuntimeException("AI 助手返回错误: " + msg);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            throw new RuntimeException("AI 助手返回数据为空");
        }

        String reply = (String) data.get("reply");
        String aiSessionId = (String) data.get("sessionId");

        // 解析结构化推荐列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) data.get("recommendations");
        List<HouseItemVO> recommendations = new ArrayList<>();
        if (recs != null) {
            for (Map<String, Object> rec : recs) {
                recommendations.add(HouseItemVO.builder()
                        .houseId((String) rec.get("houseId"))
                        .projectName((String) rec.get("projectName"))
                        .price((String) rec.get("price"))
                        .layout((String) rec.get("layout"))
                        .area((String) rec.get("area"))
                        .coverImage((String) rec.get("coverImage"))
                        .build());
            }
        }

        return AiChatVO.builder()
                .reply(reply)
                .sessionId(aiSessionId)
                .recommendations(recommendations)
                .blocks(parseBlocks(reply, recommendations))
                .build();
    }

    /**
     * 从结构化推荐数据构建 ChatBlock 列表。
     * 视图层逻辑集中在前端，后端仅提供干净的块结构。
     */
    private List<AiChatVO.ChatBlock> parseBlocks(String reply, List<HouseItemVO> recommendations) {
        List<AiChatVO.ChatBlock> blocks = new ArrayList<>();

        // 文本块：AI 的暖心导购话术
        if (reply != null && !reply.isBlank()) {
            blocks.add(AiChatVO.ChatBlock.builder()
                    .type("text")
                    .content(reply)
                    .build());
        }

        // 房源卡片块：从结构化数据构建，不依赖文本解析
        if (recommendations != null && !recommendations.isEmpty()) {
            List<String> houseIds = new ArrayList<>();
            for (HouseItemVO rec : recommendations) {
                if (rec.getHouseId() != null && !rec.getHouseId().isBlank()) {
                    houseIds.add(rec.getHouseId());
                }
            }
            if (!houseIds.isEmpty()) {
                blocks.add(AiChatVO.ChatBlock.builder()
                        .type(houseIds.size() == 1 ? "house_card" : "house_cards")
                        .houseIds(houseIds)
                        .build());
            }
        }

        return blocks;
    }
}
