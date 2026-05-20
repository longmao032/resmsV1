-- =====================================================
-- 经营报表 菜单权限 SQL
-- 说明：
--   1. 先查找"财务管理"目录的 ID
--   2. 插入"经营报表"菜单
--   3. 插入 3 个按钮权限（project / sales / trend）
--   4. 授权给 admin 角色（假设 role_id = 1）
-- =====================================================

-- Step 1: 插入"经营报表"菜单（挂载在"财务管理"下）
-- 先查找财务管理目录的 ID（menu_type=1 为目录）
SET @finance_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'finance' AND menu_type = 1 LIMIT 1);

-- 如果目录不存在则先创建
INSERT IGNORE INTO sys_menu (parent_id, menu_name, menu_code, menu_type, path, component, icon, sort_order, visible, `status`, is_deleted, create_time)
SELECT 0, '财务管理', 'finance', 1, '/finance', NULL, 'Coin', 5, 1, 1, 0, NOW()
WHERE @finance_parent_id IS NULL;

-- 重新获取 parent_id
SET @finance_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'finance' AND menu_type = 1 LIMIT 1);

-- 插入经营报表菜单（menu_type=2 为菜单）
INSERT IGNORE INTO sys_menu (parent_id, menu_name, menu_code, menu_type, path, component, icon, sort_order, visible, `status`, is_deleted, create_time)
VALUES (@finance_parent_id, '经营报表', 'finance:report', 2, '/finance/report', 'finance/report/index', 'DataAnalysis', 3, 1, 1, 0, NOW());

-- Step 2: 插入 3 个按钮权限
SET @report_menu_id = (SELECT id FROM sys_menu WHERE menu_code = 'finance:report' AND menu_type = 2 LIMIT 1);

INSERT IGNORE INTO sys_menu (parent_id, menu_name, menu_code, menu_type, sort_order, visible, `status`, is_deleted, create_time) VALUES
(@report_menu_id, '楼盘报表',     'finance:report:project', 3, 1, 1, 1, 0, NOW()),
(@report_menu_id, '销售业绩报表', 'finance:report:sales',   3, 2, 1, 1, 0, NOW()),
(@report_menu_id, '收支趋势报表', 'finance:report:trend',   3, 3, 1, 1, 0, NOW());

-- Step 3: 授权给 admin 角色（假设 role_id = 1）
-- 如果需要绑定到指定角色，取消下面的注释并修改 role_id
-- INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
-- SELECT 1, id FROM sys_menu WHERE menu_code IN ('finance:report', 'finance:report:project', 'finance:report:sales', 'finance:report:trend');
