package com.guang.common.enums;

import lombok.Getter;

/**
 * 文件分类枚举
 *
 * @author blackDuck
 */
@Getter
public enum FileCategory {

    /**
     * 头像: 公共，静态映射，覆盖旧图
     */
    AVATAR("avatar", true, "头像"),

    /**
     * 房源图片: 公共，静态映射，随房源删除
     */
    HOUSE("house", true, "房源图片"),

    /**
     * 楼盘项目图片: 公共，静态映射，随项目删除
     */
    PROJECT("project", true, "楼盘项目图片"),

    /**
     * 支付凭证: 私有，后端流传输，财务审计使用
     */
    PAYMENT("payment", false, "支付凭证"),

    /**
     * 房本图片: 私有，后端流传输，用于审核鉴权
     */
    DEED("house/deed", false, "房本图片"),

    /**
     * 合同附件: 私有，后端流传输，永久保留
     */
    CONTRACT("contract", false, "合同附件"),

    /**
     * 导出报表: 临时，临时下载链接，24h清理
     */
    EXPORT("export", false, "导出报表"),

    /**
     * 过户文件: 私有，后端流传输，包括完税证明、新产权证、受理凭证等
     */
    TRANSFER_DOC("transfer/doc", false, "过户文件"),

    /**
     * 聊天文件/图片: 公共，静态映射，随消息删除
     */
    CHAT("chat", true, "聊天文件/图片"),

    /**
     * 数据库备份: 备份，仅服务器访问，版本保留
     */
    BACKUP_DB("backup/db", false, "数据库备份");

    private final String subDir;
    private final boolean isPublic;
    private final String description;

    FileCategory(String subDir, boolean isPublic, String description) {
        this.subDir = subDir;
        this.isPublic = isPublic;
        this.description = description;
    }
}
