package com.guang.house.domain.vo;

import com.guang.house.entity.House;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房源分页列表视图对象
 *
 * @author blackDuck
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "房源分页列表视图对象")
public class HousePageVO extends House {

    private static final long serialVersionUID = 1L;

    @Schema(description = "距离中心点的距离 (米)")
    private Double distance;

    @Schema(description = "封面图URL")
    private String coverUrl;

    @Schema(description = "负责销售姓名")
    private String salesName;

    @Schema(description = "负责销售头像URL")
    private String salesAvatar;
}
