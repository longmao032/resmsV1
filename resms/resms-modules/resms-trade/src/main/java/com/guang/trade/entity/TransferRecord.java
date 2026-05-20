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

@Getter
@Setter
@ToString
@TableName("tb_transfer_record")
@Schema(name = "TransferRecord", description = "过户记录表")
public class TransferRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "关联交易ID")
    @TableField("transaction_id")
    private Integer transactionId;

    @Schema(description = "过户编号")
    @TableField("transfer_no")
    private String transferNo;

    @Schema(description = "不动产权证书号")
    @TableField("certificate_no")
    private String certificateNo;

    @Schema(description = "不动产登记中心名称")
    @TableField("registration_center")
    private String registrationCenter;

    @Schema(description = "过户日期")
    @TableField("transfer_date")
    private LocalDateTime transferDate;

    @Schema(description = "经办人ID")
    @TableField("operator_id")
    private Integer operatorId;

    @Schema(description = "状态：0=待过户，1=已完成")
    @TableField("status")
    private Byte status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
