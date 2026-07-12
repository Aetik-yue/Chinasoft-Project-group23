# 数据表结构

> 本提示词用于 MaxKB 数据库工具（Text-to-SQL），让智能体根据用户自然语言问题生成 MySQL 查询语句，查询「智慧宠物烟感安全系统」的实时与历史数据。
> ⚠️ **当前覆盖范围**：本提示词覆盖 **17 张表**，分两类。① 烟感安全监测类（9 张）：smoke_device / smoke_data / temperature_data / humidity_data / alarm_record / device_control / environment_report_hourly / vision_check / system_setting。② 鹦鹉照护类（8 张）：sys_user / pet_profile / pet_weight_record / pet_media_record / pet_medical_record / pet_ledger_record / parrot_behavior_record / user_preference。覆盖账号、宠物档案、体重、媒体、病历、记账、行为识别、环境小时报表、AI 视觉复核、用户偏好等。
> ⚠️ 本提示词所列 17 张表及字段均为真实存在。库中**没有** `pet_cage`、`pet_daily_report`、`food_safety_query`、`alarm_timeline` 这几张表，切勿生成查询它们的 SQL。笼舍编号 `cage_id` 仅作为 `pet_profile` / `smoke_device` / `alarm_record` 上的自由字符串字段存在（如 `cage-001`），无独立笼舍表。
> ⚠️ 使用前请对照实际数据库表结构：如某张表或字段尚未建成，请从本提示词中删除对应 DDL，否则 LLM 生成的 SQL 会查询失败。
> ⚠️ 本工具仅允许 SELECT 只读查询，禁止 INSERT/UPDATE/DELETE/DDL。

# 表 1: 烟感设备表（smoke_device）

### 表说明
保存烟感设备的基本信息和最新状态：设备编号、安装位置、在线状态、最后心跳、当前烟雾/粉尘浓度、当前风险等级和当前告警状态。查询"设备是否在线""当前浓度多少""设备列表"用此表。注意：库中 `device_id` 有 `device-001` 与 `SMK-001` 两个值，环境/烟雾/温湿度历史数据多落在 `device-001`，联动控制记录落在 `SMK-001`。

### DDL语句
## 烟感设备表 smoke_device

CREATE TABLE `smoke_device` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备业务编号（如 device-001 / SMK-001）',
  `name` VARCHAR(128) NOT NULL COMMENT '设备名称',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '笼舍编号（自由字符串，如 cage-001，无独立笼舍表）',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '安装位置',
  `online` TINYINT NOT NULL DEFAULT 1 COMMENT '在线状态：1-在线, 0-离线',
  `last_heartbeat` DATETIME DEFAULT NULL COMMENT '最近心跳时间',
  `current_smoke_value` INT DEFAULT 0 COMMENT '当前烟雾/粉尘浓度（ppm，冗余缓存最新值）',
  `current_risk_level` VARCHAR(32) DEFAULT 'normal' COMMENT '当前风险等级：normal/low/medium/high',
  `current_alarm_status` VARCHAR(32) DEFAULT 'safe' COMMENT '当前报警状态：safe/alarm/offline',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用, 0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_online` (`online`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_alarm_status` (`current_alarm_status`),
  KEY `idx_cage_id` (`cage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感设备表';

# 表 2: 烟雾/粉尘历史数据表（smoke_data）

### 表说明
保存每一次烟雾/粉尘浓度采集记录，数据量最大。用于历史趋势图、最新数据追溯和告警判断依据。查询"历史浓度""最近浓度""浓度趋势"用此表。默认设备编号 `device-001`。

### DDL语句
## 烟雾/粉尘历史数据表 smoke_data

CREATE TABLE `smoke_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `smoke_value` FLOAT NOT NULL COMMENT '烟雾/粉尘浓度（ppm）',
  `risk_level` VARCHAR(32) NOT NULL COMMENT '风险等级：normal/low/medium/high',
  `record_time` DATETIME NOT NULL COMMENT '数据记录时间（传感器上报时间）',
  `source` VARCHAR(32) DEFAULT 'sensor' COMMENT '数据来源：sensor-真实传感器, simulate-模拟',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `record_time`),
  KEY `idx_record_time` (`record_time`),
  KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烟雾浓度历史数据表';

# 表 3: 温度历史数据表（temperature_data）

### 表说明
保存每一次温度采集记录，用于实时监控页温度展示和历史趋势分析。查询"当前温度""温度历史"用此表。默认设备编号 `device-001`。

