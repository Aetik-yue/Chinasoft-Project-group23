-- 将行为识别记录从“按设备共享”改为“按鹦鹉个体归属”。
-- 请在部署本次后端代码前执行一次。

ALTER TABLE `parrot_behavior_record`
  ADD COLUMN `pet_id` varchar(64) DEFAULT NULL
    COMMENT '所属鹦鹉业务ID；旧版未归属记录可为空' AFTER `device_id`,
  ADD KEY `idx_pet_checked` (`pet_id`, `checked_at`);

-- 仅当某个设备只绑定一个启用中的鹦鹉时，历史数据归属才是无歧义的，可安全回填。
-- 同一设备绑定多个鹦鹉的旧记录无法判断真实归属，保留 NULL 且不会进入任何个体统计。
UPDATE `parrot_behavior_record` AS behavior_record
JOIN (
  SELECT `device_id`, MIN(`pet_id`) AS `pet_id`
  FROM `pet_profile`
  WHERE `enabled` = 1
    AND `device_id` IS NOT NULL
    AND `device_id` <> ''
  GROUP BY `device_id`
  HAVING COUNT(*) = 1
) AS unique_binding
  ON unique_binding.`device_id` = behavior_record.`device_id`
SET behavior_record.`pet_id` = unique_binding.`pet_id`
WHERE behavior_record.`pet_id` IS NULL;
