-- 用户头像存储：新增 LONGTEXT 列存放 base64 data URI
-- 与 pet_media_record.image_data 存储方式一致
ALTER TABLE `sys_user`
  ADD COLUMN `avatar_image` LONGTEXT NULL COMMENT '头像 base64（JPEG data URI）' AFTER `avatar_url`;
