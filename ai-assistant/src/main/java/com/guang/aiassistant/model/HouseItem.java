package com.guang.aiassistant.model;

/**
 * 房源推荐卡片的结构化数据契约 — 前端渲染的唯一数据源。
 * 所有字段均可能为 null，前端需做好兜底展示。
 */
public record HouseItem(
        String houseId,
        String projectName,
        String district,
        String price,
        String layout,
        String area,
        String coverUrl,
        String sellingPoint,
        String explanation
) {
    /** 兼容旧调用：不含 explanation 的 8 参数构造 */
    public HouseItem(String houseId, String projectName, String district,
                     String price, String layout, String area,
                     String coverUrl, String sellingPoint) {
        this(houseId, projectName, district, price, layout, area, coverUrl, sellingPoint, null);
    }
}
