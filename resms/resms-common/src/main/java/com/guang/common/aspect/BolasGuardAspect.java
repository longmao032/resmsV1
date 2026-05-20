package com.guang.common.aspect;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.guang.common.annotation.BolasGuard;
import com.guang.common.exception.ApiException;
import com.guang.common.security.LoginUser;
import com.guang.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * BOLA 防护切面
 *
 * @author blackDuck
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class BolasGuardAspect {

    private final JdbcTemplate jdbcTemplate;

    @Before("@annotation(bolasGuard)")
    public void doBefore(JoinPoint joinPoint, BolasGuard bolasGuard) {
        // 1. 获取当前登录用户信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.isAppUser()) {
            return;
        }

        // 2. 超级管理员 / 全部数据 权限直接放行
        if (loginUser.getDataScope() != null && loginUser.getDataScope() == 1) {
            return;
        }

        // 3. 从入参中提取出被操作的实体 ID 值
        Integer entityId = extractEntityId(joinPoint, bolasGuard.idParamName());
        if (entityId == null) {
            // 新增操作，无 ID，直接放行 (Service 层已有销售指派防越权限制)
            return;
        }

        // 4. 解析被操作实体的物理表名与负责人列名
        Class<?> clazz = bolasGuard.entityClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
        if (tableInfo == null) {
            log.error("BolasGuard 未能在 MyBatis-Plus 中找到对应的实体表信息：{}", clazz.getName());
            return;
        }

        String tableName = tableInfo.getTableName();
        String userField = bolasGuard.userField();
        String userColumn = tableInfo.getKeyColumn(); // 默认主键列兜底

        if (tableInfo.getFieldList() != null) {
            userColumn = tableInfo.getFieldList().stream()
                    .filter(f -> f.getProperty().equals(userField))
                    .map(com.baomidou.mybatisplus.core.metadata.TableFieldInfo::getColumn)
                    .findFirst()
                    .orElse(userField);
        }

        // 5. 使用 Spring JDBC 直接、高性能查询出数据负责人 ID 和归属部门 ID
        //    排除已删除的用户，dept_id 为 NULL 时表示负责人已不存在（孤立记录）
        String querySql = "SELECT t." + userColumn + " as owner_id, u.dept_id FROM " + tableName + " t " +
                " LEFT JOIN sys_user u ON t." + userColumn + " = u.id AND u.is_deleted = 0 WHERE t." + tableInfo.getKeyColumn() + " = ?";

        List<Map<String, Object>> list = jdbcTemplate.queryForList(querySql, entityId);
        if (list.isEmpty()) {
            // 数据不存在，直接放行，底层由业务方法或 MyBatis-Plus 自行处理不存在异常
            return;
        }

        Map<String, Object> data = list.get(0);
        Object ownerIdObj = data.get("owner_id");
        Object deptIdObj = data.get("dept_id");

        Integer ownerId = ownerIdObj != null ? ((Number) ownerIdObj).intValue() : null;
        Integer ownerDeptId = deptIdObj != null ? ((Number) deptIdObj).intValue() : null;

        // 如果该记录负责人为 NULL 且显式配置了 includeNullUser，则允许对此记录进行写操作放行（如公海客户）
        if (ownerId == null && bolasGuard.includeNullUser()) {
            return;
        }

        // 6. 执行权限级别对比校验
        Integer currentDeptId = loginUser.getDeptId();
        Integer currentUserId = loginUser.getUserId();
        Byte scope = loginUser.getDataScope();
        if (scope == null) {
            scope = 4; // 默认仅限本人数据
        }

        // 如果负责人已被删除（ownerId 非空但 ownerDeptId 为空），记录成为孤立数据。
        // 部门级用户（scope 2/3）可直接操作，避免离职员工的数据无人能处理。
        if (ownerId != null && ownerDeptId == null) {
            log.warn("[孤立记录放行] 负责人(id={})在 sys_user 中已不存在或已删除，允许部门级操作", ownerId);
            if (scope == 2 || scope == 3) {
                return;
            }
            // scope 4 继续走下面的校验（当前用户不是负责人，将被拒绝）
        }

        boolean isAuthorized = false;

        switch (scope) {
            case 2 -> { // 本部门
                if (currentDeptId != null && currentDeptId.equals(ownerDeptId)) {
                    isAuthorized = true;
                }
            }
            case 3 -> { // 本部门及下属子部门
                if (currentDeptId != null && ownerDeptId != null) {
                    if (currentDeptId.equals(ownerDeptId)) {
                        isAuthorized = true;
                    } else {
                        // 查询 ancestors 是否包含当前用户的 deptId
                        String checkSql = "SELECT COUNT(1) FROM sys_dept WHERE id = ? AND FIND_IN_SET(?, ancestors)";
                        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, ownerDeptId, currentDeptId);
                        if (count != null && count > 0) {
                            isAuthorized = true;
                        }
                    }
                }
            }
            case 4 -> { // 仅限本人
                if (currentUserId != null && currentUserId.equals(ownerId)) {
                    isAuthorized = true;
                }
            }
            default -> {}
        }

        if (!isAuthorized) {
            log.warn("[水平越权写操作拦截] 用户(id={}, name={}) 试图非法篡改/删除实体({} id={})，该实体的负责人为(id={})",
                    currentUserId, loginUser.getUsername(), clazz.getSimpleName(), entityId, ownerId);
            throw new ApiException("无权操作此数据");
        }
    }

    /**
     * 自动从 JointPoint 的各个入参中定位并提取实体 ID
     */
    private Integer extractEntityId(JoinPoint joinPoint, String idParamName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames == null || args == null) {
            return null;
        }

        // A. 匹配命名参数
        for (int i = 0; i < parameterNames.length; i++) {
            if (idParamName.equals(parameterNames[i]) && args[i] instanceof Number) {
                return ((Number) args[i]).intValue();
            }
        }

        // B. 检查 DTO/JavaBean 对象的内部属性 (如 HouseSaveDTO)
        for (Object arg : args) {
            if (arg != null && !(arg instanceof Number) && !(arg instanceof String) && !(arg instanceof Boolean) && !(arg instanceof Collection) && !(arg instanceof Map)) {
                try {
                    Field field = findField(arg.getClass(), idParamName);
                    if (field != null) {
                        field.setAccessible(true);
                        Object val = field.get(arg);
                        if (val instanceof Number) {
                            return ((Number) val).intValue();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
