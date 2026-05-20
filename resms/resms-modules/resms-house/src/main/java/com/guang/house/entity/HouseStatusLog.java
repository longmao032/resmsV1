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
 * 房源状态流转日志表
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("tb_house_status_log")
@Schema(name = "HouseStatusLog", description = "房源状态流转日志表")
public class HouseStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @Schema(description = "日志ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 房源ID
     */
    @TableField("house_id")
    @Schema(description = "房源ID")
    private Integer houseId;

    /**
     * 变更前状态（首次创建时为NULL）
     */
    @TableField("from_status")
    @Schema(description = "变更前状态（首次创建时为NULL）")
    private Byte fromStatus;

    /**
     * 变更后状态
     */
    @TableField("to_status")
    @Schema(description = "变更后状态")
    private Byte toStatus;

    /**
     * 变更原因
     */
    @TableField("change_reason")
    @Schema(description = "变更原因")
    private String changeReason;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private Integer operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    @Schema(description = "操作人姓名")
    private String operatorName;

    /**
     * 操作IP
     */
    @TableField("ip_address")
    @Schema(description = "操作IP")
    private String ipAddress;

    /**
     * 变更时间
     */
    @TableField("create_time")
    @Schema(description = "变更时间")
    private LocalDateTime createTime;
}
