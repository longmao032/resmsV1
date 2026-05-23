package com.guang.aiassistant.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class HouseSyncDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long eventTime;
    private String action; // SAVE, DELETE

    private Integer houseId;
    private Integer projectId;
    private String projectName;
    private String city;
    private String district;
    private String coverImage;
    private Byte houseType; // 1=新房, 2=二手房, 3=租房
    private String roomType; // "三室一厅"
    private String priceText; // "35000元/㎡" 或 "250万" 或 "2500元/月"
    private String areaText; // "120㎡"
    private String orientation; // "南北"
    private String floorInfo; // "第12层/共20层"
    private List<String> tags;
    private String description;
}
