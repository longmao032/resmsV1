package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 浏览历史保存参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "浏览历史保存参数")
public class HistoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID（管理端手动录入时使用，用于查找C端用户）")
    private Integer customerId;

    @NotNull(message = "资源类型不能为空")
    @Schema(description = "资源类型：1=房源，2=项目")
    private Byte resourceType;

    @NotNull(message = "资源ID不能为空")
    @Schema(description = "资源ID")
    private Integer resourceId;

    @Schema(description = "行为类型：view=浏览, call=电话, visit=带看, chat=咨询")
    private String actionType;

    @Schema(description = "交互时长(秒)")
    private Integer duration;

    @Schema(description = "意向评估(1-5星)")
    private Byte interestLevel;

    @Schema(description = "交互备注")
    private String content;

    @Schema(description = "交互时间")
    private LocalDateTime viewTime;
}
