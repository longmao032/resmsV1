package com.guang.aiassistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.guang.aiassistant.model.*;
import com.guang.aiassistant.service.EstateSystemAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 房产系统 API 客户端 — 自动携带 token 调用各接口
 */
@Component
public class EstateSystemClient {

    private static final Logger log = LoggerFactory.getLogger(EstateSystemClient.class);

    private final RestClient restClient;
    private final String listHousesPath;
    private final String customerProfilePath;

    public EstateSystemClient(
            @Value("${remote.estate-system.target-url}") String baseUrl,
            @Value("${remote.estate-system.endpoints.list-houses}") String listHousesPath,
            @Value("${remote.estate-system.endpoints.customer-profile}") String customerProfilePath,
            EstateSystemAuthService authService
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((ClientHttpRequestInterceptor) (request, body, execution) -> {
                    request.getHeaders().setBearerAuth(authService.getToken());
                    return execution.execute(request, body);
                })
                .build();
        this.listHousesPath = listHousesPath;
        this.customerProfilePath = customerProfilePath;
    }

    /** 获取房源列表 */
    public JsonNode listHouses() {
        log.debug("请求房源列表: {}", listHousesPath);
        return restClient.get()
                .uri(listHousesPath)
                .retrieve()
                .body(JsonNode.class);
    }



    // ========== 类型安全的简化接口 ==========

    /** 获取所有楼盘（含项目下房源摘要） */
    public java.util.List<Project> listAllProjects() {
        var json = listHouses();
        return com.guang.aiassistant.model.ProjectListResponse.fromJson(json);
    }



    /** 获取用户意向画像（含行为评分、区县均价指数） */
    public JsonNode getUserProfile(String userId) {
        log.debug("请求用户画像: {}?userId={}", customerProfilePath, userId);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(customerProfilePath)
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }
}
