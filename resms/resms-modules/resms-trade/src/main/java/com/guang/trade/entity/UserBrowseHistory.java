package com.guang.trade.entity;

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
 * 用户浏览历史记录表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_user_browse_history")
@Schema(name = "UserBrowseHistory", description = "用户浏览历史记录表")
public class UserBrowseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 浏览ID
     */
    @Schema(description = "浏览ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * C端用户ID
     */
    @TableField("app_user_id")
    @Schema(description = "C端用户ID")
    private Long appUserId;

    /**
     * 资源类型：1=房源，2=项目
     */
    @TableField("resource_type")
    @Schema(description = "资源类型：1=房源，2=项目")
    private Byte resourceType;

    /**
     * 资源ID
     */
    @TableField("resource_id")
    @Schema(description = "资源ID")
    private Integer resourceId;

    /**
     * 行为类型：view=浏览, call=电话, visit=带看, chat=咨询
     */
    @TableField("action_type")
    @Schema(description = "行为类型")
    private String actionType;

    /**
     * 交互时长(秒)
     */
    @TableField("duration")
    @Schema(description = "交互时长(秒)")
    private Integer duration;

    /**
     * 意向评估(1-5星)
     */
    @TableField("interest_level")
    @Schema(description = "意向评估(1-5星)")
    private Byte interestLevel;

    /**
     * 交互备注
     */
    @TableField("content")
    @Schema(description = "交互备注")
    private String content;

    /**
     * 交互时间
     */
    @TableField("view_time")
    @Schema(description = "交互时间")
    private LocalDateTime viewTime;
}
