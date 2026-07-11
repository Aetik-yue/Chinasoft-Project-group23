-- ============================================================
-- 截图存库改造：pet_media_record 增加 image_data 字段，file_url 改可空
-- 在公网 MySQL (47.108.58.107:3306 dream28) 执行一次即可
-- 对应前端办法 B：截图 base64 直接存数据库，上限 30 张自动顶掉最旧
-- ============================================================

ALTER TABLE `pet_media_record`
  ADD COLUMN `image_data` LONGTEXT NULL COMMENT '截图 base64（JPEG）；fileUrl 为空时使用' AFTER `file_url`,
  MODIFY COLUMN `file_url` VARCHAR(512) NULL COMMENT '文件资源URL（截图可空，改用 image_data）';

-- 验证：
-- DESC pet_media_record;
-- 应能看到 image_data 字段，且 file_url 的 Null 列为 YES。
