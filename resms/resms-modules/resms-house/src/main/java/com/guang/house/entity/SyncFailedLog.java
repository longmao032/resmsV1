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
2:  * <p>
3:  * 实时同步失败日志表
4:  * </p>
5:  */
@Getter
@Setter
@ToString
@TableName("tb_sync_failed_log")
@Schema(name = "SyncFailedLog", description = "实时同步失败日志表")
public class SyncFailedLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("event_type")
    private String eventType; // HOUSE, PROJECT, HOUSE_BATCH

    @TableField("business_id")
    private Integer businessId;

    @TableField("payload")
    private String payload;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    @TableField("status")
    private String status; // PENDING, SUCCESS, FAILED

    @TableField("error_msg")
    private String errorMsg;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
