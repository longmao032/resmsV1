-- 扩展用户足迹表，支持更丰富的交互记录
ALTER TABLE `tb_user_browse_history` 
ADD COLUMN `action_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'view' COMMENT '行为类型：view=浏览, call=电话, visit=带看, chat=咨询' AFTER `resource_id`,
ADD COLUMN `duration` int NULL DEFAULT 0 COMMENT '交互时长(秒)' AFTER `action_type`,
ADD COLUMN `interest_level` tinyint NULL DEFAULT 3 COMMENT '意向评估(1-5星)' AFTER `duration`,
ADD COLUMN `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '交互备注' AFTER `interest_level`;

-- 修改字段名以符合通用语境 (从浏览时间改为交互时间)
ALTER TABLE `tb_user_browse_history` CHANGE COLUMN `browse_time` `view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交互时间';
