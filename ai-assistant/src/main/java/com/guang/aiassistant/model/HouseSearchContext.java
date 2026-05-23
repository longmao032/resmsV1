package com.guang.aiassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 多轮对话持久上下文 — Java 状态机跨轮维护的确定性过滤参数。
 * 可序列化至 Redis，承载城市/区域/楼盘/户型/地铁的逐轮锁定与继承。
 *
 * <p>与 {@code UserIntentForm} 职责正交：
 * <ul>
 *   <li>{@code UserIntentForm} — LLM 单轮无状态瞬时快照</li>
 *   <li>{@code HouseSearchContext} — Java 状态机跨轮持久锁定上下文</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HouseSearchContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前锁定的目标城市 */
    private String city;
    /** 当前锁定的区县 */
    private String district;
    /** 当前锁定的楼盘/小区项目名 */
    private String project;
    /** 当前锁定的房源类型：1新房 2二手房 3租房 */
    private Integer houseType;
    /** 当前锁定的地铁线/站点名 */
    private String subwayLine;

    public HouseSearchContext() {}

    // ==================== 确定性清洗行为 ====================

    /**
     * 城市变更时一键清洗：强制清空旧城市遗留的区县、楼盘、地铁，
     * 彻底防止跨城数据粘连污染。
     *
     * @param newCity 新城市名，不可为 null
     * @throws IllegalArgumentException 如果 newCity 为 null 或空白
     */
    public void changeCity(String newCity) {
        if (newCity == null || newCity.isBlank()) {
            throw new IllegalArgumentException("newCity 不能为空");
        }
        if (!Objects.equals(this.city, newCity)) {
            this.city = newCity;
            this.district = null;
            this.project = null;
            this.subwayLine = null;
            // houseType 是跨城通用属性，不清洗
        }
    }

    /**
     * 聚焦到具体楼盘时：自动清空模糊的地铁线约束，
     * 防止底层 Hybrid 搜索时楼盘限定与地铁限定条件互斥导致零命中。
     *
     * @param newProject 新楼盘/小区项目名，不可为 null
     * @throws IllegalArgumentException 如果 newProject 为 null 或空白
     */
    public void updateProject(String newProject) {
        if (newProject == null || newProject.isBlank()) {
            throw new IllegalArgumentException("newProject 不能为空");
        }
        if (!Objects.equals(this.project, newProject)) {
            this.project = newProject;
            this.subwayLine = null;  // 聚焦楼盘后地铁约束失效
            // city / district / houseType 不受影响
        }
    }

    /**
     * 判断当前上下文是否处于"空状态"——没有任何锁定的搜索参数。
     */
    @JsonIgnore
    public boolean isEmpty() {
        return city == null
                && district == null
                && project == null
                && houseType == null
                && subwayLine == null;
    }

    // ==================== Getters / Setters ====================

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public Integer getHouseType() { return houseType; }
    public void setHouseType(Integer houseType) { this.houseType = houseType; }

    public String getSubwayLine() { return subwayLine; }
    public void setSubwayLine(String subwayLine) { this.subwayLine = subwayLine; }

    @Override
    public String toString() {
        return "HouseSearchContext{" +
                "city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", project='" + project + '\'' +
                ", houseType=" + houseType +
                ", subwayLine='" + subwayLine + '\'' +
                '}';
    }
}
