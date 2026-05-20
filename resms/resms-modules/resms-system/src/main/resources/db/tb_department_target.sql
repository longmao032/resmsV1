-- 部门月度目标表
CREATE TABLE IF NOT EXISTS `tb_department_target` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dept_id` int NOT NULL COMMENT '部门ID',
  `target_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标月份, YYYY-MM',
  `target_amount` decimal(14,2) NOT NULL COMMENT '目标业绩金额（万元）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_dept_month` (`dept_id`,`target_month`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门月度目标表';
