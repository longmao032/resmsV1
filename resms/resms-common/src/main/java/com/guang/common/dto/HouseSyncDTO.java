package com.guang.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class HouseSyncDTO {
    private String eventId;      // 唯一事件ID (UUID)
    private Long eventTime;      // 时间戳
    private String action;       // SAVE / DELETE
    private Integer houseId;
    private Integer projectId;
    private String projectName;
    private String city;
    private String district;
    private String coverImage;
    private Byte houseType;
    private String roomType;
    private String priceText;
    private String areaText;
    private String orientation;
    private String floorInfo;
    private List<String> tags;
    private String description;
}
