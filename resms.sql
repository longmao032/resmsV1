/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80407 (8.4.7)
 Source Host           : localhost:3306
 Source Schema         : db_reggie

 Target Server Type    : MySQL
 Target Server Version : 80407 (8.4.7)
 File Encoding         : 65001

 Date: 23/05/2026 16:31:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` int NULL DEFAULT 0 COMMENT '父部门ID，0为顶级',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门编码',
  `leader_id` int NULL DEFAULT NULL COMMENT '部门负责人ID',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门电话',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ancestors` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '层级路径',
  `dept_type` tinyint NULL DEFAULT 1 COMMENT '部门类型：1=公司，2=部门，3=门店',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门邮箱',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dept_name`(`dept_name` ASC) USING BTREE,
  UNIQUE INDEX `uk_dept_code`(`dept_code` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_leader_id`(`leader_id` ASC) USING BTREE,
  CONSTRAINT `sys_dept_ibfk_1` FOREIGN KEY (`leader_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, 0, '总部', 'HQ', NULL, NULL, 0, 1, 0, '2026-05-07 05:58:46', '2026-05-09 03:26:53', '', 1, NULL);
INSERT INTO `sys_dept` VALUES (2, 1, '销售部', 'SALES', NULL, NULL, 0, 1, 0, '2026-05-07 05:58:46', '2026-05-09 22:08:48', ',1', 2, NULL);
INSERT INTO `sys_dept` VALUES (3, 1, '财务部', 'FINANCE', NULL, NULL, 0, 1, 0, '2026-05-07 05:58:46', '2026-05-07 05:58:46', '', 2, NULL);
INSERT INTO `sys_dept` VALUES (4, 2, '销售一部', 'SALEA_01', NULL, '100000', 0, 1, 0, '2026-05-09 22:09:21', '2026-05-09 22:09:21', ',1,2', 2, '');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` int NULL DEFAULT 0 COMMENT '父菜单ID，0为顶级',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单标识',
  `menu_type` tinyint NOT NULL DEFAULT 1 COMMENT '菜单类型：1=目录，2=菜单，3=按钮',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由路径',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint NULL DEFAULT 1 COMMENT '是否可见：0=隐藏，1=可见',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 135 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 0, '控制台', 'dashboard', 2, '/dashboard', 'dashboard/index', 'DataLine', 1, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-08 08:12:34');
INSERT INTO `sys_menu` VALUES (4, 0, '系统管理', 'system', 1, '/system', 'Layout', 'Setting', 10, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (5, 4, '部门管理', 'system:dept', 2, 'dept', 'system/dept/index', 'OfficeBuilding', 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (6, 5, '新增部门', 'system:dept:add', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (7, 5, '修改部门', 'system:dept:edit', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (8, 5, '删除部门', 'system:dept:delete', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (9, 5, '查询部门树', 'system:dept:query', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (10, 4, '菜单管理', 'system:menu', 2, 'menu', 'system/menu/index', 'Menu', 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (11, 10, '获取导航菜单', 'system:menu:nav', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (12, 4, '角色管理', 'system:role', 2, 'role', 'system/role/index', 'Stamp', 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (13, 12, '新增角色', 'system:role:add', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (14, 12, '修改角色', 'system:role:edit', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (15, 12, '删除角色', 'system:role:delete', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (16, 12, '修改角色状态', 'system:role:status', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (17, 12, '查询角色', 'system:role:query', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (18, 4, '用户管理', 'system:user', 2, 'user', 'system/user/index', 'User', 4, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (19, 18, '新增用户', 'system:user:add', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (20, 18, '修改用户', 'system:user:edit', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (21, 18, '删除用户', 'system:user:delete', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (22, 18, '重置密码', 'system:user:password', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (23, 18, '修改用户状态', 'system:user:status', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (24, 18, '查询用户', 'system:user:query', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (25, 4, '操作日志', 'system:log', 2, 'log', 'system/log/index', 'Operation', 5, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (26, 25, '分页查询日志', 'system:log:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (27, 0, '房源管理', 'house', 1, '/house', 'Layout', 'House', 20, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (28, 27, '房源列表', 'house:house:list', 2, 'base', 'house/base/index', 'List', 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-10 01:43:02');
INSERT INTO `sys_menu` VALUES (29, 28, '保存房源', 'house:house:save', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-10 01:43:02');
INSERT INTO `sys_menu` VALUES (30, 28, '查询详情', 'house:house:query', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-10 01:43:02');
INSERT INTO `sys_menu` VALUES (31, 28, '房源审核', 'house:house:audit', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-10 01:43:02');
INSERT INTO `sys_menu` VALUES (32, 28, '房源导出', 'house:house:export', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-10 01:43:02');
INSERT INTO `sys_menu` VALUES (33, 28, '设为封面', 'house:image:set-cover', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (34, 28, '保存图片', 'house:image:save', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (35, 28, '图片清理', 'house:image:clean', 3, NULL, NULL, NULL, 7, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (36, 28, '图片排序', 'house:image:sort', 3, NULL, NULL, NULL, 8, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (37, 27, '楼盘项目', 'house:project:list', 2, 'project', 'house/project/index', 'Place', 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-10 01:43:02');
INSERT INTO `sys_menu` VALUES (38, 37, '保存项目', 'house:project:save', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (39, 37, '删除项目', 'house:project:delete', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (40, 37, '项目查询', 'house:project:query', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (41, 0, '业务管理', 'trade', 1, '/trade', 'Layout', 'ShoppingBag', 30, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (42, 41, '交易订单', 'trade:order', 2, 'order', 'trade/order/index', 'Tickets', 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (43, 42, '订单查询', 'trade:order:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (44, 42, '创建交易', 'trade:order:add', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (45, 42, '更新订单', 'trade:order:edit', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (46, 41, '客户管理', 'trade:customer', 2, 'customer', 'trade/customer/index', 'Avatar', 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (47, 46, '查询客户', 'trade:customer:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (48, 46, '保存客户', 'trade:customer:save', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (49, 46, '删除客户', 'trade:customer:delete', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (50, 46, '查看手机号', 'trade:customer:view-phone', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (51, 0, '财务管理', 'finance', 1, '/finance', 'Layout', 'Money', 40, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (52, 51, '支付流水', 'fin:payment', 2, 'payment', 'finance/payment/index', 'CreditCard', 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-08 06:26:17');
INSERT INTO `sys_menu` VALUES (53, 52, '流水查询', 'fin:payment:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (54, 52, '提交流水', 'fin:payment:add', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (55, 52, '财务审核', 'fin:payment:audit', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-07 12:09:39', '2026-05-07 12:09:39');
INSERT INTO `sys_menu` VALUES (56, 70, '通知公告', 'message:notice', 2, 'notice', 'message/notice/index', 'Bell', 6, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-09 21:07:34');
INSERT INTO `sys_menu` VALUES (57, 56, '查询公告', 'message:notice:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-09 02:06:53');
INSERT INTO `sys_menu` VALUES (58, 56, '发布公告', 'message:notice:add', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-09 02:06:53');
INSERT INTO `sys_menu` VALUES (59, 51, '佣金管理', 'fin:commission', 2, 'commission', 'finance/commission/index', 'Money', 2, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-08 07:46:07');
INSERT INTO `sys_menu` VALUES (60, 51, '经营报表', 'fin:report', 2, 'report', 'finance/report/index', 'DataAnalysis', 3, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-08 07:46:07');
INSERT INTO `sys_menu` VALUES (61, 41, '客户足迹', 'trade:history', 2, 'history', 'trade/history/index', 'List', 3, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-08 23:59:55');
INSERT INTO `sys_menu` VALUES (62, 41, '客户收藏', 'trade:favorite', 2, 'favorite', 'trade/favorite/index', 'Star', 4, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-08 07:46:07');
INSERT INTO `sys_menu` VALUES (70, 0, '消息中心', 'message', 1, '/message', 'Layout', 'ChatDotRound', 50, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-08 07:46:07');
INSERT INTO `sys_menu` VALUES (71, 70, '在线聊天', 'message:chat', 2, 'chat', 'message/chat/index', 'ChatLineRound', 1, 1, 1, 0, '2026-05-08 07:46:07', '2026-05-08 07:46:07');
INSERT INTO `sys_menu` VALUES (72, 27, '房源日志', 'house:log', 2, 'log', 'house/log/index', 'List', 3, 1, 1, 0, '2026-05-08 23:55:00', '2026-05-08 23:59:01');
INSERT INTO `sys_menu` VALUES (73, 0, '我的部门', 'team', 1, '/team', 'Layout', 'UserFilled', 45, 1, 1, 0, '2026-05-09 02:24:02', '2026-05-11 14:02:13');
INSERT INTO `sys_menu` VALUES (74, 73, '部门成员', 'team:user', 2, 'user', 'team/user/index', 'User', 1, 1, 1, 0, '2026-05-09 02:24:02', '2026-05-09 03:18:22');
INSERT INTO `sys_menu` VALUES (75, 73, '业绩统计', 'team:performance', 2, 'performance', 'team/performance/index', 'TrendCharts', 2, 1, 1, 0, '2026-05-09 02:24:02', '2026-05-09 02:24:02');
INSERT INTO `sys_menu` VALUES (76, 5, '部门成员', 'system:dept:members', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-09 19:44:42', '2026-05-09 19:44:42');
INSERT INTO `sys_menu` VALUES (77, 5, '添加成员', 'system:dept:member:add', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-09 19:44:42', '2026-05-09 19:44:42');
INSERT INTO `sys_menu` VALUES (78, 5, '移除成员', 'system:dept:member:remove', 3, NULL, NULL, NULL, 7, 1, 1, 0, '2026-05-09 19:44:42', '2026-05-09 19:44:42');
INSERT INTO `sys_menu` VALUES (79, 10, '菜单查询', 'system:menu:query', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-09 20:54:05', '2026-05-09 20:54:05');
INSERT INTO `sys_menu` VALUES (80, 10, '菜单新增', 'system:menu:add', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-09 20:54:05', '2026-05-09 20:54:05');
INSERT INTO `sys_menu` VALUES (81, 10, '菜单修改', 'system:menu:edit', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-09 20:54:05', '2026-05-09 20:54:05');
INSERT INTO `sys_menu` VALUES (82, 10, '菜单删除', 'system:menu:delete', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-09 20:54:05', '2026-05-09 20:54:05');
INSERT INTO `sys_menu` VALUES (83, 25, '操作导出', 'system:log:export', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-09 20:57:08', '2026-05-09 20:57:08');
INSERT INTO `sys_menu` VALUES (84, 25, '操作详情', 'system:log:detail', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-09 20:57:08', '2026-05-09 20:57:08');
INSERT INTO `sys_menu` VALUES (85, 18, '用户导出', 'system:user:export', 3, NULL, NULL, NULL, 7, 1, 1, 0, '2026-05-09 21:01:18', '2026-05-09 21:01:18');
INSERT INTO `sys_menu` VALUES (86, 56, '修改公告', 'message:notice:edit', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-09 21:07:34', '2026-05-09 21:07:34');
INSERT INTO `sys_menu` VALUES (87, 56, '删除公告', 'message:notice:delete', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-09 21:07:34', '2026-05-09 21:07:34');
INSERT INTO `sys_menu` VALUES (88, 56, '撤回公告', 'message:notice:withdraw', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-09 21:07:34', '2026-05-09 21:07:34');
INSERT INTO `sys_menu` VALUES (89, 28, '删除房源', 'house:house:delete', 3, NULL, NULL, NULL, 9, 1, 1, 0, '2026-05-10 00:48:43', '2026-05-10 01:43:02');
INSERT INTO `sys_menu` VALUES (90, 37, '导出项目', 'house:project:export', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-10 00:49:20', '2026-05-10 00:49:20');
INSERT INTO `sys_menu` VALUES (92, 37, '变动日志', 'house:project:log', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-10 00:49:20', '2026-05-13 00:31:12');
INSERT INTO `sys_menu` VALUES (93, 72, '查询日志', 'house:log:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-10 00:49:36', '2026-05-10 00:49:36');
INSERT INTO `sys_menu` VALUES (94, 72, '导出日志', 'house:log:export', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-10 00:49:36', '2026-05-10 00:49:36');
INSERT INTO `sys_menu` VALUES (95, 41, '客户列表', 'trade:appuser', 2, 'appuser', 'trade/appuser/index', 'UserFilled', 10, 1, 1, 0, '2026-05-10 10:44:50', '2026-05-10 10:44:50');
INSERT INTO `sys_menu` VALUES (96, 95, '查询客户', 'trade:appuser:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-10 10:44:50', '2026-05-10 10:44:50');
INSERT INTO `sys_menu` VALUES (97, 95, '新增客户', 'trade:appuser:add', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-10 10:44:50', '2026-05-10 10:44:50');
INSERT INTO `sys_menu` VALUES (98, 95, '修改客户', 'trade:appuser:edit', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-10 10:44:50', '2026-05-10 10:44:50');
INSERT INTO `sys_menu` VALUES (99, 95, '删除客户', 'trade:appuser:delete', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-10 10:44:50', '2026-05-10 10:44:50');
INSERT INTO `sys_menu` VALUES (100, 95, '导出客户', 'trade:appuser:export', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-10 10:44:50', '2026-05-10 10:44:50');
INSERT INTO `sys_menu` VALUES (101, 95, '修改状态', 'trade:appuser:status', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-10 10:44:50', '2026-05-10 10:44:50');
INSERT INTO `sys_menu` VALUES (102, 42, '导出订单', 'trade:order:export', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-10 20:11:08', '2026-05-10 20:11:08');
INSERT INTO `sys_menu` VALUES (103, 42, '打印合同', 'trade:order:print', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-10 20:11:08', '2026-05-10 20:11:08');
INSERT INTO `sys_menu` VALUES (104, 42, '下载凭证', 'trade:order:download', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-10 20:11:08', '2026-05-10 20:11:08');
INSERT INTO `sys_menu` VALUES (105, 46, '公海池', 'trade:customer:public-pool', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-10 20:12:53', '2026-05-10 20:12:53');
INSERT INTO `sys_menu` VALUES (106, 46, '跟进记录', 'trade:customer:followup', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-10 20:12:53', '2026-05-10 20:12:53');
INSERT INTO `sys_menu` VALUES (107, 61, '足迹查询', 'trade:history:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-10 20:18:58', '2026-05-10 20:18:58');
INSERT INTO `sys_menu` VALUES (108, 61, '录入轨迹', 'trade:history:add', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-10 20:18:58', '2026-05-10 20:18:58');
INSERT INTO `sys_menu` VALUES (109, 61, '导出报告', 'trade:history:export', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-10 20:18:58', '2026-05-10 20:18:58');
INSERT INTO `sys_menu` VALUES (110, 62, '查看画像', 'trade:favorite:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-10 20:58:07', '2026-05-10 20:58:07');
INSERT INTO `sys_menu` VALUES (111, 28, '指派销售', 'house:house:assign', 3, NULL, NULL, NULL, 10, 1, 1, 0, '2026-05-11 16:19:56', '2026-05-11 16:19:56');
INSERT INTO `sys_menu` VALUES (112, 42, '指派订单销售', 'trade:order:assign', 3, NULL, NULL, NULL, 20, 1, 1, 0, '2026-05-11 16:27:22', '2026-05-11 16:27:22');
INSERT INTO `sys_menu` VALUES (113, 41, '过户管理', 'trade:transfer', 2, 'transfer', 'trade/transfer/index', 'Finished', 5, 1, 1, 0, '2026-05-12 10:00:00', '2026-05-12 10:00:00');
INSERT INTO `sys_menu` VALUES (114, 113, '查询过户', 'trade:transfer:query', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-12 10:00:00', '2026-05-12 10:00:00');
INSERT INTO `sys_menu` VALUES (115, 113, '创建过户', 'trade:transfer:add', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-12 10:00:00', '2026-05-12 10:00:00');
INSERT INTO `sys_menu` VALUES (116, 113, '确认过户', 'trade:transfer:complete', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-12 10:00:00', '2026-05-12 10:00:00');
INSERT INTO `sys_menu` VALUES (117, 113, '上传文件', 'trade:transfer:upload', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-12 10:00:00', '2026-05-12 10:00:00');
INSERT INTO `sys_menu` VALUES (118, 59, '佣金列表', 'finance:commission:list', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-12 23:44:16', '2026-05-12 23:44:16');
INSERT INTO `sys_menu` VALUES (119, 59, '佣金查询', 'finance:commission:query', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-12 23:44:16', '2026-05-12 23:44:16');
INSERT INTO `sys_menu` VALUES (120, 59, '佣金核算', 'finance:commission:calculate', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-12 23:44:16', '2026-05-12 23:44:16');
INSERT INTO `sys_menu` VALUES (121, 59, '佣金发放', 'finance:commission:issue', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-12 23:44:16', '2026-05-12 23:44:16');
INSERT INTO `sys_menu` VALUES (122, 60, '楼盘报表', 'finance:report:project', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-13 00:10:43', '2026-05-13 00:10:43');
INSERT INTO `sys_menu` VALUES (123, 60, '销售业绩报表', 'finance:report:sales', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-13 00:11:32', '2026-05-13 00:11:32');
INSERT INTO `sys_menu` VALUES (124, 60, '收支趋势报表', 'finance:report:trend', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-13 00:12:15', '2026-05-13 00:12:15');
INSERT INTO `sys_menu` VALUES (125, 46, '公海池领取', 'trade:customer:claim', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-14 20:33:34', '2026-05-14 20:33:34');
INSERT INTO `sys_menu` VALUES (126, 46, '预约带看', 'trade:customer:appointment', 3, NULL, NULL, NULL, 7, 1, 1, 0, '2026-05-14 20:34:00', '2026-05-14 20:34:00');
INSERT INTO `sys_menu` VALUES (127, 74, '新增成员', 'team:user:add', 3, NULL, NULL, NULL, 1, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');
INSERT INTO `sys_menu` VALUES (128, 74, '导出名册', 'team:user:export', 3, NULL, NULL, NULL, 2, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');
INSERT INTO `sys_menu` VALUES (129, 74, '编辑月度目标', 'team:performance:edit', 3, NULL, NULL, NULL, 3, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');
INSERT INTO `sys_menu` VALUES (130, 74, '调整部门', 'team:user:dept', 3, NULL, NULL, NULL, 4, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');
INSERT INTO `sys_menu` VALUES (131, 74, '设为负责人', 'team:user:promote', 3, NULL, NULL, NULL, 5, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');
INSERT INTO `sys_menu` VALUES (132, 74, '工作交接', 'team:user:handover', 3, NULL, NULL, NULL, 6, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');
INSERT INTO `sys_menu` VALUES (133, 74, '移除部门', 'team:user:remove', 3, NULL, NULL, NULL, 7, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');
INSERT INTO `sys_menu` VALUES (134, 74, '快捷修改状态', 'team:user:status', 3, NULL, NULL, NULL, 8, 1, 1, 0, '2026-05-19 00:22:29', '2026-05-19 00:22:29');

-- ----------------------------
-- Table structure for sys_notification
-- ----------------------------
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `content_type` tinyint NULL DEFAULT 1 COMMENT '内容格式：1=纯文本，2=HTML，3=Markdown',
  `notice_type` tinyint NOT NULL COMMENT '通知类型：1=系统通知，2=任务提醒，3=交易提醒，4=审批通知',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联业务类型',
  `business_id` int NULL DEFAULT NULL COMMENT '关联业务ID',
  `router_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '跳转路径',
  `priority` tinyint NULL DEFAULT 2 COMMENT '优先级：1=紧急，2=重要，3=普通',
  `sender_id` int NOT NULL COMMENT '发送人ID',
  `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发送人姓名',
  `receiver_type` tinyint NOT NULL COMMENT '接收类型：1=指定用户，2=部门，3=角色，4=全体',
  `receiver_ids` json NULL COMMENT '接收者ID列表',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `send_time` datetime NULL DEFAULT NULL COMMENT '发送时间',
  `withdraw_status` tinyint NULL DEFAULT 0 COMMENT '撤回状态：0=正常，1=已撤回',
  `withdraw_time` datetime NULL DEFAULT NULL COMMENT '撤回时间',
  `total_receiver_count` int NULL DEFAULT 0 COMMENT '应接收人数',
  `read_count` int NULL DEFAULT 0 COMMENT '已读人数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=草稿，1=已发送',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `extra_data` json NULL COMMENT '扩展数据',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_notice_type`(`notice_type` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE,
  INDEX `idx_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  CONSTRAINT `sys_notification_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作通知表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_notification
-- ----------------------------

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作模块',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务类型，如 HOUSE, CUSTOMER, TRANSACTION',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型，如 ADD, UPDATE, DELETE, EXPORT',
  `operation_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作描述',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_params` json NULL COMMENT '请求参数(JSON)',
  `response_result` json NULL COMMENT '响应结果(JSON)',
  `user_id` int NULL DEFAULT NULL COMMENT '操作人ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人用户名',
  `dept_id` int NULL DEFAULT NULL COMMENT '操作人部门ID',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器UA',
  `execute_time` bigint NULL DEFAULT NULL COMMENT '执行耗时(ms)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=失败，1=成功',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `risk_level` tinyint NULL DEFAULT 3 COMMENT '风险等级：1=高危，2=中等，3=普通',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_operation_time`(`operation_time` ASC) USING BTREE,
  INDEX `idx_business_type`(`business_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色代码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `data_scope` tinyint NULL DEFAULT 1 COMMENT '数据权限：1=全部，2=本部门，3=本部门及子部门，4=仅本人',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `create_user_id` int NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE,
  INDEX `idx_create_user_id`(`create_user_id` ASC) USING BTREE,
  CONSTRAINT `sys_role_ibfk_1` FOREIGN KEY (`create_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '管理员', 'admin', NULL, 1, 1, 0, NULL, '2026-05-07 05:58:46', '2026-05-09 22:27:49');
INSERT INTO `sys_role` VALUES (2, '销售专员', 'sales', NULL, 4, 1, 0, NULL, '2026-05-07 05:58:46', '2026-05-09 23:15:39');
INSERT INTO `sys_role` VALUES (3, '财务专员', 'finance', NULL, 1, 1, 0, NULL, '2026-05-07 05:58:46', '2026-05-19 17:45:30');
INSERT INTO `sys_role` VALUES (4, '销售经理', 'sales_manager', NULL, 3, 1, 0, NULL, '2026-05-09 03:24:12', '2026-05-09 23:11:29');
INSERT INTO `sys_role` VALUES (5, '财务经理', 'finance_manager', NULL, 1, 1, 0, NULL, '2026-05-09 03:24:12', '2026-05-19 17:45:34');
INSERT INTO `sys_role` VALUES (6, 'user', 'user', '仅拥有基础权限', 4, 1, 0, NULL, '2026-05-09 22:32:07', '2026-05-09 22:32:35');
INSERT INTO `sys_role` VALUES (7, 'systemBot', 'rbot', '仅用于发送通知', 4, 1, 0, NULL, '2026-05-12 12:43:27', '2026-05-12 12:43:27');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL COMMENT '角色ID',
  `menu_id` int NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE,
  CONSTRAINT `sys_role_menu_ibfk_1` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_role_menu_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 125 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1, 1);
INSERT INTO `sys_role_menu` VALUES (2, 1, 4);
INSERT INTO `sys_role_menu` VALUES (3, 1, 5);
INSERT INTO `sys_role_menu` VALUES (4, 1, 6);
INSERT INTO `sys_role_menu` VALUES (5, 1, 7);
INSERT INTO `sys_role_menu` VALUES (6, 1, 8);
INSERT INTO `sys_role_menu` VALUES (7, 1, 9);
INSERT INTO `sys_role_menu` VALUES (8, 1, 10);
INSERT INTO `sys_role_menu` VALUES (9, 1, 11);
INSERT INTO `sys_role_menu` VALUES (10, 1, 12);
INSERT INTO `sys_role_menu` VALUES (11, 1, 13);
INSERT INTO `sys_role_menu` VALUES (12, 1, 14);
INSERT INTO `sys_role_menu` VALUES (13, 1, 15);
INSERT INTO `sys_role_menu` VALUES (14, 1, 16);
INSERT INTO `sys_role_menu` VALUES (15, 1, 17);
INSERT INTO `sys_role_menu` VALUES (16, 1, 18);
INSERT INTO `sys_role_menu` VALUES (17, 1, 19);
INSERT INTO `sys_role_menu` VALUES (18, 1, 20);
INSERT INTO `sys_role_menu` VALUES (19, 1, 21);
INSERT INTO `sys_role_menu` VALUES (20, 1, 22);
INSERT INTO `sys_role_menu` VALUES (21, 1, 23);
INSERT INTO `sys_role_menu` VALUES (22, 1, 24);
INSERT INTO `sys_role_menu` VALUES (23, 1, 25);
INSERT INTO `sys_role_menu` VALUES (24, 1, 26);
INSERT INTO `sys_role_menu` VALUES (25, 1, 27);
INSERT INTO `sys_role_menu` VALUES (26, 1, 28);
INSERT INTO `sys_role_menu` VALUES (27, 1, 29);
INSERT INTO `sys_role_menu` VALUES (28, 1, 30);
INSERT INTO `sys_role_menu` VALUES (29, 1, 31);
INSERT INTO `sys_role_menu` VALUES (30, 1, 32);
INSERT INTO `sys_role_menu` VALUES (31, 1, 33);
INSERT INTO `sys_role_menu` VALUES (32, 1, 34);
INSERT INTO `sys_role_menu` VALUES (33, 1, 35);
INSERT INTO `sys_role_menu` VALUES (34, 1, 36);
INSERT INTO `sys_role_menu` VALUES (35, 1, 37);
INSERT INTO `sys_role_menu` VALUES (36, 1, 38);
INSERT INTO `sys_role_menu` VALUES (37, 1, 39);
INSERT INTO `sys_role_menu` VALUES (38, 1, 40);
INSERT INTO `sys_role_menu` VALUES (39, 1, 41);
INSERT INTO `sys_role_menu` VALUES (40, 1, 42);
INSERT INTO `sys_role_menu` VALUES (41, 1, 43);
INSERT INTO `sys_role_menu` VALUES (42, 1, 44);
INSERT INTO `sys_role_menu` VALUES (43, 1, 45);
INSERT INTO `sys_role_menu` VALUES (44, 1, 46);
INSERT INTO `sys_role_menu` VALUES (45, 1, 47);
INSERT INTO `sys_role_menu` VALUES (46, 1, 48);
INSERT INTO `sys_role_menu` VALUES (47, 1, 49);
INSERT INTO `sys_role_menu` VALUES (48, 1, 50);
INSERT INTO `sys_role_menu` VALUES (49, 1, 51);
INSERT INTO `sys_role_menu` VALUES (50, 1, 52);
INSERT INTO `sys_role_menu` VALUES (51, 1, 53);
INSERT INTO `sys_role_menu` VALUES (52, 1, 54);
INSERT INTO `sys_role_menu` VALUES (53, 1, 55);
INSERT INTO `sys_role_menu` VALUES (54, 1, 56);
INSERT INTO `sys_role_menu` VALUES (55, 1, 57);
INSERT INTO `sys_role_menu` VALUES (56, 1, 58);
INSERT INTO `sys_role_menu` VALUES (57, 1, 59);
INSERT INTO `sys_role_menu` VALUES (58, 1, 60);
INSERT INTO `sys_role_menu` VALUES (59, 1, 61);
INSERT INTO `sys_role_menu` VALUES (60, 1, 62);
INSERT INTO `sys_role_menu` VALUES (61, 1, 70);
INSERT INTO `sys_role_menu` VALUES (62, 1, 71);
INSERT INTO `sys_role_menu` VALUES (63, 1, 72);
INSERT INTO `sys_role_menu` VALUES (64, 1, 73);
INSERT INTO `sys_role_menu` VALUES (65, 1, 74);
INSERT INTO `sys_role_menu` VALUES (66, 1, 75);
INSERT INTO `sys_role_menu` VALUES (67, 1, 76);
INSERT INTO `sys_role_menu` VALUES (68, 1, 77);
INSERT INTO `sys_role_menu` VALUES (69, 1, 78);
INSERT INTO `sys_role_menu` VALUES (70, 1, 79);
INSERT INTO `sys_role_menu` VALUES (71, 1, 80);
INSERT INTO `sys_role_menu` VALUES (72, 1, 81);
INSERT INTO `sys_role_menu` VALUES (73, 1, 82);
INSERT INTO `sys_role_menu` VALUES (74, 1, 83);
INSERT INTO `sys_role_menu` VALUES (75, 1, 84);
INSERT INTO `sys_role_menu` VALUES (76, 1, 85);
INSERT INTO `sys_role_menu` VALUES (77, 1, 86);
INSERT INTO `sys_role_menu` VALUES (78, 1, 87);
INSERT INTO `sys_role_menu` VALUES (79, 1, 88);
INSERT INTO `sys_role_menu` VALUES (80, 1, 89);
INSERT INTO `sys_role_menu` VALUES (81, 1, 90);
INSERT INTO `sys_role_menu` VALUES (82, 1, 92);
INSERT INTO `sys_role_menu` VALUES (83, 1, 93);
INSERT INTO `sys_role_menu` VALUES (84, 1, 94);
INSERT INTO `sys_role_menu` VALUES (85, 1, 95);
INSERT INTO `sys_role_menu` VALUES (86, 1, 96);
INSERT INTO `sys_role_menu` VALUES (87, 1, 97);
INSERT INTO `sys_role_menu` VALUES (88, 1, 98);
INSERT INTO `sys_role_menu` VALUES (89, 1, 99);
INSERT INTO `sys_role_menu` VALUES (90, 1, 100);
INSERT INTO `sys_role_menu` VALUES (91, 1, 101);
INSERT INTO `sys_role_menu` VALUES (92, 1, 102);
INSERT INTO `sys_role_menu` VALUES (93, 1, 103);
INSERT INTO `sys_role_menu` VALUES (94, 1, 104);
INSERT INTO `sys_role_menu` VALUES (95, 1, 105);
INSERT INTO `sys_role_menu` VALUES (96, 1, 106);
INSERT INTO `sys_role_menu` VALUES (97, 1, 107);
INSERT INTO `sys_role_menu` VALUES (98, 1, 108);
INSERT INTO `sys_role_menu` VALUES (99, 1, 109);
INSERT INTO `sys_role_menu` VALUES (100, 1, 110);
INSERT INTO `sys_role_menu` VALUES (101, 1, 111);
INSERT INTO `sys_role_menu` VALUES (102, 1, 112);
INSERT INTO `sys_role_menu` VALUES (103, 1, 113);
INSERT INTO `sys_role_menu` VALUES (104, 1, 114);
INSERT INTO `sys_role_menu` VALUES (105, 1, 115);
INSERT INTO `sys_role_menu` VALUES (106, 1, 116);
INSERT INTO `sys_role_menu` VALUES (107, 1, 117);
INSERT INTO `sys_role_menu` VALUES (108, 1, 118);
INSERT INTO `sys_role_menu` VALUES (109, 1, 119);
INSERT INTO `sys_role_menu` VALUES (110, 1, 120);
INSERT INTO `sys_role_menu` VALUES (111, 1, 121);
INSERT INTO `sys_role_menu` VALUES (112, 1, 122);
INSERT INTO `sys_role_menu` VALUES (113, 1, 123);
INSERT INTO `sys_role_menu` VALUES (114, 1, 124);
INSERT INTO `sys_role_menu` VALUES (115, 1, 125);
INSERT INTO `sys_role_menu` VALUES (116, 1, 126);
INSERT INTO `sys_role_menu` VALUES (117, 1, 127);
INSERT INTO `sys_role_menu` VALUES (118, 1, 128);
INSERT INTO `sys_role_menu` VALUES (119, 1, 129);
INSERT INTO `sys_role_menu` VALUES (120, 1, 130);
INSERT INTO `sys_role_menu` VALUES (121, 1, 131);
INSERT INTO `sys_role_menu` VALUES (122, 1, 132);
INSERT INTO `sys_role_menu` VALUES (123, 1, 133);
INSERT INTO `sys_role_menu` VALUES (124, 1, 134);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '真实姓名',
  `nick_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `sex` tinyint NULL DEFAULT 0 COMMENT '性别：0=未知，1=男，2=女',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `dept_id` int NULL DEFAULT NULL COMMENT '所属部门ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=禁用，1=正常',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$Fl89nMYNBWkkKAIyPP6zQOOv/B4XuNO4oWf8uVsuhvU1FL/xVJHfK', '超级管理员', 'Auth', '13800000001', '', 0, 'avatar/2026/05/13/a7de55401a164a879529d35c912f38de.jpg', 1, 1, 0, NULL, NULL, NULL, '2026-05-07 05:58:46', '2026-05-13 19:00:10');
INSERT INTO `sys_user` VALUES (2, 'sales01', '$2a$10$Fl89nMYNBWkkKAIyPP6zQOOv/B4XuNO4oWf8uVsuhvU1FL/xVJHfK', '张销售', 'Ax', '13800000002', NULL, 0, NULL, 2, 1, 0, NULL, NULL, NULL, '2026-05-07 05:58:46', '2026-05-13 18:30:05');
INSERT INTO `sys_user` VALUES (3, 'fin01', '$2a$10$Fl89nMYNBWkkKAIyPP6zQOOv/B4XuNO4oWf8uVsuhvU1FL/xVJHfK', '李财务', 'nidayin', '13800000003', '', 0, '/api/profile/avatar/2026/05/19/3e904216ec1440739e5e0a868cf76b3c.jpg', 3, 1, 0, NULL, NULL, NULL, '2026-05-07 05:58:46', '2026-05-19 17:48:39');
INSERT INTO `sys_user` VALUES (4, 'zhangsan', '$2a$10$byFRJie00R0cAO65IbfY6.U3RtnWEVgsUFXl/jb7HyjKjra/dQQnW', '张三', 'ax', '18178329427', 'zhangsan@163.com', 1, '/api/profile/avatar/2026/05/13/9fd7a409b5024eb9b35f9298f1415cc7.jpg', 1, 1, 0, NULL, NULL, '', '2026-05-09 21:56:19', '2026-05-23 16:26:25');
INSERT INTO `sys_user` VALUES (5, 'lihua', '$2a$10$.XyW2vr1ZYis4MRKFW8unupKKaYjysp4iDtOGlw82eIzY6oiAvVIe', '李华', 'AX', '18923130123', '123@11.com', 0, NULL, 4, 1, 0, NULL, NULL, '', '2026-05-09 22:11:35', '2026-05-13 18:30:09');
INSERT INTO `sys_user` VALUES (6, 'tee', '$2a$10$FmpF9Rpr9mcxUItelw0.HefcbPUSbREp.utUMpdgj3SYJ.9WBOa0W', '刘世杰', 'liuXX', '18023566345', '1212@qq.com', 1, '/api/profile/avatar/2026/05/19/400e32a388094175958f1d751f9107e0.jpg', 4, 1, 0, NULL, NULL, '', '2026-05-09 22:21:06', '2026-05-19 18:36:07');
INSERT INTO `sys_user` VALUES (7, 'linyi', '$2a$10$t7acC4lGowPHfB9TILqmV.I1HIjiUj041GtpmkQUBZMNpki0jU.wG', '林一', 'llyi', '18712344567', '', 0, '/api/profile/avatar/2026/05/13/30740c4679054a2abd44a65ccfb5d0aa.jpg', 2, 1, 0, NULL, NULL, '', '2026-05-09 23:17:09', '2026-05-13 18:46:25');
INSERT INTO `sys_user` VALUES (8, 'rbot', '$2a$10$h3wQ.q/4EjNCYlbH/AV0PO/w2tmV1ctXZY6E7W8T.UvPimishSuSC', 'x', 'bot', '18990123123', 'boot@qq.com', 0, NULL, 1, 1, 0, NULL, NULL, '0', '2026-05-12 12:44:21', '2026-05-15 20:57:34');

-- ----------------------------
-- Table structure for sys_user_notification
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_notification`;
CREATE TABLE `sys_user_notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notification_id` int NOT NULL COMMENT '通知ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读：0=未读，1=已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notification_user`(`notification_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_read`(`user_id` ASC, `is_read` ASC) USING BTREE,
  CONSTRAINT `sys_user_notification_ibfk_1` FOREIGN KEY (`notification_id`) REFERENCES `sys_notification` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_user_notification_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户通知关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_notification
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  CONSTRAINT `sys_user_role_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `sys_user_role_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 2, 2, '2026-05-07 12:13:00');
INSERT INTO `sys_user_role` VALUES (2, 1, 1, '2026-05-09 21:55:01');
INSERT INTO `sys_user_role` VALUES (3, 4, 1, '2026-05-09 21:56:19');
INSERT INTO `sys_user_role` VALUES (4, 6, 2, '2026-05-09 23:06:54');
INSERT INTO `sys_user_role` VALUES (5, 6, 6, '2026-05-09 23:06:54');
INSERT INTO `sys_user_role` VALUES (6, 5, 2, '2026-05-09 23:07:00');
INSERT INTO `sys_user_role` VALUES (7, 5, 6, '2026-05-09 23:07:00');
INSERT INTO `sys_user_role` VALUES (8, 3, 3, '2026-05-09 23:07:08');
INSERT INTO `sys_user_role` VALUES (9, 3, 6, '2026-05-09 23:07:08');
INSERT INTO `sys_user_role` VALUES (10, 7, 6, '2026-05-09 23:17:48');
INSERT INTO `sys_user_role` VALUES (11, 7, 4, '2026-05-09 23:17:48');
INSERT INTO `sys_user_role` VALUES (12, 8, 7, '2026-05-15 20:57:34');
INSERT INTO `sys_user_role` VALUES (13, 8, 6, '2026-05-15 20:57:34');

-- ----------------------------
-- Table structure for tb_app_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_app_user`;
CREATE TABLE `tb_app_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'C端用户ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号（核心登录凭证）',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码（可选，主流App多为验证码或微信登录）',
  `wechat_openid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信OpenID（用于小程序快捷登录）',
  `union_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信UnionID（用于跨应用账号打通）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别：0=未知，1=男，2=女',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0=封禁，1=正常',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `uk_wechat_openid`(`wechat_openid` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'C端移动端用户账号表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_app_user
-- ----------------------------

-- ----------------------------
-- Table structure for tb_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `tb_chat_message`;
CREATE TABLE `tb_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '所属会话ID',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '回复的消息ID（引用回复）',
  `sender_type` tinyint NOT NULL DEFAULT 1 COMMENT '发送者类型：1=员工(sys_user)，2=C端客户(tb_app_user)',
  `sender_id` bigint NOT NULL COMMENT '发送者ID（根据 sender_type 对应不同用户表）',
  `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发送者名称快照（冗余，避免查询）',
  `sender_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者头像快照',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消息内容（文本类型时有值）',
  `msg_type` tinyint NOT NULL DEFAULT 1 COMMENT '消息类型：1=文本，2=图片，3=文件，4=系统提示',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件/图片地址',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `is_recalled` tinyint NOT NULL DEFAULT 0 COMMENT '是否已撤回：0=正常，1=已撤回',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_time`(`session_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_sender`(`sender_type` ASC, `sender_id` ASC) USING BTREE,
  CONSTRAINT `tb_chat_message_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `tb_chat_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表（多态发送者）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for tb_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `tb_chat_session`;
CREATE TABLE `tb_chat_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_type` tinyint NOT NULL DEFAULT 1 COMMENT '会话类型：1=员工私聊，2=员工群聊，3=销售与C端客户',
  `session_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话名称（群聊时有效）',
  `last_message_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后一条消息内容快照',
  `last_message_type` tinyint NULL DEFAULT 1 COMMENT '最后一条消息类型',
  `last_message_time` datetime NULL DEFAULT NULL COMMENT '最后一条消息发送时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_last_message_time`(`last_message_time` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天会话主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_chat_session
-- ----------------------------

-- ----------------------------
-- Table structure for tb_chat_session_member
-- ----------------------------
DROP TABLE IF EXISTS `tb_chat_session_member`;
CREATE TABLE `tb_chat_session_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `user_type` tinyint NOT NULL DEFAULT 1 COMMENT '用户类型：1=员工(sys_user)，2=C端客户(tb_app_user)',
  `user_id` bigint NOT NULL COMMENT '用户ID（根据 user_type 对应不同用户表）',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名称快照',
  `user_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像快照',
  `unread_count` int NOT NULL DEFAULT 0 COMMENT '未读消息数',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶：0=否，1=是',
  `is_disturb` tinyint NOT NULL DEFAULT 0 COMMENT '消息免打扰：0=否，1=是',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session_usertype_user`(`session_id` ASC, `user_type` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_query`(`user_type` ASC, `user_id` ASC) USING BTREE,
  CONSTRAINT `tb_chat_session_member_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `tb_chat_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会话成员关联表（多态用户）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_chat_session_member
-- ----------------------------

-- ----------------------------
-- Table structure for tb_commission
-- ----------------------------
DROP TABLE IF EXISTS `tb_commission`;
CREATE TABLE `tb_commission`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '佣金ID',
  `transaction_id` int NOT NULL COMMENT '交易ID',
  `sales_id` int NOT NULL COMMENT '销售ID',
  `commission_rate` decimal(5, 2) NOT NULL COMMENT '提成比例(%)',
  `amount` decimal(10, 2) NOT NULL COMMENT '佣金金额（元）',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0=待核算，1=已核算，2=已发放',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `calculate_time` datetime NULL DEFAULT NULL COMMENT '核算时间',
  `issue_time` datetime NULL DEFAULT NULL COMMENT '发放时间',
  `finance_id` int NULL DEFAULT NULL COMMENT '核算财务ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `bank_card_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发放银行卡号',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '财务核算备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_transaction_sales`(`transaction_id` ASC, `sales_id` ASC) USING BTREE,
  INDEX `idx_sales_id`(`sales_id` ASC) USING BTREE,
  INDEX `idx_finance_id`(`finance_id` ASC) USING BTREE,
  CONSTRAINT `tb_commission_ibfk_1` FOREIGN KEY (`finance_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `tb_commission_ibfk_2` FOREIGN KEY (`sales_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_commission_ibfk_3` FOREIGN KEY (`transaction_id`) REFERENCES `tb_transaction` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售佣金表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_commission
-- ----------------------------

-- ----------------------------
-- Table structure for tb_customer
-- ----------------------------
DROP TABLE IF EXISTS `tb_customer`;
CREATE TABLE `tb_customer`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `app_user_id` bigint NULL DEFAULT NULL COMMENT '绑定的C端账号ID',
  `customer_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户编号',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户电话',
  `id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号（脱敏存储）',
  `demand_area` decimal(10, 2) NULL DEFAULT NULL COMMENT '意向面积',
  `demand_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '意向价格',
  `demand_layout` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '意向户型',
  `demand_area_region` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '意向区域',
  `intention_level` tinyint NULL DEFAULT 2 COMMENT '意向等级：1=高，2=中，3=低',
  `sales_id` int NULL DEFAULT NULL COMMENT '对接销售ID',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户来源',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_customer_no`(`customer_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_customer_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `uk_app_user_id`(`app_user_id` ASC) USING BTREE,
  INDEX `idx_sales_id`(`sales_id` ASC) USING BTREE,
  INDEX `idx_intention_level`(`intention_level` ASC) USING BTREE,
  CONSTRAINT `tb_customer_ibfk_1` FOREIGN KEY (`sales_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_customer
-- ----------------------------

-- ----------------------------
-- Table structure for tb_department_target
-- ----------------------------
DROP TABLE IF EXISTS `tb_department_target`;
CREATE TABLE `tb_department_target`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dept_id` int NOT NULL COMMENT '部门ID',
  `target_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '月度, YYYY-MM',
  `target_amount` decimal(14, 2) NOT NULL COMMENT '部门月度目标',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dept_month`(`dept_id` ASC, `target_month` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '閮ㄩ棬鏈堝害鐩?爣琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_department_target
-- ----------------------------

-- ----------------------------
-- Table structure for tb_house
-- ----------------------------
DROP TABLE IF EXISTS `tb_house`;
CREATE TABLE `tb_house`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '房源ID',
  `house_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '房源编号',
  `project_id` int NOT NULL COMMENT '所属项目ID',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '冗余：项目名称（避免频繁JOIN）',
  `house_type` tinyint NOT NULL COMMENT '房源类型：1=新房，2=二手房，3=租房',
  `building_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '楼栋号',
  `unit_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单元号',
  `room_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '房号',
  `province` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '冗余：省',
  `city` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '冗余：市',
  `district` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '冗余：区',
  `area` decimal(10, 2) NOT NULL COMMENT '建筑面积（㎡）',
  `layout` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '户型',
  `floor` int NOT NULL COMMENT '所在楼层',
  `total_floor` int NOT NULL COMMENT '总楼层',
  `orientation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '朝向',
  `decoration` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '装修情况',
  `tags` json NULL COMMENT '房源标签',
  `unit_price_fen` bigint UNSIGNED NULL DEFAULT NULL COMMENT '单价（分/㎡）= 元/㎡ × 100，新房和二手房有值，租房为 NULL',
  `total_price_fen` bigint UNSIGNED NULL DEFAULT NULL COMMENT '总价（分）= 万元 × 1_000_000，二手房有值，新房可由单价×面积推算，租房为 NULL',
  `rent_price_fen` int UNSIGNED NULL DEFAULT NULL COMMENT '月租（分/月）= 元/月 × 100，仅租房有值，最高约 214,748 元/月，INT足够',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '房源描述',
  `sales_id` int NULL DEFAULT NULL COMMENT '负责销售ID',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0=待审核，1=在售，2=已预订，3=已成交，4=下架',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `coordinate` point NOT NULL,
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_house_no`(`house_no` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_city_district_area`(`city` ASC, `district` ASC, `area` ASC) USING BTREE,
  INDEX `idx_search_new`(`house_type` ASC, `status` ASC, `city` ASC, `district` ASC, `unit_price_fen` ASC) USING BTREE,
  INDEX `idx_search_second`(`house_type` ASC, `status` ASC, `city` ASC, `district` ASC, `total_price_fen` ASC) USING BTREE,
  INDEX `idx_search_rental`(`house_type` ASC, `status` ASC, `city` ASC, `district` ASC, `rent_price_fen` ASC) USING BTREE,
  INDEX `idx_status_area`(`status` ASC, `area` ASC) USING BTREE,
  INDEX `idx_project_status`(`project_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sales_status`(`sales_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房源主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_house
-- ----------------------------

-- ----------------------------
-- Table structure for tb_house_image
-- ----------------------------
DROP TABLE IF EXISTS `tb_house_image`;
CREATE TABLE `tb_house_image`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `house_id` int NOT NULL COMMENT '房源ID',
  `file_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储Key/相对路径（如 house/FC001/cover.jpg），由后端拼接BaseURL',
  `thumbnail_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图Key/相对路径',
  `image_type` tinyint NOT NULL COMMENT '图片类型：1=封面图，2=室内图，3=户型图，4=环境图，5=其他',
  `image_group` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片分组，如客厅、卧室、厨房等',
  `tags` json NULL COMMENT '标签，如[\"朝南\",\"带阳台\"]',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `is_default` tinyint NULL DEFAULT NULL COMMENT '是否默认首图：1=是（仅封面图），NULL=否',
  `file_size` int NULL DEFAULT NULL COMMENT '文件大小(KB)',
  `width` int NULL DEFAULT NULL COMMENT '图片宽度(px)',
  `height` int NULL DEFAULT NULL COMMENT '图片高度(px)',
  `upload_user_id` int NOT NULL COMMENT '上传人ID',
  `audit_status` tinyint NULL DEFAULT 0 COMMENT '审核状态：0=待审核，1=通过，2=驳回',
  `audit_user_id` int NULL DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_house_default`(`house_id` ASC, `is_default` ASC) USING BTREE COMMENT '每个房源仅一张默认首图',
  INDEX `idx_house_id`(`house_id` ASC) USING BTREE,
  INDEX `idx_image_type`(`image_type` ASC) USING BTREE,
  INDEX `idx_audit_status`(`audit_status` ASC) USING BTREE,
  INDEX `idx_upload_user_id`(`upload_user_id` ASC) USING BTREE,
  INDEX `idx_audit_user_id`(`audit_user_id` ASC) USING BTREE,
  CONSTRAINT `tb_house_image_ibfk_1` FOREIGN KEY (`audit_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `tb_house_image_ibfk_2` FOREIGN KEY (`house_id`) REFERENCES `tb_house` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `tb_house_image_ibfk_3` FOREIGN KEY (`upload_user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房源图片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_house_image
-- ----------------------------

-- ----------------------------
-- Table structure for tb_house_status_log
-- ----------------------------
DROP TABLE IF EXISTS `tb_house_status_log`;
CREATE TABLE `tb_house_status_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `house_id` int NOT NULL COMMENT '房源ID',
  `from_status` tinyint NULL DEFAULT NULL COMMENT '变更前状态（首次创建时为NULL）',
  `to_status` tinyint NOT NULL COMMENT '变更后状态',
  `change_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更原因',
  `operator_id` int NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人姓名',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_house_id`(`house_id` ASC) USING BTREE,
  INDEX `idx_operator_id`(`operator_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `tb_house_status_log_ibfk_1` FOREIGN KEY (`house_id`) REFERENCES `tb_house` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `tb_house_status_log_ibfk_2` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房源状态流转日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_house_status_log
-- ----------------------------

-- ----------------------------
-- Table structure for tb_loan_record
-- ----------------------------
DROP TABLE IF EXISTS `tb_loan_record`;
CREATE TABLE `tb_loan_record`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `transaction_id` int NOT NULL COMMENT '关联交易ID',
  `loan_amount` decimal(15, 2) NOT NULL COMMENT '贷款金额（元）',
  `bank_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '贷款银行',
  `loan_term` int NULL DEFAULT NULL COMMENT '贷款期限（月）',
  `interest_rate` decimal(5, 4) NULL DEFAULT NULL COMMENT '利率',
  `application_time` datetime NULL DEFAULT NULL COMMENT '申请时间',
  `approval_time` datetime NULL DEFAULT NULL COMMENT '审批时间',
  `disbursement_time` datetime NULL DEFAULT NULL COMMENT '放款时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=待申请，1=审核中，2=已放款，3=未通过',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_transaction_id`(`transaction_id` ASC) USING BTREE,
  CONSTRAINT `tb_loan_record_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `tb_transaction` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '贷款记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_loan_record
-- ----------------------------

-- ----------------------------
-- Table structure for tb_new_house_extend
-- ----------------------------
DROP TABLE IF EXISTS `tb_new_house_extend`;
CREATE TABLE `tb_new_house_extend`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `house_id` int NOT NULL COMMENT '房源ID',
  `pre_sale_license_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预售许可证号',
  `record_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '备案价（元/㎡），新房的官方备案价格',
  `avg_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '楼盘均价（元/㎡），冗余用于快速展示',
  `property_right_years` int NULL DEFAULT 70 COMMENT '产权年限',
  `estimated_delivery_date` date NULL DEFAULT NULL COMMENT '预计交房日期',
  `delivery_standard` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '毛坯' COMMENT '交付标准',
  `elevator_ratio` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '梯户比',
  `actual_area_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '得房率(%)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_house_id`(`house_id` ASC) USING BTREE,
  CONSTRAINT `tb_new_house_extend_ibfk_1` FOREIGN KEY (`house_id`) REFERENCES `tb_house` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '新房扩展信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_new_house_extend
-- ----------------------------

-- ----------------------------
-- Table structure for tb_payment
-- ----------------------------
DROP TABLE IF EXISTS `tb_payment`;
CREATE TABLE `tb_payment`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '收款ID',
  `transaction_id` int NOT NULL COMMENT '交易ID',
  `payment_plan_id` int NULL DEFAULT NULL COMMENT '关联的应收账单ID（用于对账核销）',
  `payment_type` tinyint NOT NULL COMMENT '款项类型：1=定金，2=首付款，3=尾款，4=中介费，5=贷款',
  `flow_type` tinyint NOT NULL DEFAULT 1 COMMENT '资金流向：1=收款，2=退款',
  `amount` decimal(12, 2) NOT NULL COMMENT '金额（元）',
  `payment_status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0=待确认，1=有效，2=已作废',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `payment_time` datetime NOT NULL COMMENT '变动时间',
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付方式',
  `receipt_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收据/发票编号',
  `proof_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '凭证图片路径',
  `payer_info` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '付款人备注',
  `finance_id` int NOT NULL COMMENT '经办财务ID',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `actual_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '实际到账金额',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '财务审核时间',
  `audit_user_id` int NULL DEFAULT NULL COMMENT '审核人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_receipt_no`(`receipt_no` ASC) USING BTREE,
  INDEX `idx_transaction_id`(`transaction_id` ASC) USING BTREE,
  INDEX `idx_finance_id`(`finance_id` ASC) USING BTREE,
  INDEX `fk_payment_plan`(`payment_plan_id` ASC) USING BTREE,
  CONSTRAINT `tb_payment_ibfk_1` FOREIGN KEY (`finance_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_payment_ibfk_2` FOREIGN KEY (`payment_plan_id`) REFERENCES `tb_payment_plan` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `tb_payment_ibfk_3` FOREIGN KEY (`transaction_id`) REFERENCES `tb_transaction` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收退款记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_payment
-- ----------------------------

-- ----------------------------
-- Table structure for tb_payment_plan
-- ----------------------------
DROP TABLE IF EXISTS `tb_payment_plan`;
CREATE TABLE `tb_payment_plan`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '账单ID',
  `transaction_id` int NOT NULL COMMENT '关联交易ID',
  `stage` int NOT NULL DEFAULT 1 COMMENT '期数/阶段 (0:定金, 1:首期/第一期, 2:第二期, 3:尾款/按揭款...)',
  `pay_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '款项名称 (如：定金、首付款、第二期分期款、银行按揭贷款)',
  `receivable_amount` decimal(12, 2) NOT NULL COMMENT '应收金额（元）',
  `paid_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '已收金额（元）',
  `due_date` datetime NOT NULL COMMENT '应收截止时间/应还日期',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '账单状态：0=待付款，1=部分付款，2=已结清',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_transaction_id`(`transaction_id` ASC) USING BTREE,
  CONSTRAINT `tb_payment_plan_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `tb_transaction` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交易付款应收账单计划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_payment_plan
-- ----------------------------

-- ----------------------------
-- Table structure for tb_project
-- ----------------------------
DROP TABLE IF EXISTS `tb_project`;
CREATE TABLE `tb_project`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编号',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `project_type` tinyint NOT NULL DEFAULT 1 COMMENT '项目类型：1=新房楼盘，2=二手房小区',
  `developer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开发商',
  `property_company` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物业公司',
  `province` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省',
  `city` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市',
  `district` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细地址',
  `total_households` int NULL DEFAULT NULL COMMENT '总户数',
  `property_fee` decimal(8, 2) NULL DEFAULT NULL COMMENT '物业费（元/㎡/月）',
  `plot_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '容积率',
  `greening_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '绿化率(%)',
  `tags` json NULL COMMENT '项目标签',
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图',
  `coordinate` point NULL,
  `longitude` decimal(10, 6) GENERATED ALWAYS AS (st_longitude(`coordinate`)) STORED COMMENT '经度（虚拟列）' NULL,
  `latitude` decimal(10, 6) GENERATED ALWAYS AS (st_latitude(`coordinate`)) STORED COMMENT '纬度（虚拟列）' NULL,
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=在售，2=售罄，3=待售，4=下架',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `creator_id` int NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `commission_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '佣金比例（%）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_project_no`(`project_no` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_city_district`(`city` ASC, `district` ASC) USING BTREE,
  INDEX `idx_creator_id`(`creator_id` ASC) USING BTREE,
  CONSTRAINT `tb_project_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '楼盘项目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_project
-- ----------------------------

-- ----------------------------
-- Table structure for tb_project_log
-- ----------------------------
DROP TABLE IF EXISTS `tb_project_log`;
CREATE TABLE `tb_project_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `project_id` int NOT NULL COMMENT '项目ID',
  `field_label` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更字段中文名',
  `old_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更前值',
  `new_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更后值',
  `operator_id` int NULL DEFAULT 0 COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目变更日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_project_log
-- ----------------------------

-- ----------------------------
-- Table structure for tb_rent_house_extend
-- ----------------------------
DROP TABLE IF EXISTS `tb_rent_house_extend`;
CREATE TABLE `tb_rent_house_extend`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `house_id` int NOT NULL COMMENT '房源ID',
  `monthly_rent` decimal(10, 2) NULL DEFAULT NULL COMMENT '月租金（元/月），冗余用于快速展示',
  `rent_type` tinyint NULL DEFAULT 1 COMMENT '出租方式：1=整租，2=合租',
  `deposit_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '押金方式，如押一付三、押二付一',
  `deposit_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '押金金额（元）',
  `check_in_date` date NULL DEFAULT NULL COMMENT '可入住日期',
  `min_lease_period` int NULL DEFAULT NULL COMMENT '最短租期（月）',
  `support_short_rent` tinyint NULL DEFAULT 0 COMMENT '是否支持短租',
  `appliances` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配套设施，逗号分隔',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_house_id`(`house_id` ASC) USING BTREE,
  CONSTRAINT `tb_rent_house_extend_ibfk_1` FOREIGN KEY (`house_id`) REFERENCES `tb_house` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租房扩展信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_rent_house_extend
-- ----------------------------

-- ----------------------------
-- Table structure for tb_second_house_extend
-- ----------------------------
DROP TABLE IF EXISTS `tb_second_house_extend`;
CREATE TABLE `tb_second_house_extend`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `house_id` int NOT NULL COMMENT '房源ID',
  `total_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '二手房总价（万元），冗余用于快速展示',
  `build_year` int NULL DEFAULT NULL COMMENT '建筑年代',
  `house_usage` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '房屋用途',
  `is_only_house` tinyint NULL DEFAULT 0 COMMENT '是否唯一住房',
  `is_full_two` tinyint NULL DEFAULT 0 COMMENT '是否满二',
  `is_full_five` tinyint NULL DEFAULT 0 COMMENT '是否满五',
  `mortgage_status` tinyint NULL DEFAULT 0 COMMENT '抵押状态：0=无抵押，1=有抵押',
  `property_deed` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '房本信息',
  `property_deed_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '房本图片URL',
  `last_transaction_time` datetime NULL DEFAULT NULL COMMENT '上次交易时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_house_id`(`house_id` ASC) USING BTREE,
  CONSTRAINT `tb_second_house_extend_ibfk_1` FOREIGN KEY (`house_id`) REFERENCES `tb_house` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '二手房扩展信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_second_house_extend
-- ----------------------------

-- ----------------------------
-- Table structure for tb_sync_failed_log
-- ----------------------------
DROP TABLE IF EXISTS `tb_sync_failed_log`;
CREATE TABLE `tb_sync_failed_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `business_id` int NOT NULL,
  `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_time` datetime NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status_next_retry`(`status` ASC, `next_retry_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_sync_failed_log
-- ----------------------------

-- ----------------------------
-- Table structure for tb_transaction
-- ----------------------------
DROP TABLE IF EXISTS `tb_transaction`;
CREATE TABLE `tb_transaction`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `transaction_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易编号',
  `house_id` int NOT NULL COMMENT '房源ID',
  `house_address` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '房源地址（冗余）',
  `customer_id` int NOT NULL COMMENT '客户ID',
  `customer_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户姓名（冗余）',
  `customer_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户电话（冗余）',
  `sales_id` int NOT NULL COMMENT '销售ID',
  `deal_price` decimal(12, 2) NOT NULL COMMENT '成交价格（元）',
  `deposit` decimal(10, 2) NOT NULL COMMENT '定金金额（元）',
  `payment_type` tinyint NULL DEFAULT NULL COMMENT '付款方式：1=一次性付款，2=分期付款，3=按揭贷款',
  `deposit_time` datetime NULL DEFAULT NULL COMMENT '定金支付时间',
  `down_payment` decimal(12, 2) NULL DEFAULT NULL COMMENT '首付款金额（元）',
  `down_payment_time` datetime NULL DEFAULT NULL COMMENT '首付款支付时间',
  `loan_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '贷款金额（元）',
  `loan_status` tinyint NULL DEFAULT 0 COMMENT '贷款状态：0=未申请，1=审核中，2=已放款，3=未通过',
  `transfer_time` datetime NULL DEFAULT NULL COMMENT '过户时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '交易状态：0=待付定金，1=已付定金，2=已付首付，3=已过户，4=已完成，5=已取消',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `manager_audit` tinyint NULL DEFAULT 0 COMMENT '经理审核：0=待审核，1=已通过，2=已驳回',
  `finish_audit` tinyint NULL DEFAULT 0 COMMENT '完成审核：0=未申请，1=待审核，2=已通过，3=已驳回',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `actual_paid_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '已收总金额',
  `next_payment_time` datetime NULL DEFAULT NULL COMMENT '预计下次收款时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_transaction_no`(`transaction_no` ASC) USING BTREE,
  INDEX `idx_house_id`(`house_id` ASC) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sales_id`(`sales_id` ASC) USING BTREE,
  CONSTRAINT `tb_transaction_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `tb_customer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_transaction_ibfk_2` FOREIGN KEY (`house_id`) REFERENCES `tb_house` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_transaction_ibfk_3` FOREIGN KEY (`sales_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '交易信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_transaction
-- ----------------------------

-- ----------------------------
-- Table structure for tb_transaction_fee
-- ----------------------------
DROP TABLE IF EXISTS `tb_transaction_fee`;
CREATE TABLE `tb_transaction_fee`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `transaction_id` int NOT NULL COMMENT '关联交易ID',
  `fee_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '费用类型：deed_tax=契税，income_tax=个税，vat=增值税，service_fee=中介费，other=其他',
  `fee_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '费用名称',
  `amount` decimal(15, 2) NOT NULL COMMENT '金额（元）',
  `payer` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付方：buyer=买方，seller=卖方，agent=中介',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '缴纳时间',
  `proof_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '缴费凭证',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_transaction_id`(`transaction_id` ASC) USING BTREE,
  CONSTRAINT `tb_transaction_fee_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `tb_transaction` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '交易费用表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_transaction_fee
-- ----------------------------

-- ----------------------------
-- Table structure for tb_transfer_document
-- ----------------------------
DROP TABLE IF EXISTS `tb_transfer_document`;
CREATE TABLE `tb_transfer_document`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `transfer_id` int NOT NULL COMMENT '关联过户记录ID',
  `doc_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件类型：tax_receipt=完税证明，new_deed=新产权证，app_form=受理凭证，other=其他',
  `doc_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件原名',
  `file_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_transfer_id`(`transfer_id` ASC) USING BTREE,
  CONSTRAINT `tb_transfer_document_ibfk_1` FOREIGN KEY (`transfer_id`) REFERENCES `tb_transfer_record` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '过户文件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_transfer_document
-- ----------------------------

-- ----------------------------
-- Table structure for tb_transfer_record
-- ----------------------------
DROP TABLE IF EXISTS `tb_transfer_record`;
CREATE TABLE `tb_transfer_record`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `transaction_id` int NOT NULL COMMENT '关联交易ID',
  `transfer_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '过户编号（TG+yyyyMMdd+4位随机）',
  `certificate_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '不动产权证书号',
  `registration_center` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '不动产登记中心名称',
  `transfer_date` datetime NULL DEFAULT NULL COMMENT '过户日期',
  `operator_id` int NULL DEFAULT NULL COMMENT '经办人ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=待过户，1=已完成',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_transaction_id`(`transaction_id` ASC) USING BTREE,
  INDEX `idx_transfer_no`(`transfer_no` ASC) USING BTREE,
  CONSTRAINT `tb_transfer_record_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `tb_transaction` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '过户记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_transfer_record
-- ----------------------------

-- ----------------------------
-- Table structure for tb_user_browse_history
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_browse_history`;
CREATE TABLE `tb_user_browse_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览ID',
  `app_user_id` bigint NOT NULL COMMENT 'C端用户ID',
  `resource_type` tinyint NOT NULL COMMENT '资源类型：1=房源，2=项目',
  `resource_id` int NOT NULL COMMENT '资源ID',
  `action_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'view' COMMENT '行为类型：view=浏览, call=电话, visit=带看, chat=咨询',
  `duration` int NULL DEFAULT 0 COMMENT '交互时长(秒)',
  `interest_level` tinyint NULL DEFAULT 3 COMMENT '意向评估(1-5星)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '交互备注',
  `view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交互时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_resource`(`app_user_id` ASC, `resource_type` ASC, `resource_id` ASC) USING BTREE,
  INDEX `idx_browse_time`(`view_time` ASC) USING BTREE,
  CONSTRAINT `tb_user_browse_history_ibfk_1` FOREIGN KEY (`app_user_id`) REFERENCES `tb_app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户浏览历史记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_user_browse_history
-- ----------------------------

-- ----------------------------
-- Table structure for tb_user_favorite
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_favorite`;
CREATE TABLE `tb_user_favorite`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `app_user_id` bigint NOT NULL COMMENT 'C端用户ID',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1=房源，2=楼盘项目',
  `target_id` int NOT NULL COMMENT '资源ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_target`(`app_user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`app_user_id` ASC) USING BTREE,
  CONSTRAINT `tb_user_favorite_ibfk_1` FOREIGN KEY (`app_user_id`) REFERENCES `tb_app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户收藏记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_user_favorite
-- ----------------------------

-- ----------------------------
-- Table structure for tb_view_record
-- ----------------------------
DROP TABLE IF EXISTS `tb_view_record`;
CREATE TABLE `tb_view_record`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '带看ID',
  `customer_id` int NOT NULL COMMENT '客户ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'visit' COMMENT '跟进类型：visit=实地带看, call=电话咨询, wechat=微信沟通, other=其他',
  `house_id` int NULL DEFAULT NULL COMMENT '房源ID（带看时必填）',
  `sales_id` int NOT NULL COMMENT '销售ID',
  `view_time` datetime NOT NULL COMMENT '带看时间',
  `status` tinyint NOT NULL DEFAULT 2 COMMENT '状态：0=待确认，1=已预约，2=已完成，3=已取消',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0=未删除，1=已删除',
  `appoint_type` tinyint NOT NULL DEFAULT 1 COMMENT '预约方式：1=销售录入，2=客户线上申请',
  `customer_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '客户反馈',
  `follow_advice` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '跟进建议',
  `cancel_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取消原因',
  `new_intention_level` tinyint NULL DEFAULT NULL COMMENT '调整后意向等级（1=高，2=中，3=低），不调整则留空',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_house_id`(`house_id` ASC) USING BTREE,
  INDEX `idx_sales_id`(`sales_id` ASC) USING BTREE,
  CONSTRAINT `tb_view_record_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `tb_customer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_view_record_ibfk_2` FOREIGN KEY (`house_id`) REFERENCES `tb_house` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_view_record_ibfk_3` FOREIGN KEY (`sales_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '带看记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of tb_view_record
-- ----------------------------

-- ----------------------------
-- Function structure for fn_check_user_permission
-- ----------------------------
DROP FUNCTION IF EXISTS `fn_check_user_permission`;
delimiter ;;
CREATE FUNCTION `fn_check_user_permission`(p_user_id INT, p_perm_code VARCHAR(100))
 RETURNS tinyint(1)
  READS SQL DATA 
BEGIN
    DECLARE v_count INT;
    
    SELECT COUNT(1) INTO v_count
    FROM sys_user_role ur
    JOIN sys_role_menu rm ON ur.role_id = rm.role_id
    JOIN sys_menu m ON rm.menu_id = m.id
    WHERE ur.user_id = p_user_id 
      AND m.menu_code = p_perm_code  -- 此处对应 sys_menu 表中的权限标识字段
      AND m.status = 1
      AND m.is_deleted = 0;
      
    RETURN IF(v_count > 0, 1, 0);
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for sp_change_house_status
-- ----------------------------
DROP PROCEDURE IF EXISTS `sp_change_house_status`;
delimiter ;;
CREATE PROCEDURE `sp_change_house_status`(IN p_house_id INT,
    IN p_new_status TINYINT,
    IN p_change_reason VARCHAR(500),
    IN p_operator_id INT,
    IN p_ip_address VARCHAR(50))
BEGIN
    DECLARE v_old_status TINYINT;
    DECLARE v_operator_name VARCHAR(50);
    
    -- 获取当前状态和操作人信息
    SELECT `status` INTO v_old_status FROM `tb_house` WHERE `id` = p_house_id;
    SELECT `real_name` INTO v_operator_name FROM `sys_user` WHERE `id` = p_operator_id;
    
    -- 开始事务
    START TRANSACTION;
    
    -- 1. 更新房源状态
    UPDATE `tb_house` 
    SET `status` = p_new_status, `update_time` = NOW()
    WHERE `id` = p_house_id;
    
    -- 2. 记录状态流转日志（审计用）
    INSERT INTO `tb_house_status_log` (
        `house_id`, `from_status`, `to_status`, `change_reason`,
        `operator_id`, `operator_name`, `ip_address`, `create_time`
    ) VALUES (
        p_house_id, v_old_status, p_new_status, p_change_reason,
        p_operator_id, v_operator_name, p_ip_address, NOW()
    );
    
    -- 提交事务
    COMMIT;
    
    SELECT 1 AS `result`, '房源状态变更成功' AS `message`;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for sp_change_user_role
-- ----------------------------
DROP PROCEDURE IF EXISTS `sp_change_user_role`;
delimiter ;;
CREATE PROCEDURE `sp_change_user_role`(IN p_user_id INT,
    IN p_new_role_id INT,
    IN p_operator_id INT,
    IN p_ip_address VARCHAR(50))
BEGIN
    DECLARE v_username VARCHAR(50);
    DECLARE v_real_name VARCHAR(50);
    DECLARE v_role_name VARCHAR(50);
    DECLARE v_operator_name VARCHAR(50);
    DECLARE v_dept_id INT;
    
    -- 获取用户和角色信息
    SELECT `username`, `real_name`, `dept_id` INTO v_username, v_real_name, v_dept_id 
    FROM `sys_user` WHERE `id` = p_user_id;
    
    SELECT `role_name` INTO v_role_name 
    FROM `sys_role` WHERE `id` = p_new_role_id;
    
    SELECT `real_name` INTO v_operator_name
    FROM `sys_user` WHERE `id` = p_operator_id;
    
    -- 开始事务
    START TRANSACTION;
    
    -- 1. 删除用户原有角色关联
    DELETE FROM `sys_user_role` WHERE `user_id` = p_user_id;
    
    -- 2. 添加新的角色关联
    INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`)
    VALUES (p_user_id, p_new_role_id, NOW());
    
    -- 3. 记录操作日志
    INSERT INTO `sys_operation_log` (
        `module`, `business_type`, `operation_type`, `operation_desc`, 
        `request_method`, `request_url`, `request_params`,
        `user_id`, `user_name`, `dept_id`, `ip_address`, `status`, `risk_level`, `operation_time`
    ) VALUES (
        '用户管理',
        'USER',
        'CHANGE_ROLE',
        CONCAT('修改用户 [', v_username, '] 的角色为 ', v_role_name),
        'POST',
        '/api/user/change-role',
        JSON_OBJECT('userId', p_user_id, 'newRoleId', p_new_role_id),
        p_operator_id,
        v_operator_name,
        v_dept_id,
        p_ip_address,
        1,
        1,
        NOW()
    );
    
    -- 提交事务
    COMMIT;
    
    -- 返回成功
    SELECT 1 AS `result`, '角色修改成功' AS `message`;
END
;;
delimiter ;

-- ----------------------------
-- Procedure structure for sp_find_nearby_houses
-- ----------------------------
DROP PROCEDURE IF EXISTS `sp_find_nearby_houses`;
delimiter ;;
CREATE PROCEDURE `sp_find_nearby_houses`(IN p_lng DECIMAL(10,6),   -- 中心点经度
    IN p_lat DECIMAL(10,6),   -- 中心点纬度
    IN p_radius INT,           -- 搜索半径（米）
    IN p_house_type TINYINT,   -- 房源类型（可选，NULL表示全部）
    IN p_limit INT)
BEGIN
    -- 使用空间索引进行高效范围查询
    SELECT 
        h.`id`,
        h.`house_no`,
        h.`project_name`,
        h.`house_type`,
        h.`city`,
        h.`district`,
        h.`area`,
        h.`layout`,
        h.`price`,
        h.`price_unit`,
        h.`status`,
        ST_Longitude(h.`coordinate`) AS `longitude`, -- 修正为 st_longitude 获取经度
        ST_Latitude(h.`coordinate`) AS `latitude`,   -- 修正为 st_latitude 获取纬度
        ST_Distance_Sphere(h.`coordinate`, ST_GeomFromText(CONCAT('POINT(', p_lat, ' ', p_lng, ')'), 4326)) AS `distance` -- 修正为 POINT(lat lng)
    FROM `tb_house` h
    WHERE h.`status` = 1  -- 在售状态
    AND (p_house_type IS NULL OR h.`house_type` = p_house_type)
    -- 使用 MBRContains 进行初步筛选（利用空间索引），再用 ST_Distance_Sphere 精确计算
    AND MBRContains(
        ST_Buffer(ST_GeomFromText(CONCAT('POINT(', p_lat, ' ', p_lng, ')'), 4326), p_radius / 111320.0), -- 修正为 POINT(lat lng)
        h.`coordinate`
    )
    HAVING `distance` <= p_radius
    ORDER BY `distance` ASC
    LIMIT p_limit;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
