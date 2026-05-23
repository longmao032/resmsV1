package com.guang.aiassistant.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectSyncDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long eventTime;
    private String action; // SAVE, DELETE

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
