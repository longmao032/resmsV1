-- 合并 tb_follow_up_record 到 tb_view_record
-- 1. 扩展 tb_view_record，增加类型和意向调整字段
ALTER TABLE `tb_view_record`
  ADD COLUMN `type` varchar(20) NOT NULL DEFAULT 'visit' COMMENT '跟进类型：visit=实地带看, call=电话咨询, wechat=微信沟通, other=其他' AFTER `customer_id`,
  ADD COLUMN `new_intention_level` tinyint DEFAULT NULL COMMENT '调整后意向等级（1=高，2=中，3=低），不调整则留空' AFTER `cancel_reason`,
  MODIFY COLUMN `house_id` int DEFAULT NULL COMMENT '房源ID（带看时必填）';

-- 2. 迁移 tb_follow_up_record 数据到 tb_view_record
INSERT INTO `tb_view_record` (`customer_id`, `type`, `house_id`, `sales_id`, `view_time`, `status`, `appoint_type`, `customer_feedback`, `new_intention_level`, `is_deleted`, `create_time`, `update_time`)
SELECT
  `customer_id`,
  `type`,
  `house_id`,
  `sales_id`,
  `follow_date` AS `view_time`,
  2 AS `status`,
  1 AS `appoint_type`,
  `content` AS `customer_feedback`,
  `new_intention_level`,
  `is_deleted`,
  `create_time`,
  `update_time`
FROM `tb_follow_up_record`;

-- 3. 删除旧表
DROP TABLE IF EXISTS `tb_follow_up_record`;
