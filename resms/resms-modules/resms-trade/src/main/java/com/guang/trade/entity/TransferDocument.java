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
@TableName("tb_transfer_document")
@Schema(name = "TransferDocument", description = "过户文件表")
public class TransferDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "关联过户记录ID")
    @TableField("transfer_id")
    private Integer transferId;

    @Schema(description = "文件类型")
    @TableField("doc_type")
    private String docType;

    @Schema(description = "文件原名")
    @TableField("doc_name")
    private String docName;

    @Schema(description = "文件存储路径")
    @TableField("file_url")
    private String fileUrl;

    @Schema(description = "上传时间")
    @TableField("upload_time")
    private LocalDateTime uploadTime;
}
