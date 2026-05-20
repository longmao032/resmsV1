package com.guang.portal.domain.dto;
 
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
 
import java.io.Serializable;
import java.time.LocalDateTime;
 
/**
 * 预约看房保存参数
 */
@Data
@Schema(description = "预约看房保存参数")
public class AppointmentSaveDTO implements Serializable {
 
    private static final long serialVersionUID = 1L;
 
    @NotNull(message = "房源ID不能为空")
    @Schema(description = "房源ID")
    private Integer houseId;
 
    @NotNull(message = "预约时间不能为空")
    @Future(message = "预约时间必须是将来时间")
    @Schema(description = "预约时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime viewTime;
 
    @Schema(description = "备注/需求")
    private String remark;
}
