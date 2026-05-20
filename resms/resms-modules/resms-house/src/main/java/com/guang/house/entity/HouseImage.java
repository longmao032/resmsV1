package com.guang.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 房源图片表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_house_image")
@Schema(name = "HouseImage", description = "房源图片表")
public class HouseImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    @Schema(description = "图片ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 房源ID
     */
    @TableField("house_id")
    @Schema(description = "房源ID")
    private Integer houseId;

    /**
     * 文件存储Key/相对路径（如 house/FC001/cover.jpg），由后端拼接BaseURL
     */
    @TableField("file_key")
    @Schema(description = "文件存储Key/相对路径（如 house/FC001/cover.jpg），由后端拼接BaseURL")
    private String fileKey;

    /**
     * 缩略图Key/相对路径
     */
    @TableField("thumbnail_key")
    @Schema(description = "缩略图Key/相对路径")
    private String thumbnailKey;

    /**
     * 图片类型：1=封面图，2=室内图，3=户型图，4=环境图，5=其他
     */
    @TableField("image_type")
    @Schema(description = "图片类型：1=封面图，2=室内图，3=户型图，4=环境图，5=其他")
    private Byte imageType;

    /**
     * 图片分组，如客厅、卧室、厨房等
     */
    @TableField("image_group")
    @Schema(description = "图片分组，如客厅、卧室、厨房等")
    private String imageGroup;

    /**
     * 标签，如[\"朝南\",\"带阳台\"]
     */
    @TableField("tags")
    @Schema(description = "标签，如[\"朝南\",\"带阳台\"]")
    private String tags;

    /**
     * 排序
     */
    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;

    /**
     * 是否默认首图：0=否，1=是（每个房源有且仅有一张）
     */
    @TableField("is_default")
    @Schema(description = "是否默认首图：0=否，1=是（每个房源有且仅有一张）")
    private Byte isDefault;

    /**
     * 文件大小(KB)
     */
    @TableField("file_size")
    @Schema(description = "文件大小(KB)")
    private Integer fileSize;

    /**
     * 图片宽度(px)
     */
    @TableField("width")
    @Schema(description = "图片宽度(px)")
    private Integer width;

    /**
     * 图片高度(px)
     */
    @TableField("height")
    @Schema(description = "图片高度(px)")
    private Integer height;

    /**
     * 上传人ID
     */
    @TableField("upload_user_id")
    @Schema(description = "上传人ID")
    private Integer uploadUserId;

    /**
     * 审核状态：0=待审核，1=通过，2=驳回
     */
    @TableField("audit_status")
    @Schema(description = "审核状态：0=待审核，1=通过，2=驳回")
    private Byte auditStatus;

    /**
     * 审核人ID
     */
    @TableField("audit_user_id")
    @Schema(description = "审核人ID")
    private Integer auditUserId;

    /**
     * 审核时间
     */
    @TableField("audit_time")
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    /**
     * 审核备注
     */
    @TableField("audit_remark")
    @Schema(description = "审核备注")
    private String auditRemark;

    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 完整访问URL（非DB字段，由后端拼接 prefix + fileKey 后填充）
     */
    @TableField(exist = false)
    private String url;
}
