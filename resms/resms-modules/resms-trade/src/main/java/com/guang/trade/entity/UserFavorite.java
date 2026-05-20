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
 * 用户收藏记录表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_user_favorite")
@Schema(name = "UserFavorite", description = "用户收藏记录表")
public class UserFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    @Schema(description = "收藏ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * C端用户ID
     */
    @TableField("app_user_id")
    @Schema(description = "C端用户ID")
    private Long appUserId;

    /**
     * 目标类型：1=房源，2=楼盘项目
     */
    @TableField("target_type")
    @Schema(description = "目标类型：1=房源，2=楼盘项目")
    private Byte targetType;

    /**
     * 资源ID
     */
    @TableField("target_id")
    @Schema(description = "资源ID")
    private Integer targetId;

    /**
     * 收藏时间
     */
    @TableField("create_time")
    @Schema(description = "收藏时间")
    private LocalDateTime createTime;
}
