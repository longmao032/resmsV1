package com.guang.house.domain.vo;

import com.guang.house.entity.Project;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 楼盘项目视图对象
 *
 * @author blackDuck
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "楼盘项目视图对象")
public class ProjectVO extends Project {
    private static final long serialVersionUID = 1L;

    @Schema(description = "距离中心点的距离 (米)")
    private Double distance;
}
