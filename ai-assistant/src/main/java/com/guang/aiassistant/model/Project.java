package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 楼盘项目
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Project(
        @JsonProperty("id") Long id,
        @JsonProperty("projectName") String projectName,
        @JsonProperty("projectType") Integer projectType,
        @JsonProperty("developer") String developer,
        @JsonProperty("propertyCompany") String propertyCompany,
        @JsonProperty("province") String province,
        @JsonProperty("city") String city,
        @JsonProperty("district") String district,
        @JsonProperty("address") String address,
        @JsonProperty("totalHouseholds") Integer totalHouseholds,
        @JsonProperty("propertyFee") BigDecimal propertyFee,
        @JsonProperty("plotRatio") BigDecimal plotRatio,
        @JsonProperty("greeningRate") Integer greeningRate,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("coverUrl") String coverUrl,
        @JsonProperty("longitude") BigDecimal longitude,
        @JsonProperty("latitude") BigDecimal latitude,
        @JsonProperty("updateTime") LocalDateTime updateTime,
        @JsonProperty("houses") List<ProjectHouse> houses
) {

    /**
     * 项目下的房源摘要
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectHouse(
            @JsonProperty("id") Long id,
            @JsonProperty("houseTitle") String houseTitle,
            @JsonProperty("roomType") String roomType,
            @JsonProperty("areaText") String areaText,
            @JsonProperty("priceText") String priceText,
            @JsonProperty("unitPriceText") String unitPriceText,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("floorInfo") String floorInfo,
            @JsonProperty("orientation") String orientation,
            @JsonProperty("houseType") Integer houseType,
            @JsonProperty("description") String description,
            @JsonProperty("coverUrl") String coverUrl
    ) {}
}