### DDL语句
## 温度历史数据表 temperature_data

CREATE TABLE `temperature_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `temperature_value` FLOAT NOT NULL COMMENT '温度（℃）',
  `record_time` DATETIME NOT NULL COMMENT '数据记录时间（传感器上报时间）',
  `source` VARCHAR(32) DEFAULT 'sensor' COMMENT '数据来源：sensor-真实传感器, simulate-模拟',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `record_time`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温度历史数据表';

# 表 4: 湿度历史数据表（humidity_data）

### 表说明
保存每一次湿度采集记录，用于实时监控页湿度展示和历史趋势分析。查询"当前湿度""湿度历史"用此表。默认设备编号 `device-001`。

### DDL语句
## 湿度历史数据表 humidity_data

CREATE TABLE `humidity_data` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `humidity_value` FLOAT NOT NULL COMMENT '相对湿度（%RH）',
  `record_time` DATETIME NOT NULL COMMENT '数据记录时间（传感器上报时间）',
  `source` VARCHAR(32) DEFAULT 'sensor' COMMENT '数据来源：sensor-真实传感器, simulate-模拟',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `record_time`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='湿度历史数据表';

# 表 5: 告警记录表（alarm_record）

### 表说明
告警事件主表。当烟雾/粉尘值达到告警阈值、温湿度越界或设备离线时生成记录。查询"今天有哪些告警""最近告警""未处理告警""某告警详情"用此表。告警编号 `alarm_id` 为 UUID 字符串（如 `9aa52612-a010-48a1-9a78-0465d6ead11c`），**不是** `ALM-yyyymmdd-001` 形式。⚠️ 本表**没有 user_id 列**；查"某用户的告警"需先从 `pet_profile(user_id)` 取出该用户的 `device_id`，再关联 `alarm_record.device_id`。

### DDL语句
## 告警记录表 alarm_record

CREATE TABLE `alarm_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` VARCHAR(64) NOT NULL COMMENT '告警编号（UUID 字符串，业务唯一）',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '笼舍编号（自由字符串）',
  `alarm_type` VARCHAR(64) NOT NULL COMMENT '告警类型：smoke-烟雾/粉尘超限, temperature_low-温度过低, temperature_high-温度过高, humidity_low-湿度过低, humidity_high-湿度过高, device_offline-设备离线',
  `smoke_value` INT DEFAULT NULL COMMENT '触发时浓度（ppm，非烟雾告警为 NULL）',
  `risk_level` VARCHAR(32) NOT NULL COMMENT '风险等级：low/medium/high',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending-待处理, processing-处理中, resolved-已处理',
  `handler` VARCHAR(64) DEFAULT NULL COMMENT '处理人用户名',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `remark` VARCHAR(1000) DEFAULT NULL COMMENT '处理备注',
  `triggered_at` DATETIME NOT NULL COMMENT '告警触发时间',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '告警恢复时间',
  `is_simulated` TINYINT NOT NULL DEFAULT 0 COMMENT '是否模拟触发：1-是, 0-否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `alarm_message` VARCHAR(500) DEFAULT NULL COMMENT '告警消息文案（环境告警为"温度/湿度 X 越界"）',
  `alarm_value` DOUBLE DEFAULT NULL COMMENT '触发时实际数值（环境告警为越界的温度/湿度值）',
  `create_time` DATETIME(6) NOT NULL COMMENT '实体创建时间（微秒精度）',
  `threshold_value` DOUBLE DEFAULT NULL COMMENT '触发时阈值（环境告警为越界边界值）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_id` (`alarm_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_triggered_at` (`triggered_at`),
  KEY `idx_alarm_type` (`alarm_type`),
  KEY `idx_composite` (`device_id`, `status`, `triggered_at`),
  KEY `idx_cage_id` (`cage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感告警记录表';

# 表 6: 联动控制设备表（device_control）

### 表说明
蜂鸣器、报警灯、总开关等联动控制设备的状态。查询"蜂鸣器开了吗""报警灯状态""总开关""联动设备开关"用此表。⚠️ 库中 `control_type` 取值为 `buzzer`（蜂鸣器）/ `alarm_light`（报警灯）/ `switch`（总开关），**没有** `fan`（排风扇）和 `air_purifier`（空气净化器）。联动设备记录的 `device_id` 为 `SMK-001`。

### DDL语句
## 联动控制设备表 device_control

CREATE TABLE `device_control` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '所属烟感设备编号',
  `control_type` VARCHAR(64) NOT NULL COMMENT '控制设备类型：buzzer-蜂鸣器, alarm_light-报警灯, switch-总开关',
  `control_name` VARCHAR(128) DEFAULT NULL COMMENT '设备显示名称（如 蜂鸣器/报警灯/总开关）',
  `status` VARCHAR(32) NOT NULL DEFAULT 'off' COMMENT '当前状态：on-开启, off-关闭',
  `auto_linkage` TINYINT NOT NULL DEFAULT 1 COMMENT '是否参与自动联动：1-是, 0-否',
  `last_operated_at` DATETIME DEFAULT NULL COMMENT '最后操作时间',
  `last_operated_by` VARCHAR(64) DEFAULT NULL COMMENT '最后操作人（system 表示系统自动）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_control` (`device_id`, `control_type`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感联动控制设备表';

# 表 7: 环境小时报表（environment_report_hourly）

### 表说明
按设备和整点小时聚合的环境报表：平均温度、平均湿度、平均粉尘、样本数。用于环境报告页与历史趋势分析，比逐条原始数据轻量。查询"每小时温湿度均值""环境报告""近一天环境趋势"用此表。默认设备编号 `device-001`。

### DDL语句
## 环境小时报表 environment_report_hourly

CREATE TABLE `environment_report_hourly` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `hour_time` DATETIME NOT NULL COMMENT '统计整点时间（如 2026-07-12 20:00:00）',
  `avg_temperature` FLOAT DEFAULT NULL COMMENT '该小时平均温度（℃）',
  `avg_humidity` FLOAT DEFAULT NULL COMMENT '该小时平均湿度（%RH）',
  `avg_dust` FLOAT DEFAULT NULL COMMENT '该小时平均粉尘浓度（ppm）',
  `sample_count` INT NOT NULL DEFAULT 0 COMMENT '该小时样本数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_hour` (`device_id`, `hour_time`),
  KEY `idx_hour_time` (`hour_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环境小时报表';

# 表 8: AI 视觉复核记录表（vision_check）

### 表说明
告警触发后调用 AI 视觉模型对摄像头截图进行复核的记录，与告警一对一（`alarm_id` 唯一）。含 AI 结果、置信度、人工确认信息。查询"那次告警的视觉复核""AI 复核结果""复核截图"用此表。

### DDL语句
## AI视觉复核记录表 vision_check

CREATE TABLE `vision_check` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` VARCHAR(64) NOT NULL COMMENT '关联告警编号（唯一，对应 alarm_record.alarm_id）',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `image_url` VARCHAR(512) NOT NULL COMMENT '摄像头截图URL',
  `ai_result` VARCHAR(64) DEFAULT NULL COMMENT 'AI识别结果',
  `confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '置信度（0.00-1.00）',
  `confirmed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否人工确认：1-是, 0-否',
  `confirmed_by` VARCHAR(64) DEFAULT NULL COMMENT '确认人',
  `confirmed_at` DATETIME DEFAULT NULL COMMENT '确认时间',
  `checked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复核时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_id` (`alarm_id`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI视觉复核记录表';

# 表 9: 系统设置表（system_setting）

### 表说明
全局配置，KV 形式存储。分组：`threshold`（阈值，如 warning_threshold=200、danger_threshold=400、temperature_warning_threshold=40、humidity_warning_threshold=85）、`general`（通用，如 heartbeat_timeout=10、system_name、data_retention_days）、`keys`（第三方密钥，**查询时不要展示 value**）。查询"告警阈值多少""心跳超时""系统配置"用此表。

### DDL语句
## 系统设置表 system_setting

CREATE TABLE `system_setting` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `setting_key` VARCHAR(128) NOT NULL COMMENT '配置项键名',
  `setting_value` VARCHAR(512) NOT NULL COMMENT '配置项值',
  `setting_group` VARCHAR(64) DEFAULT 'general' COMMENT '配置分组：threshold-阈值, general-通用, keys-密钥',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`setting_key`),
  KEY `idx_group` (`setting_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感系统设置表';

# 表 10: 用户表（sys_user）

### 表说明
系统登录账号与用户管理，含用户名、角色、手机、邮箱、位置、最后登录时间。查询"我的账号信息""管理员是谁""用户位置""最后登录时间""用户列表"用此表。默认用户编号 1（admin）。

### DDL语句
## 用户表 sys_user

CREATE TABLE `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '加密密码（只读查询不要返回此字段）',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `role` VARCHAR(32) NOT NULL DEFAULT 'viewer' COMMENT '角色：admin-管理员, viewer-只读用户',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `avatar_image` LONGTEXT COMMENT '头像 base64（JPEG data URI）',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '用户位置信息（如 重庆市两江新区）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用, 0-禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感用户表';

# 表 11: 宠物档案表（pet_profile）

### 表说明
保存用户的宠物基础资料，并通过设备编号关联笼舍环境监测数据。查询"我家鹦鹉信息""某宠物档案""某品种""我的鹦鹉列表"用此表。`pet_id` 为 UUID 字符串（如 `PET-b50a7834-70cb-427c-9278-74977e05d2e1`），**不要臆造** `PET-001`；若用户用名字提问，先 `SELECT pet_id FROM pet_profile WHERE name=?` 反查。默认用户编号 1（admin），其下默认鹦鹉"蹦二"（和尚鹦鹉）。

### DDL语句
## 宠物档案表 pet_profile

CREATE TABLE `pet_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '宠物业务编号（UUID 字符串，如 PET-b50a7834-...）',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '笼舍编号（自由字符串，如 cage-001）',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '关联 smoke_device.device_id（多为 device-001）',
  `name` VARCHAR(64) NOT NULL COMMENT '宠物名称',
  `species` VARCHAR(64) NOT NULL COMMENT '宠物品种（完整中文名，如 和尚鹦鹉/太阳锥尾鹦鹉/黑顶凯克/牡丹鹦鹉/吸蜜鹦鹉 等）',
  `birthday` DATE DEFAULT NULL COMMENT '出生日期',
  `sex` VARCHAR(16) NOT NULL DEFAULT 'unknown' COMMENT 'male/female/unknown',
  `weight_grams` DECIMAL(8,2) DEFAULT NULL COMMENT '当前体重（克）',
  `feather_color` VARCHAR(64) DEFAULT NULL COMMENT '羽毛颜色',
  `sterilized` TINYINT DEFAULT 0 COMMENT '是否绝育：1-是, 0-否或未知',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像资源URL',
  `current_status` VARCHAR(32) DEFAULT NULL COMMENT '当前状态（如 站立、吃东西、睡觉）',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_id` (`pet_id`),
  KEY `idx_user_enabled` (`user_id`, `enabled`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_cage_id` (`cage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Pet profile table';

# 表 12: 宠物体重记录表（pet_weight_record）

### 表说明
宠物历次称重记录，用于体重趋势。查询"蹦二的体重记录""最近体重""体重变化趋势"用此表。关联 `pet_profile.pet_id`。

### DDL语句
## 宠物体重记录表 pet_weight_record

CREATE TABLE `pet_weight_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '关联 pet_profile.pet_id',
  `weight_grams` DECIMAL(8,2) NOT NULL COMMENT '体重，单位克',
  `measured_at` DATETIME NOT NULL COMMENT '测量时间',
  `source` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT 'manual/device/import',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pet_measured_at` (`pet_id`, `measured_at`),
  KEY `idx_measured_at` (`measured_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Pet weight history table';

# 表 13: 宠物媒体记录表（pet_media_record）

### 表说明
照片、截图、录音、视频等媒体记录，含标题、标签、时长、拍摄时间。截图类可内嵌 base64（`image_data`）。查询"蹦二的照片""录音""监控截图""成长相册"用此表。媒体类型字段值：photo/screenshot/recording/video（当前库实际数据以 recording、screenshot 为主）。

### DDL语句
## 宠物媒体记录表 pet_media_record

CREATE TABLE `pet_media_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `media_id` VARCHAR(64) NOT NULL COMMENT '媒体业务编号',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '关联 pet_profile.pet_id',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '笼舍编号（自由字符串）',
  `media_type` VARCHAR(32) NOT NULL COMMENT '媒体类型：photo-照片, screenshot-截图, recording-录音, video-视频',
  `title` VARCHAR(128) DEFAULT NULL COMMENT '标题',
  `file_url` VARCHAR(512) DEFAULT NULL COMMENT '文件资源URL（截图可空，改用 image_data）',
  `image_data` LONGTEXT COMMENT '截图/照片 base64（JPEG）；file_url 为空时使用',
  `thumbnail_url` VARCHAR(512) DEFAULT NULL COMMENT '缩略图URL',
  `duration_seconds` INT DEFAULT NULL COMMENT '录音/视频时长（秒）',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT '标签（如 睡觉, 吃饭, 兴奋）',
  `captured_at` DATETIME NOT NULL COMMENT '拍摄/录制时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_id` (`media_id`),
  KEY `idx_pet_type_time` (`pet_id`, `media_type`, `captured_at`),
  KEY `idx_captured_at` (`captured_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物媒体记录表';

# 表 14: 宠物病历记录表（pet_medical_record）

### 表说明
宠物就诊、用药、症状、复查记录，含医院名称电话。查询"蹦二的病历""就诊记录""用药""复查事项"用此表。记录类型：symptom/diagnosis/medication/recheck/other。

### DDL语句
## 宠物病历记录表 pet_medical_record

CREATE TABLE `pet_medical_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` VARCHAR(64) NOT NULL COMMENT '病历业务编号',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '关联 pet_profile.pet_id',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `record_type` VARCHAR(32) NOT NULL DEFAULT 'symptom' COMMENT '记录类型：symptom-症状, diagnosis-诊断, medication-用药, recheck-复查, other-其他',
  `title` VARCHAR(128) DEFAULT NULL COMMENT '标题摘要',
  `content` TEXT NOT NULL COMMENT '详细内容',
  `hospital_name` VARCHAR(128) DEFAULT NULL COMMENT '医院名称',
  `hospital_phone` VARCHAR(32) DEFAULT NULL COMMENT '医院电话',
  `attachments` JSON DEFAULT NULL COMMENT '附件列表（如检查单照片URL数组）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_id` (`record_id`),
  KEY `idx_pet_date` (`pet_id`, `record_date`),
  KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物病历记录表';

# 表 15: 宠物记账记录表（pet_ledger_record）

### 表说明
饲养支出记录，按宠物/用户/日期/标签分类。查询"养鹦鹉花了多少""医疗支出""本月开销""按标签汇总花费"用此表。⚠️ 真实消费标签 `category` 取值为：`食物`/`饲料`/`清洁`/`医疗`/`日常用品`/`玩具`/`其他`（列默认值 `其他`），**不是** `主粮/零食/用品`。关联 `pet_profile.pet_id` 与 `sys_user.id(user_id)`。

### DDL语句
## 宠物记账记录表 pet_ledger_record

CREATE TABLE `pet_ledger_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` VARCHAR(64) NOT NULL COMMENT '账单业务编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '关联 pet_profile.pet_id',
  `expense_date` DATE NOT NULL COMMENT '消费日期',
  `category` VARCHAR(64) NOT NULL DEFAULT '其他' COMMENT '消费标签：食物/饲料/清洁/医疗/日常用品/玩具/其他',
  `description` VARCHAR(255) NOT NULL COMMENT '消费描述',
  `amount` DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '支出金额',
  `currency` CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT 'ISO 4217币种代码',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ledger_id` (`ledger_id`),
  KEY `idx_pet_expense_date` (`pet_id`, `expense_date`),
  KEY `idx_user_expense_date` (`user_id`, `expense_date`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物饲养记账记录表';

# 表 16: 鹦鹉行为识别记录表（parrot_behavior_record）

### 表说明
AI 行为与种类识别历史，每次识别落一条，含行为标签、种类标签与置信度。查询"今天鹦鹉在干嘛""行为统计""种类识别结果"用此表。⚠️ 本表既有 `device_id` 也有 `pet_id`（所属鹦鹉，旧记录可能为 NULL）；按鹦鹉个体查询用 `pet_id`，按设备查询用 `device_id`。行为标签取值：进食/饮水/梳理羽毛/飞翔/攀爬/睡觉/鸣叫/排泄/玩耍/跳跃/站立观察/啃咬磨嘴/坐着/未知/无/错误识别。种类标签取值：虎皮鹦鹉/玄凤鹦鹉/牡丹鹦鹉/绿颊锥尾鹦鹉/太阳锥尾鹦鹉/和尚鹦鹉/凯克鹦鹉/黑顶凯克/吸蜜鹦鹉/折衷鹦鹉/金刚鹦鹉/葵花鹦鹉/亚马逊鹦鹉/环颈鹦鹉/红领绿鹦鹉/绿鹦鹉（异常情况下可能出现 错误识别/未识别到鹦鹉/未检测到鹦鹉/未识别/未知）。

### DDL语句
## 鹦鹉行为识别记录表 parrot_behavior_record

CREATE TABLE `parrot_behavior_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `pet_id` VARCHAR(64) DEFAULT NULL COMMENT '所属鹦鹉业务ID（关联 pet_profile.pet_id；旧版未归属记录可为空）',
  `image_url` VARCHAR(512) NOT NULL COMMENT '截图路径/URL',
  `parrot_detected` TINYINT NOT NULL DEFAULT 0 COMMENT '是否检测到鹦鹉：1-是, 0-否',
  `parrot_confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '鹦鹉检测置信度（0.00-1.00）',
  `behavior` VARCHAR(64) DEFAULT NULL COMMENT '行为标签',
  `behavior_confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '行为置信度（0.00-1.00）',
  `checked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  `species` VARCHAR(64) DEFAULT NULL COMMENT '种类标签',
  `species_confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '种类置信度（0.00-1.00）',
  PRIMARY KEY (`id`),
  KEY `idx_device_checked` (`device_id`, `checked_at`),
  KEY `idx_pet_checked` (`pet_id`, `checked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鹦鹉行为识别记录表';

# 表 17: 用户偏好表（user_preference）

### 表说明
用户级偏好配置（KV 形式），与全局 `system_setting` 区分。包含显示偏好（theme/font_size/language）、环境舒适区间（environment_temperature_lower/upper、environment_humidity_lower/upper、environment_dust_lower/upper）、资料偏好（avatar_parrot_id、pet_avatar_media_map）、QQ 绑定（bound_qq）等。分组：display/environment/general/profile/qq_bot/theme。查询"我的主题""我的温湿度舒适区间""绑定QQ""偏好设置"用此表。默认用户编号 1。

### DDL语句
## 用户偏好表 user_preference

CREATE TABLE `user_preference` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  `pref_key` VARCHAR(128) NOT NULL COMMENT '偏好项键名',
  `pref_value` VARCHAR(512) NOT NULL COMMENT '偏好项值',
  `pref_group` VARCHAR(64) DEFAULT 'general' COMMENT '偏好分组：display/environment/general/profile/qq_bot/theme',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pref_key` (`user_id`, `pref_key`),
  KEY `idx_user_group` (`user_id`, `pref_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好表';


# 用户问题：
{{开始.question}}


# 回答要求：
- 生成的SQL语句必须符合 MySQL 数据库的语法规范。
- 不要使用 Markdown 和 SQL 语法格式输出，禁止添加语法标准、备注、说明等信息。
- 直接输出符合 MySQL 标准的 SQL 语句，用 txt 纯文本格式展示即可。
- 当前数据库中的时间类字段（record_time、triggered_at、hour_time、measured_at、captured_at、checked_at、created_at、last_heartbeat 等）均为 DATETIME 类型，可直接用时间函数比较，例如：`WHERE triggered_at >= CURDATE()`、`WHERE record_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)`、`WHERE hour_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)`。
- 仅允许 SELECT 查询，禁止生成 INSERT、UPDATE、DELETE、DROP、ALTER 等写操作和 DDL 语句。
- 查询"最新""当前"数据时使用 `ORDER BY record_time DESC LIMIT 1` 或 `ORDER BY triggered_at DESC LIMIT N`。
- 查询"今日"数据使用 `>= CURDATE()`；查询"最近 N 小时"使用 `>= DATE_SUB(NOW(), INTERVAL N HOUR)`；查询"本月"使用 `>= DATE_FORMAT(NOW(), '%Y-%m-01')`。
- **设备编号**：环境/烟雾/温湿度历史数据（smoke_data/temperature_data/humidity_data/environment_report_hourly）默认用 `device-001`；联动控制与告警设备相关（device_control）默认用 `SMK-001`。两套 device_id 在库中并存，按所查表选择。
- **联动设备类型** `control_type` 字段值为 `buzzer`/`alarm_light`/`switch`（总开关），无 fan/air_purifier。
- **风险等级**字段值为 normal/low/medium/high；**告警状态**字段值为 pending/processing/resolved。
- **告警类型** `alarm_type` 字段值为 smoke（烟雾超限，最常见）/temperature_low/temperature_high/humidity_low/humidity_high/device_offline。
- **宠物编号** `pet_id` 为 UUID 字符串（如 `PET-b50a7834-...`），不要臆造 `PET-001`；用户用名字提问时用 `pet_profile.name` 反查或直接 JOIN。
- **默认宠物名**用"蹦二"（user_id=1 admin 的鹦鹉，品种和尚鹦鹉）；**默认用户编号**用 1（admin）；**笼舍编号**为自由字符串（如 cage-001），无独立笼舍表，不要查询 pet_cage。
- **关联关系**：宠物→用户用 `pet_profile.user_id = sys_user.id`；宠物→体重/媒体/病历/行为用 `pet_profile.pet_id` 关联 `pet_weight_record.pet_id` / `pet_media_record.pet_id` / `pet_medical_record.pet_id` / `parrot_behavior_record.pet_id`；宠物→记账用 `pet_profile.pet_id = pet_ledger_record.pet_id`（同时 `pet_profile.user_id = pet_ledger_record.user_id`）；用户→偏好用 `sys_user.id = user_preference.user_id`；用户→设备通过 `pet_profile.device_id`（`pet_profile.user_id`→`device_id`）；告警→视觉复核用 `alarm_record.alarm_id = vision_check.alarm_id`；环境报表→设备用 `environment_report_hourly.device_id`。⚠️ `alarm_record` **无 user_id 列**，查某用户告警需先取该用户在 `pet_profile` 的 `device_id` 再关联 `alarm_record.device_id`。
- **媒体类型**字段值为 photo/screenshot/recording/video；**病历记录类型**为 symptom/diagnosis/medication/recheck/other；**记账标签**为 食物/饲料/清洁/医疗/日常用品/玩具/其他；**行为标签**为 进食/饮水/梳理羽毛/飞翔/攀爬/睡觉/鸣叫/排泄/玩耍/跳跃/站立观察/啃咬磨嘴/坐着/未知/无/错误识别；**用户偏好分组**为 display/environment/general/profile/qq_bot/theme；**系统设置分组**为 threshold/general/keys（keys 组含密钥，查询时不要返回 setting_value）。
- 涉及密码（sys_user.password）、密钥（system_setting 中 setting_group='keys'）、头像 base64（sys_user.avatar_image、pet_media_record.image_data）等敏感大字段时，SELECT 字段列表中不要包含这些列。


# 示例：

- 示例1：自然语言描述："当前烟雾浓度是多少"
SELECT device_id, smoke_value, risk_level, record_time FROM smoke_data ORDER BY record_time DESC LIMIT 1


- 示例2：自然语言描述："今天有哪些告警"
SELECT alarm_id, device_id, alarm_type, smoke_value, risk_level, status, triggered_at FROM alarm_record WHERE triggered_at >= CURDATE() ORDER BY triggered_at DESC


- 示例3：自然语言描述："设备是否在线"
SELECT device_id, name, online, current_smoke_value, current_risk_level, current_alarm_status, last_heartbeat FROM smoke_device


- 示例4：自然语言描述："SMK-001 的蜂鸣器和报警灯开了吗"
SELECT control_type, control_name, status, last_operated_at, last_operated_by FROM device_control WHERE device_id = 'SMK-001'


- 示例5：自然语言描述："当前温度和湿度各是多少"
SELECT (SELECT temperature_value FROM temperature_data ORDER BY record_time DESC LIMIT 1) AS temperature, (SELECT humidity_value FROM humidity_data ORDER BY record_time DESC LIMIT 1) AS humidity


- 示例6：自然语言描述："蹦二的宠物档案信息"
SELECT pet_id, name, species, birthday, sex, weight_grams, feather_color, sterilized, current_status, device_id FROM pet_profile WHERE name = '蹦二'


- 示例7：自然语言描述："过去24小时烟雾浓度变化趋势"
SELECT smoke_value, risk_level, record_time FROM smoke_data WHERE device_id = 'device-001' AND record_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) ORDER BY record_time ASC


- 示例8：自然语言描述："今天各风险等级告警分别有多少条"
SELECT risk_level, COUNT(*) AS num FROM alarm_record WHERE triggered_at >= CURDATE() GROUP BY risk_level


- 示例9：自然语言描述："系统告警阈值是多少"
SELECT setting_key, setting_value, description FROM system_setting WHERE setting_group = 'threshold'


- 示例10：自然语言描述："device-001 最近5条告警记录"
SELECT alarm_id, alarm_type, smoke_value, risk_level, status, triggered_at, resolved_at FROM alarm_record WHERE device_id = 'device-001' ORDER BY triggered_at DESC LIMIT 5


- 示例11：自然语言描述："当前有多少条未处理的告警"
SELECT COUNT(*) AS pending_count FROM alarm_record WHERE status IN ('pending', 'processing')


- 示例12：自然语言描述："查询所有设备及其最新一条烟雾浓度"
SELECT d.device_id, d.name, d.online, d.current_smoke_value, d.current_risk_level, s.record_time FROM smoke_device d LEFT JOIN (SELECT device_id, MAX(record_time) AS record_time FROM smoke_data GROUP BY device_id) t ON d.device_id = t.device_id LEFT JOIN smoke_data s ON s.device_id = t.device_id AND s.record_time = t.record_time


- 示例13：自然语言描述："蹦二最近5次体重记录"
SELECT w.weight_grams, w.measured_at, w.source FROM pet_weight_record w JOIN pet_profile p ON w.pet_id = p.pet_id WHERE p.name = '蹦二' ORDER BY w.measured_at DESC LIMIT 5


- 示例14：自然语言描述："蹦二本月养它花了多少钱，按标签汇总"
SELECT l.category, SUM(l.amount) AS total, COUNT(*) AS cnt FROM pet_ledger_record l JOIN pet_profile p ON l.pet_id = p.pet_id WHERE p.name = '蹦二' AND l.expense_date >= DATE_FORMAT(NOW(), '%Y-%m-01') GROUP BY l.category ORDER BY total DESC


- 示例15：自然语言描述："蹦二的病历记录"
SELECT m.record_date, m.record_type, m.title, m.hospital_name, m.hospital_phone FROM pet_medical_record m JOIN pet_profile p ON m.pet_id = p.pet_id WHERE p.name = '蹦二' ORDER BY m.record_date DESC


- 示例16：自然语言描述："今天识别到鹦鹉的行为统计"
SELECT behavior, COUNT(*) AS cnt FROM parrot_behavior_record WHERE checked_at >= CURDATE() AND parrot_detected = 1 GROUP BY behavior ORDER BY cnt DESC


- 示例17：自然语言描述："蹦二今天的行为识别记录"
SELECT b.behavior, b.species, b.parrot_confidence, b.behavior_confidence, b.checked_at FROM parrot_behavior_record b JOIN pet_profile p ON b.pet_id = p.pet_id WHERE p.name = '蹦二' AND b.checked_at >= CURDATE() ORDER BY b.checked_at DESC


- 示例18：自然语言描述："最近24小时环境小时报表"
SELECT hour_time, avg_temperature, avg_humidity, avg_dust, sample_count FROM environment_report_hourly WHERE device_id = 'device-001' AND hour_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) ORDER BY hour_time ASC


- 示例19：自然语言描述："我的温湿度舒适区间设置"
SELECT pref_key, pref_value FROM user_preference WHERE user_id = 1 AND pref_group = 'environment' ORDER BY pref_key


- 示例20：自然语言描述："我的偏好设置有哪些"
SELECT pref_key, pref_value, pref_group FROM user_preference WHERE user_id = 1 ORDER BY pref_group, pref_key


- 示例21：自然语言描述："查询某次告警的AI视觉复核结果"
SELECT v.alarm_id, v.ai_result, v.confidence, v.confirmed, v.confirmed_by, v.checked_at FROM vision_check v WHERE v.alarm_id = '9aa52612-a010-48a1-9a78-0465d6ead11c'


- 示例22：自然语言描述："我的鹦鹉列表及绑定的设备"
SELECT p.pet_id, p.name, p.species, p.device_id, p.cage_id, d.online, d.current_smoke_value, d.current_risk_level FROM pet_profile p LEFT JOIN smoke_device d ON p.device_id = d.device_id WHERE p.user_id = 1 AND p.enabled = 1


- 示例23：自然语言描述："admin 用户的设备上最近有哪些告警"
SELECT a.alarm_id, a.device_id, a.alarm_type, a.risk_level, a.status, a.triggered_at FROM alarm_record a WHERE a.device_id IN (SELECT DISTINCT device_id FROM pet_profile WHERE user_id = 1 AND device_id IS NOT NULL) ORDER BY a.triggered_at DESC LIMIT 10


- 示例24：自然语言描述："各记账标签总支出排行"
SELECT category, SUM(amount) AS total, COUNT(*) AS cnt FROM pet_ledger_record GROUP BY category ORDER BY total DESC
