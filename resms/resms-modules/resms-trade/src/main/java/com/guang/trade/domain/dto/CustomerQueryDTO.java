package com.guang.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户查询参数
 *
 * @author blackDuck
 */
@Data
@Schema(description = "客户查询参数")
public class CustomerQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "客户姓名")
    private String realName;

    @Schema(description = "客户手机号")
    private String phone;

    @Schema(description = "意向等级：1=高，2=中，3=低")
    private Byte intentionLevel;

    @Schema(description = "对接销售ID")
    private Integer salesId;

    @Schema(description = "负责销售姓名")
    private String salesName;

    @Schema(description = "客户来源")
    private String source;

    @Schema(description = "创建时间-开始 (yyyy-MM-dd)")
    private String createTimeBegin;

    @Schema(description = "创建时间-结束 (yyyy-MM-dd)")
    private String createTimeEnd;

    @Schema(description = "是否删除：0=未删除，1=已删除，默认0")
    private Integer isDeleted = 0;

    @Schema(description = "池筛选：all=全部, mine=我的客户, pool=公海池")
    private String poolFilter;
}
