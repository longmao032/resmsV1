package com.guang.portal.domain.dto;
 
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
 
import java.io.Serializable;
 
/**
 * 浏览记录保存参数
 */
@Data
@Schema(description = "浏览记录保存参数")
public class BrowseHistoryDTO implements Serializable {
 
    private static final long serialVersionUID = 1L;
 
    @NotNull(message = "资源类型不能为空")
    @Schema(description = "资源类型：1=房源，2=项目")
    private Byte resourceType;
 
    @NotNull(message = "资源ID不能为空")
    @Schema(description = "资源ID")
    private Integer resourceId;

    @Schema(description = "交互时长(秒)")
    private Integer duration;
}
