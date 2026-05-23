package com.guang.house.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guang.common.exception.ApiException;
import com.guang.common.util.CodeGeneratorUtil;
import com.guang.common.util.ExcelUtils;
import com.guang.common.util.IpUtils;
import com.guang.common.util.SecurityUtils;
import com.guang.house.domain.dto.ProjectQueryDTO;
import com.guang.house.domain.dto.ProjectSaveDTO;
import com.guang.house.domain.vo.ProjectExportVO;
import com.guang.house.domain.vo.ProjectPageVO;
import com.guang.house.domain.vo.ProjectStatisticsVO;
import com.guang.house.domain.vo.ProjectVO;
import com.guang.house.entity.House;
import com.guang.house.entity.Project;
import com.guang.house.entity.ProjectLog;
import com.guang.house.mapper.HouseMapper;
import com.guang.house.mapper.ProjectMapper;
import com.guang.house.service.ProjectLogService;
import com.guang.house.service.ProjectService;
import com.guang.common.dto.ProjectSyncDTO;
import com.guang.common.event.ProjectSyncEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 楼盘项目表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectLogService projectLogService;
    private final HouseMapper houseMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<ProjectVO> pageProjects(ProjectQueryDTO queryDTO) {
        Page<ProjectVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Project> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getProjectName()), "project_name", queryDTO.getProjectName())
                .like(StrUtil.isNotBlank(queryDTO.getCity()), "city", queryDTO.getCity())
                .eq(StrUtil.isNotBlank(queryDTO.getDistrict()), "district", queryDTO.getDistrict())
                .eq(queryDTO.getStatus() != null, "status", queryDTO.getStatus())
                .eq(queryDTO.getProjectType() != null, "project_type", queryDTO.getProjectType())
                .eq("is_deleted", 0);

        if (queryDTO.getLongitude() != null && queryDTO.getLatitude() != null) {
            String centerPoint = String.format("ST_GeomFromText('POINT(%f %f)', 4326)", queryDTO.getLatitude(), queryDTO.getLongitude());
            wrapper.select("*", "ST_Distance_Sphere(coordinate, " + centerPoint + ") as distance");
            if (queryDTO.getRadius() != null) {
                wrapper.apply("ST_Distance_Sphere(coordinate, " + centerPoint + ") <= {0}", queryDTO.getRadius());
            }
            wrapper.orderByAsc("distance");
        } else {
            wrapper.select("*");
            wrapper.orderByDesc("create_time");
        }

        return (Page<ProjectVO>) baseMapper.selectPageVo(page, wrapper);
    }

    @Override
    public Page<ProjectPageVO> pageProjectsForPortal(ProjectQueryDTO queryDTO) {
        Page<ProjectPageVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 构建 WHERE 子句
        StringBuilder where = new StringBuilder("WHERE p.is_deleted = 0");
        if (StrUtil.isNotBlank(queryDTO.getCity())) {
            where.append(" AND p.city LIKE '%").append(queryDTO.getCity()).append("%'");
        }
        if (StrUtil.isNotBlank(queryDTO.getDistrict())) {
            where.append(" AND p.district = '").append(queryDTO.getDistrict()).append("'");
        }
        if (queryDTO.getStatus() != null) {
            where.append(" AND p.status = ").append(queryDTO.getStatus());
        }
        if (queryDTO.getProjectType() != null) {
            where.append(" AND p.project_type = ").append(queryDTO.getProjectType());
        }

        // 构建 SELECT 距离列（可选）
        String distanceSelect = "";
        String orderBy;
        if (queryDTO.getLongitude() != null && queryDTO.getLatitude() != null) {
            String centerPoint = String.format("ST_GeomFromText('POINT(%f %f)', 4326)", queryDTO.getLatitude(), queryDTO.getLongitude());
            distanceSelect = ", ST_Distance_Sphere(p.coordinate, " + centerPoint + ") AS distance";
            if (queryDTO.getRadius() != null) {
                where.append(" AND ST_Distance_Sphere(p.coordinate, ").append(centerPoint).append(") <= ").append(queryDTO.getRadius());
            }
            orderBy = "ORDER BY distance ASC";
        } else {
            orderBy = "ORDER BY p.create_time DESC";
        }

        String sql = "SELECT p.*" + distanceSelect + ", " +
                "ROUND(AVG(h.unit_price_fen) / 100, 0) AS avgPrice, " +
                "MIN(h.area) AS minArea, " +
                "MAX(h.area) AS maxArea, " +
                "CONCAT(MIN(CAST(SUBSTRING_INDEX(h.layout, '室', 1) AS UNSIGNED)), '-', " +
                "MAX(CAST(SUBSTRING_INDEX(h.layout, '室', 1) AS UNSIGNED)), '室') AS layoutSummary, " +
                "COUNT(h.id) AS houseCount " +
                "FROM tb_project p " +
                "LEFT JOIN tb_house h ON h.project_id = p.id AND h.house_type = 1 AND h.status = 1 AND h.is_deleted = 0 " +
                where + " " +
                "GROUP BY p.id " + orderBy;

        return (Page<ProjectPageVO>) baseMapper.selectProjectPageVo(page, sql);
    }

    @Override
    public ProjectPageVO getProjectDetailForPortal(Integer id) {
        String sql = "SELECT p.*, " +
                "ROUND(AVG(h.unit_price_fen) / 100, 0) AS avgPrice, " +
                "MIN(h.area) AS minArea, " +
                "MAX(h.area) AS maxArea, " +
                "CONCAT(MIN(CAST(SUBSTRING_INDEX(h.layout, '室', 1) AS UNSIGNED)), '-', " +
                "MAX(CAST(SUBSTRING_INDEX(h.layout, '室', 1) AS UNSIGNED)), '室') AS layoutSummary, " +
                "COUNT(h.id) AS houseCount " +
                "FROM tb_project p " +
                "LEFT JOIN tb_house h ON h.project_id = p.id AND h.house_type = 1 AND h.status = 1 AND h.is_deleted = 0 " +
                "WHERE p.id = " + id + " AND p.is_deleted = 0 " +
                "GROUP BY p.id";

        Page<ProjectPageVO> page = new Page<>(1, 1);
        Page<ProjectPageVO> result = (Page<ProjectPageVO>) baseMapper.selectProjectPageVo(page, sql);
        return result.getRecords().isEmpty() ? null : result.getRecords().get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveProject(ProjectSaveDTO saveDTO) {
        Project project = BeanUtil.copyProperties(saveDTO, Project.class);
        boolean isNew = project.getId() == null;

        Project oldProject = null;
        if (!isNew) {
            oldProject = this.getById(project.getId());
        }

        if (isNew) {
            project.setProjectNo(CodeGeneratorUtil.generateProjectNo());
        }

        boolean success = this.saveOrUpdate(project);

        if (success && saveDTO.getLongitude() != null && saveDTO.getLatitude() != null) {
            String wkt = String.format("POINT(%f %f)", saveDTO.getLatitude(), saveDTO.getLongitude());
            this.update().setSql("coordinate = ST_GeomFromText('" + wkt + "', 4326)")
                    .eq("id", project.getId())
                    .update();
        }

        if (success) {
            recordChangeLog(project, oldProject);
            try {
                ProjectSyncDTO dto = new ProjectSyncDTO();
                dto.setEventId(java.util.UUID.randomUUID().toString());
                dto.setEventTime(System.currentTimeMillis());
                dto.setAction(project.getIsDeleted() != null && project.getIsDeleted() == 1 ? "DELETE" : "SAVE");
                dto.setProjectId(project.getId());
                dto.setProjectName(project.getProjectName());
                dto.setCity(project.getCity());
                dto.setDistrict(project.getDistrict());
                dto.setAddress(project.getAddress());
                dto.setDeveloper(project.getDeveloper());
                dto.setPropertyCompany(project.getPropertyCompany());
                dto.setTotalHouseholds(project.getTotalHouseholds());
                dto.setPropertyFee(project.getPropertyFee());
                dto.setPlotRatio(project.getPlotRatio());
                dto.setGreeningRate(project.getGreeningRate());
                dto.setTags(project.getTags());
                dto.setCoverUrl(project.getCoverUrl());
                eventPublisher.publishEvent(new ProjectSyncEvent(this, dto));
            } catch (Exception e) {
                log.error("发布楼盘同步事件异常", e);
            }
        }

        return success;
    }

    @Override
    public ProjectStatisticsVO getStatistics() {
        ProjectStatisticsVO vo = new ProjectStatisticsVO();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime prevMonthStart = monthStart.minusMonths(1);
        LocalDateTime prevMonthEnd = monthStart;

        // 1. 项目总数
        long total = lambdaQuery().eq(Project::getIsDeleted, (byte) 0).count();
        long totalPrev = lambdaQuery().eq(Project::getIsDeleted, (byte) 0)
                .lt(Project::getCreateTime, prevMonthEnd).count();
        vo.setTotalCount(total);
        vo.setTotalTrend(calcTrend(total, totalPrev));

        // 2. 在售楼盘
        long onSale = lambdaQuery().eq(Project::getStatus, (byte) 1).eq(Project::getIsDeleted, (byte) 0).count();
        long onSalePrev = lambdaQuery().eq(Project::getStatus, (byte) 1).eq(Project::getIsDeleted, (byte) 0)
                .lt(Project::getCreateTime, prevMonthEnd).count();
        vo.setOnSaleCount(onSale);
        vo.setOnSaleTrend(calcTrend(onSale, onSalePrev));

        // 3. 待开项目
        long pending = lambdaQuery().eq(Project::getStatus, (byte) 3).eq(Project::getIsDeleted, (byte) 0).count();
        long pendingPrev = lambdaQuery().eq(Project::getStatus, (byte) 3).eq(Project::getIsDeleted, (byte) 0)
                .lt(Project::getCreateTime, prevMonthEnd).count();
        vo.setPendingCount(pending);
        vo.setPendingTrend(calcTrend(pending, pendingPrev));

        // 4. 平均佣金
        BigDecimal avgCommission = baseMapper.selectAvgCommissionRate();
        vo.setAvgCommissionRate(avgCommission != null ? avgCommission.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 上月平均佣金（趋势）
        BigDecimal avgCommissionPrev = baseMapper.selectAvgCommissionRateBefore(prevMonthEnd);
        double commissionTrend;
        if (avgCommissionPrev != null && avgCommissionPrev.compareTo(BigDecimal.ZERO) > 0) {
            commissionTrend = vo.getAvgCommissionRate().subtract(avgCommissionPrev)
                    .divide(avgCommissionPrev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        } else {
            commissionTrend = 0;
        }
        vo.setAvgCommissionTrend(commissionTrend);

        return vo;
    }

    @Override
    public void exportProjects(ProjectQueryDTO queryDTO, HttpServletResponse response) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Project> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getProjectName()), "project_name", queryDTO.getProjectName())
                .like(StrUtil.isNotBlank(queryDTO.getCity()), "city", queryDTO.getCity())
                .eq(StrUtil.isNotBlank(queryDTO.getDistrict()), "district", queryDTO.getDistrict())
                .eq(queryDTO.getStatus() != null, "status", queryDTO.getStatus())
                .eq(queryDTO.getProjectType() != null, "project_type", queryDTO.getProjectType())
                .eq("is_deleted", 0)
                .orderByDesc("create_time");

        List<Project> list = this.list(wrapper);

        List<ProjectExportVO> exportList = list.stream().map(project -> {
            ProjectExportVO vo = BeanUtil.copyProperties(project, ProjectExportVO.class);

            // 项目类型转换
            if (project.getProjectType() != null) {
                vo.setProjectTypeText(project.getProjectType() == 1 ? "新房楼盘" : "二手房小区");
            }

            // 销售状态转换
            String statusText;
            if (project.getStatus() != null) {
                switch (project.getStatus()) {
                    case 1:  statusText = "在售"; break;
                    case 2:  statusText = "售罄"; break;
                    case 3:  statusText = "待售"; break;
                    case 4:  statusText = "下架"; break;
                    default: statusText = "未知"; break;
                }
            } else {
                statusText = "未知";
            }
            vo.setStatusText(statusText);

            // 地址拼接
            vo.setAddress((project.getProvince() != null ? project.getProvince() : "")
                    + (project.getCity() != null ? project.getCity() : "")
                    + (project.getDistrict() != null ? project.getDistrict() : "")
                    + (project.getAddress() != null ? project.getAddress() : ""));

            return vo;
        }).collect(Collectors.toList());

        ExcelUtils.exportExcel(response, "项目列表_" + System.currentTimeMillis(), "项目资料", ProjectExportVO.class, exportList);
    }

    private double calcTrend(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0;
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public Boolean deleteProject(Integer id) {
        // 校验：检查该楼盘下是否有未删除的关联房源
        Long houseCount = houseMapper.selectCount(
                new LambdaQueryWrapper<House>()
                        .eq(House::getProjectId, id)
                        .eq(House::getIsDeleted, (byte) 0));
        if (houseCount > 0) {
            throw new ApiException("该楼盘下存在 " + houseCount + " 个关联房源，请先处理相关房源后再删除");
        }

        Project project = new Project();
        project.setId(id);
        project.setIsDeleted((byte) 1);
        return this.updateById(project);
    }

    @Override
    public ProjectVO getProjectDetail(Integer id) {
        Project project = this.getById(id);
        if (project == null || project.getIsDeleted() == 1) {
            return null;
        }
        return BeanUtil.copyProperties(project, ProjectVO.class);
    }

    // ========== 变更日志记录 ==========

    private void recordChangeLog(Project project, Project oldProject) {
        List<ProjectLog> logs = new ArrayList<>();
        Integer operatorId = SecurityUtils.getUserId() != null ? SecurityUtils.getUserId() : 0;
        String operatorName = SecurityUtils.getUsername() != null ? SecurityUtils.getUsername() : "系统";
        String clientIp = getClientIP();

        if (oldProject == null) {
            addInitialLog(logs, project.getId(), "项目名称", project.getProjectName(), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "项目类型", formatProjectType(project.getProjectType()), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "开发商", project.getDeveloper(), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "物业公司", project.getPropertyCompany(), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "销售状态", formatStatus(project.getStatus()), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "佣金比例", formatPercent(project.getCommissionRate()), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "总户数", project.getTotalHouseholds() != null ? project.getTotalHouseholds() + "户" : null, operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "物业费", formatBigDecimal(project.getPropertyFee(), "元/㎡/月"), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "容积率", formatDecimal(project.getPlotRatio()), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "绿化率", formatBigDecimal(project.getGreeningRate(), "%"), operatorId, operatorName, clientIp);
            addInitialLog(logs, project.getId(), "详细地址", project.getAddress(), operatorId, operatorName, clientIp);
        } else {
            compareAndLog(logs, project.getId(), "项目名称", oldProject.getProjectName(), project.getProjectName(), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "项目类型", formatProjectType(oldProject.getProjectType()), formatProjectType(project.getProjectType()), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "开发商", oldProject.getDeveloper(), project.getDeveloper(), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "物业公司", oldProject.getPropertyCompany(), project.getPropertyCompany(), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "销售状态", formatStatus(oldProject.getStatus()), formatStatus(project.getStatus()), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "佣金比例", formatPercent(oldProject.getCommissionRate()), formatPercent(project.getCommissionRate()), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "总户数", oldProject.getTotalHouseholds() != null ? oldProject.getTotalHouseholds() + "户" : null, project.getTotalHouseholds() != null ? project.getTotalHouseholds() + "户" : null, operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "物业费", formatBigDecimal(oldProject.getPropertyFee(), "元/㎡/月"), formatBigDecimal(project.getPropertyFee(), "元/㎡/月"), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "容积率", formatDecimal(oldProject.getPlotRatio()), formatDecimal(project.getPlotRatio()), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "绿化率", formatBigDecimal(oldProject.getGreeningRate(), "%"), formatBigDecimal(project.getGreeningRate(), "%"), operatorId, operatorName, clientIp);
            compareAndLog(logs, project.getId(), "详细地址", oldProject.getAddress(), project.getAddress(), operatorId, operatorName, clientIp);
        }

        if (!logs.isEmpty()) {
            projectLogService.saveBatch(logs);
        }
    }

    private void compareAndLog(List<ProjectLog> logs, Integer projectId, String fieldLabel, String oldVal, String newVal, Integer operatorId, String operatorName, String clientIp) {
        if (!Objects.equals(oldVal, newVal)) {
            logs.add(buildLog(projectId, fieldLabel, oldVal, newVal, operatorId, operatorName, clientIp));
        }
    }

    private void addInitialLog(List<ProjectLog> logs, Integer projectId, String fieldLabel, String value, Integer operatorId, String operatorName, String clientIp) {
        logs.add(buildLog(projectId, fieldLabel, null, value, operatorId, operatorName, clientIp));
    }

    private ProjectLog buildLog(Integer projectId, String fieldLabel, String oldValue, String newValue, Integer operatorId, String operatorName, String clientIp) {
        ProjectLog log = new ProjectLog();
        log.setProjectId(projectId);
        log.setFieldLabel(fieldLabel);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setIpAddress(clientIp);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    private String formatProjectType(Byte type) {
        if (type == null) return null;
        return type == 1 ? "新房楼盘" : "二手房小区";
    }

    private String formatStatus(Byte status) {
        if (status == null) return null;
        switch (status) {
            case 1: return "在售";
            case 2: return "售罄";
            case 3: return "待售";
            case 4: return "下架";
            default: return "未知";
        }
    }

    private String formatPercent(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() + "%" : null;
    }

    private String formatBigDecimal(BigDecimal value, String unit) {
        return value != null ? value.stripTrailingZeros().toPlainString() + unit : null;
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : null;
    }

    private String getClientIP() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return IpUtils.getClientIP(attributes.getRequest());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
