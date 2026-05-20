package com.guang.house.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 房源图片保存对象
 *
 * @author blackDuck
 */
@Data
@Schema(description = "房源图片保存对象")
public class HouseImageSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "房源ID")
    private Integer houseId;

    @Schema(description = "文件存储Key")
    private String fileKey;

    @Schema(description = "图片类型：1=封面图，2=室内图，3=户型图，4=环境图，5=其他")
    private Byte imageType;

    @Schema(description = "图片分组")
    private String imageGroup;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否默认首图")
    private Byte isDefault;

    @Schema(description = "图片访问地址 (对应后端 url 字段)")
    private String url;
}
