package com.guang.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@TableName("tb_department_target")
@Schema(name = "DeptTarget", description = "部门月度目标表")
public class DeptTarget implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "部门ID")
    @TableField("dept_id")
    private Integer deptId;

    @Schema(description = "目标月份, YYYY-MM")
    @TableField("target_month")
    private String targetMonth;

    @Schema(description = "目标业绩金额（万元）")
    @TableField("target_amount")
    private BigDecimal targetAmount;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
