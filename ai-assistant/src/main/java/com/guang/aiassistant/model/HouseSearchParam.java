package com.guang.aiassistant.model;

import lombok.Builder;
import lombok.Data;

/**
 * 物理检索强类型参数载体 — 高低层隔离的干净数据契约。
 * 由指挥官层（ChatServiceImpl）统一组装，透传给底层物理检索工具。
 */
@Data
@Builder
public class HouseSearchParam {

    /** 目标城市 */
    private String city;
    /** 目标区县 */
    private String district;
    /** 楼盘/小区项目名 */
    private String projectName;
    /** 房源类型：1新房 2二手房 3租房 */
    private Integer houseType;
    /** 最低总价（万元） */
    private Integer minPrice;
    /** 最高总价（万元） */
    private Integer maxPrice;
    /** 地铁线/站点名 */
    private String subwayLine;
    /** 向量检索专用的模糊自然语言描述 */
    private String semanticQuery;

    /**
     * 判断是否有任何有效的搜索条件。
     */
    public boolean isEmpty() {
        return city == null
                && district == null
                && projectName == null
                && houseType == null
                && minPrice == null
                && maxPrice == null
                && subwayLine == null
                && (semanticQuery == null || semanticQuery.isBlank());
    }

    @Override
    public String toString() {
        return "HouseSearchParam{" +
                "city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", projectName='" + projectName + '\'' +
                ", houseType=" + houseType +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", subwayLine='" + subwayLine + '\'' +
                ", semanticQuery='" + semanticQuery + '\'' +
                '}';
    }
}
