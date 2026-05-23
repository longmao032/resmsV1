package com.guang.common.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectSyncDTO {
    private String eventId;      // 唯一事件ID (UUID)
    private Long eventTime;      // 时间戳
    private String action;       // SAVE / DELETE
    private Integer projectId;
    private String projectName;
    private String city;
    private String district;
    private String address;
    private String developer;
    private String propertyCompany;
    private Integer totalHouseholds;
    private BigDecimal propertyFee;
    private BigDecimal plotRatio;
    private BigDecimal greeningRate;
    private List<String> tags;
    private String coverUrl;
}
