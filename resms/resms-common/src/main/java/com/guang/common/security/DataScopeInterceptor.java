package com.guang.common.security;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.guang.common.annotation.DataScope;
import com.guang.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限过滤拦截器（基于 MyBatis-Plus InnerInterceptor + JSqlParser AST 改写）
 * <p>
 * 核心策略：
 * <ol>
 *   <li>方法名白名单 — 显式跳过工具类查询</li>
 *   <li>TablesNamesFinder — 校验目标表是否在 SQL 中</li>
 *   <li>动态别名提取 — 优先使用 SQL 中声明的表别名</li>
 *   <li>finally 清理 ThreadLocal — 防止跨查询污染</li>
 * </ol>
 *
 * @author blackDuck
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    /**
     * 方法名白名单：这些 Mapper 方法永远不会被注入数据权限过滤条件
     */
    private static final Set<String> METHOD_WHITELIST = Set.of(
            "selectUserNames",
            "selectAvgTotalPriceFen",
            "selectAvgTotalPriceFenBefore",
            "countPendingPayments",
            "selectById"
    );

    private static final Map<String, Class<?>> MAPPER_ENTITY_CACHE = new ConcurrentHashMap<>();

    // ---------- entity resolution ----------

    private Class<?> getEntityClass(String msId) {
        if (msId == null || !msId.contains(".")) {
            return null;
        }
        String mapperClassName = msId.substring(0, msId.lastIndexOf("."));
        return MAPPER_ENTITY_CACHE.computeIfAbsent(mapperClassName, key -> {
            try {
                Class<?> mapperClass = Class.forName(key);
                for (Type type : mapperClass.getGenericInterfaces()) {
                    if (type instanceof ParameterizedType parameterizedType) {
                        Type rawType = parameterizedType.getRawType();
                        if (rawType instanceof Class<?> clazz && BaseMapper.class.isAssignableFrom(clazz)) {
                            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                            if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                                return (Class<?>) actualTypeArguments[0];
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取 Mapper 对应的实体类失败，msId: {}", msId, e);
            }
            return Object.class;
        });
    }

    /**
     * 提取方法名（msId 的最后一段，如 selectPageVo）
     */
    private String getMethodName(String msId) {
        if (msId == null || !msId.contains(".")) return msId;
        return msId.substring(msId.lastIndexOf(".") + 1);
    }

    /**
     * 校验实体类是否包含指定的数据隔离字段
     */
    private boolean entityHasField(TableInfo tableInfo, String userField) {
        boolean hasField = tableInfo.getFieldList().stream().anyMatch(field ->
                field.getColumn().equalsIgnoreCase(userField) || field.getProperty().equalsIgnoreCase(userField)
        );
        if (tableInfo.getKeyColumn() != null && tableInfo.getKeyColumn().equalsIgnoreCase(userField)) {
            hasField = true;
        }
        if (tableInfo.getKeyProperty() != null && tableInfo.getKeyProperty().equalsIgnoreCase(userField)) {
            hasField = true;
        }
        return hasField;
    }

    /**
     * 从 PlainSelect 的 FROM + JOIN 子句中提取目标物理表对应的别名。
     * 优先使用 Table.getName()，兜底用 toString() 字符串解析，最后退化为 null。
     */
    private String resolveAlias(PlainSelect plainSelect, String targetTable) {
        // 从 FromItem 提取纯表名和别名
        record AliasResult(String bareTable, String aliasName) {}
        java.util.function.Function<FromItem, AliasResult> extract = item -> {
            if (item == null) return null;
            Alias a = item.getAlias();
            String aliasName = (a != null) ? a.getName() : null;
            // 优先用 Table 的 getName（精确），兜底 toString 解析
            String bare;
            if (item instanceof net.sf.jsqlparser.schema.Table table) {
                bare = table.getName();
            } else {
                String s = item.toString();
                bare = (aliasName != null) ? s.substring(0, Math.max(0, s.length() - aliasName.length())).trim() : s;
                if (bare.contains(".")) {
                    bare = bare.substring(bare.lastIndexOf(".") + 1);
                }
            }
            return new AliasResult(bare, aliasName);
        };

        // 主 FROM 项
        AliasResult main = extract.apply(plainSelect.getFromItem());
        if (main != null && targetTable.equalsIgnoreCase(main.bareTable())) {
            return main.aliasName();
        }

        // JOIN 项
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                AliasResult r = extract.apply(join.getRightItem());
                if (r != null && targetTable.equalsIgnoreCase(r.bareTable())) {
                    return r.aliasName();
                }
            }
        }

        return null;
    }

    // ---------- interceptor entry ----------

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        DataScope dataScope = DataScopeContext.get();
        if (dataScope == null) {
            return;
        }

        try {
            // 0. 方法名白名单 — 显式排除工具类查询
            String methodName = getMethodName(ms.getId());
            if (METHOD_WHITELIST.contains(methodName)) {
                return;
            }

            // 1. 解析实体类 → 获取目标物理表名 & 字段校验
            Class<?> entityClass = getEntityClass(ms.getId());
            String targetTable = null;
            if (entityClass != null && entityClass != Object.class) {
                TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
                if (tableInfo != null) {
                    targetTable = tableInfo.getTableName();
                    if (!entityHasField(tableInfo, dataScope.userField())) {
                        return;
                    }
                }
            }
            if (targetTable == null) {
                return;
            }

            // 2. 获取登录用户
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null || loginUser.isAppUser()) {
                return;
            }

            // 3. 超级管理员 / 全部数据权限 跳过
            if (loginUser.getDataScope() != null && loginUser.getDataScope() == 1) {
                return;
            }

            // 4. 获取原始 SQL 并解析
            PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
            String originalSql = mpBoundSql.sql();

            Select select = (Select) CCJSqlParserUtil.parse(originalSql);
            if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
                return;
            }

            // 5. TablesNamesFinder — 扫描 SQL 涉及的所有表名（小写归一化集合）
            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> sqlTables = tablesNamesFinder.getTableList((Statement) select);
            final String finalTargetTable = targetTable;
            // 某些表名带有 ` 反引号，统一去除
            boolean containsTarget = sqlTables.stream().anyMatch(t ->
                    t.replace("`", "").equalsIgnoreCase(finalTargetTable)
            );
            if (!containsTarget) {
                return;
            }

            // 6. 动态提取 SQL 中目标表对应的别名
            String alias = resolveAlias(plainSelect, targetTable);
            // 优先级：SQL 别名 > 物理表名
            String qualifier = (alias != null) ? alias : targetTable;

            // 7. 构造过滤条件
            String filterSql = buildFilterSql(loginUser, dataScope, qualifier);
            if (filterSql == null || filterSql.isEmpty()) {
                return;
            }

            // 8. AST 注入 WHERE 条件
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(CCJSqlParserUtil.parseCondExpression(filterSql));
            } else {
                Expression filterExpr = CCJSqlParserUtil.parseCondExpression("(" + filterSql + ")");
                plainSelect.setWhere(new AndExpression(where, filterExpr));
            }

            mpBoundSql.sql(select.toString());

        } catch (Exception e) {
            log.error("数据隔离 SQL 重写解析发生异常，原始SQL为：{}，异常：", boundSql.getSql(), e);
        } finally {
            DataScopeContext.clear();
        }
    }

    // ---------- filter SQL builder ----------

    /**
     * 根据当前用户的数据权限范围和注解配置动态拼接过滤条件 SQL 片段。
     *
     * @param qualifier 列限定符（SQL 实际使用的表别名或物理表名）
     */
    private String buildFilterSql(LoginUser loginUser, DataScope dataScope, String qualifier) {
        Integer deptId = loginUser.getDeptId();
        Integer userId = loginUser.getUserId();
        Byte scope = loginUser.getDataScope();

        if (scope == null) {
            scope = 4;
        }

        String deptAlias = dataScope.deptAlias();
        String userAlias = dataScope.userAlias();
        String deptField = dataScope.deptField();
        String userField = dataScope.userField();
        boolean joinUserDept = dataScope.joinUserDept();
        boolean includeNullUser = dataScope.includeNullUser();

        // 列限定符优先级：注解显式别名 > SQL 动态别名/表名 > 裸列名
        String deptQualifier = (deptAlias != null && !deptAlias.trim().isEmpty())
                ? deptAlias.trim() : qualifier;
        String userQualifier = (userAlias != null && !userAlias.trim().isEmpty())
                ? userAlias.trim() : qualifier;

        String deptColumn = deptQualifier + "." + deptField;
        String userColumn = userQualifier + "." + userField;

        String filterSql;

        if (joinUserDept) {
            filterSql = switch (scope) {
                case 2 -> {
                    if (deptId == null) yield "1=0";
                    yield userColumn + " IN (SELECT id FROM sys_user WHERE dept_id = " + deptId + " AND is_deleted = 0)";
                }
                case 3 -> {
                    if (deptId == null) yield "1=0";
                    yield userColumn + " IN (SELECT id FROM sys_user WHERE (dept_id = " + deptId
                            + " OR dept_id IN (SELECT id FROM sys_dept WHERE FIND_IN_SET(" + deptId + ", ancestors))) AND is_deleted = 0)";
                }
                case 4 ->
                        userColumn + " = " + userId;
                default ->
                        userColumn + " = " + userId;
            };
        } else {
            filterSql = switch (scope) {
                case 2 -> {
                    if (deptId == null) yield "1=0";
                    yield deptColumn + " = " + deptId;
                }
                case 3 -> {
                    if (deptId == null) yield "1=0";
                    yield "(" + deptColumn + " = " + deptId
                            + " OR " + deptColumn + " IN (SELECT id FROM sys_dept WHERE FIND_IN_SET(" + deptId + ", ancestors)))";
                }
                case 4 ->
                        userColumn + " = " + userId;
                default ->
                        userColumn + " = " + userId;
            };
        }

        if (includeNullUser && !filterSql.isEmpty() && !"1=0".equals(filterSql)) {
            filterSql = "(" + filterSql + " OR " + userColumn + " IS NULL)";
        }

        return filterSql;
    }
}
