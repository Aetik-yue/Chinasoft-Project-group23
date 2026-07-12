# 数据表结构

> 本提示词用于 MaxKB 数据库工具（Text-to-SQL），让智能体根据用户自然语言问题生成 MySQL 查询语句，查询「智慧宠物烟感安全系统」的实时数据。
> ⚠️ **当前覆盖范围**：本提示词覆盖 18 张表，分两类。① 烟感安全监测类（8 张）：smoke_device / smoke_data / temperature_data / humidity_data / alarm_record / device_control / pet_profile / system_setting。② 鹦鹉照护类（10 张）：sys_user / pet_cage / pet_weight_record / pet_daily_report / pet_media_record / pet_medical_record / pet_ledger_record / parrot_behavior_record / food_safety_query / alarm_timeline。覆盖账号、笼舍、体重、成长日报、媒体、病历、记账、行为识别、食物安全、告警时间线等宠物相关问题。
> ⚠️ 使用前请对照实际数据库表结构：如某张表或字段尚未建成，请从本提示词中删除对应 DDL，否则 LLM 生成的 SQL 会查询失败。
> ⚠️ 本工具仅允许 SELECT 只读查询，禁止 INSERT/UPDATE/DELETE/DDL。

# 表 1: 烟感设备表（smoke_device）

### 表说明
保存烟感设备的基本信息和最新状态：设备编号、安装位置、在线状态、最后心跳、当前烟雾/粉尘浓度、当前风险等级和当前告警状态。查询"设备是否在线""当前浓度多少"用此表。

### DDL语句
## 烟感设备表 smoke_device

CREATE TABLE `smoke_device` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号（业务唯一，如 SMK-001）',
  `name` VARCHAR(128) NOT NULL COMMENT '设备名称（如 笼舍A01烟感）',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '关联笼舍编号',
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
  UNIQUE KEY `uk_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烟感设备表';

# 表 2: 烟雾/粉尘历史数据表（smoke_data）

### 表说明
保存每一次烟雾/粉尘浓度采集记录，数据量最大。用于历史趋势图、最新数据追溯和告警判断依据。查询"历史浓度""最近浓度"用此表。

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
  KEY `idx_device_time` (`device_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烟雾/粉尘历史数据表';

# 表 3: 温度历史数据表（temperature_data）

### 表说明
保存每一次温度采集记录，用于实时监控页温度展示和历史趋势分析。查询"当前温度""温度历史"用此表。

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
  KEY `idx_device_time` (`device_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温度历史数据表';

# 表 4: 湿度历史数据表（humidity_data）

### 表说明
保存每一次湿度采集记录，用于实时监控页湿度展示和历史趋势分析。查询"当前湿度""湿度历史"用此表。

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
  KEY `idx_device_time` (`device_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='湿度历史数据表';

# 表 5: 告警记录表（alarm_record）

### 表说明
告警事件主表。当烟雾/粉尘值达到告警阈值或设备离线时生成记录。查询"今天有哪些告警""最近告警""未处理告警"用此表。

### DDL语句
## 告警记录表 alarm_record

CREATE TABLE `alarm_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` VARCHAR(64) NOT NULL COMMENT '告警编号（业务唯一，如 ALM-20260705-001）',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '关联笼舍编号',
  `alarm_type` VARCHAR(64) NOT NULL COMMENT '告警类型：smoke_high-烟雾/粉尘超限, temp_high-温度过高, temp_low-温度过低, humidity_high-湿度过高, humidity_low-湿度过低, device_offline-设备离线',
  `smoke_value` INT DEFAULT NULL COMMENT '触发时浓度（ppm，非烟雾告警为null）',
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_id` (`alarm_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_triggered_at` (`triggered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

# 表 6: 联动控制设备表（device_control）

### 表说明
蜂鸣器、报警灯、排风扇、空气净化器等联动控制设备的状态。查询"排风扇开了吗""蜂鸣器状态""联动设备开关"用此表。

### DDL语句
## 联动控制设备表 device_control

CREATE TABLE `device_control` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '所属烟感设备编号',
  `control_type` VARCHAR(64) NOT NULL COMMENT '控制设备类型：buzzer-蜂鸣器, alarm_light-报警灯, fan-排风扇, air_purifier-空气净化器',
  `control_name` VARCHAR(128) DEFAULT NULL COMMENT '设备显示名称（如 蜂鸣器）',
  `status` VARCHAR(32) NOT NULL DEFAULT 'off' COMMENT '当前状态：on-开启, off-关闭',
  `auto_linkage` TINYINT NOT NULL DEFAULT 1 COMMENT '是否参与自动联动：1-是, 0-否',
  `last_operated_at` DATETIME DEFAULT NULL COMMENT '最后操作时间',
  `last_operated_by` VARCHAR(64) DEFAULT NULL COMMENT '最后操作人（system 表示系统自动）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_control` (`device_id`, `control_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联动控制设备表';

# 表 7: 宠物档案表（pet_profile）

### 表说明
保存用户的宠物基础资料，并通过设备编号关联笼舍环境监测数据。查询"我家鹦鹉信息""某宠物档案""某品种"用此表。

### DDL语句
## 宠物档案表 pet_profile

CREATE TABLE `pet_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '宠物业务编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '关联笼舍编号',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '关联 smoke_device.device_id',
  `name` VARCHAR(64) NOT NULL COMMENT '宠物名称',
  `species` VARCHAR(64) NOT NULL COMMENT '宠物品种（如 小太阳、虎皮、玄凤）',
  `birthday` DATE DEFAULT NULL COMMENT '出生日期',
  `sex` VARCHAR(16) NOT NULL DEFAULT 'unknown' COMMENT 'male/female/unknown',
  `weight_grams` DECIMAL(8,2) DEFAULT NULL COMMENT '当前体重（克）',
  `feather_color` VARCHAR(64) DEFAULT NULL COMMENT '羽毛颜色',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像资源URL',
  `current_status` VARCHAR(32) DEFAULT NULL COMMENT '当前状态（如 站立、吃东西、睡觉）',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_id` (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';

# 表 8: 系统设置表（system_setting）

### 表说明
全局配置，KV 形式存储。包含风险阈值、心跳超时、温湿度舒适区间、粉尘阈值等。查询"告警阈值多少""心跳超时""系统配置"用此表。

### DDL语句
## 系统设置表 system_setting

CREATE TABLE `system_setting` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `setting_key` VARCHAR(128) NOT NULL COMMENT '配置项键名',
  `setting_value` VARCHAR(512) NOT NULL COMMENT '配置项值',
  `setting_group` VARCHAR(64) DEFAULT 'general' COMMENT '配置分组：threshold-阈值, general-通用, mqtt-MQTT配置',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

# 表 9: 用户表（sys_user）

### 表说明
系统登录账号与用户管理，含用户名、角色、手机、邮箱、位置、最后登录时间。查询"我的账号信息""管理员是谁""用户位置""最后登录时间"用此表。

### DDL语句
## 用户表 sys_user

CREATE TABLE `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '加密密码',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `role` VARCHAR(32) NOT NULL DEFAULT 'viewer' COMMENT '角色：admin-管理员, viewer-只读用户',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '用户位置信息（如 上海市·浦东新区）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用, 0-禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

# 表 10: 宠物笼舍表（pet_cage）

### 表说明
笼舍/监测区域信息，归属用户并绑定监测设备。查询"我的笼舍""某笼舍绑的设备""笼舍位置""笼舍列表"用此表。

### DDL语句
## 宠物笼舍表 pet_cage

CREATE TABLE `pet_cage` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 sys_user.id',
  `cage_id` VARCHAR(64) NOT NULL COMMENT '笼舍业务编号（如 CAGE-A01）',
  `cage_name` VARCHAR(128) NOT NULL COMMENT '笼舍名称（如 笼舍 A-01）',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '安装位置（如 客厅南侧）',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '逻辑关联 smoke_device.device_id',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用, 0-禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cage_id` (`cage_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物笼舍表';

# 表 11: 宠物体重记录表（pet_weight_record）

### 表说明
宠物历次称重记录，用于体重趋势与成长报告。查询"啾啾的体重记录""最近体重""体重变化趋势"用此表。

### DDL语句
## 宠物体重记录表 pet_weight_record

CREATE TABLE `pet_weight_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '逻辑关联 pet_profile.pet_id',
  `weight_grams` DECIMAL(8,2) NOT NULL COMMENT '体重，单位克',
  `measured_at` DATETIME NOT NULL COMMENT '测量时间',
  `source` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT 'manual/device/import',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pet_measured_at` (`pet_id`, `measured_at`),
  KEY `idx_measured_at` (`measured_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物体重历史表';

# 表 12: 宠物成长日报表（pet_daily_report）

### 表说明
按宠物和日期保存健康评分及行为汇总（睡眠、鸣叫、进食、排泄、环境均值）。周报月报由日报按日期范围聚合生成。查询"今天成长报告""健康评分""睡眠时长""鸣叫次数""进食次数"用此表。

### DDL语句
## 宠物成长日报表 pet_daily_report

CREATE TABLE `pet_daily_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '逻辑关联 pet_profile.pet_id',
  `report_date` DATE NOT NULL COMMENT '报告日期',
  `health_score` TINYINT UNSIGNED NOT NULL COMMENT '健康评分，业务范围0-100',
  `sleep_minutes` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '睡眠总分钟数',
  `vocalization_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '鸣叫次数',
  `feeding_count` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '进食次数',
  `excretion_count` SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排泄次数',
  `dust_avg_value` INT DEFAULT NULL COMMENT '当日平均粉尘浓度（ppm）',
  `temp_avg_value` FLOAT DEFAULT NULL COMMENT '当日平均温度（℃）',
  `humidity_avg_value` FLOAT DEFAULT NULL COMMENT '当日平均湿度（%RH）',
  `source` VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT 'system/manual/import',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '报告备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_report_date` (`pet_id`, `report_date`),
  KEY `idx_report_date` (`report_date`),
  KEY `idx_health_score` (`health_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物每日成长报告表';

# 表 13: 宠物媒体记录表（pet_media_record）

### 表说明
照片、截图、录音、视频等媒体记录，含标题、标签、时长、拍摄时间。查询"啾啾的照片""录音""监控截图""成长相册"用此表。

### DDL语句
## 宠物媒体记录表 pet_media_record

CREATE TABLE `pet_media_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `media_id` VARCHAR(64) NOT NULL COMMENT '媒体业务编号',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '逻辑关联 pet_profile.pet_id',
  `cage_id` VARCHAR(64) DEFAULT NULL COMMENT '逻辑关联 pet_cage.cage_id',
  `media_type` VARCHAR(32) NOT NULL COMMENT '媒体类型：photo-照片, screenshot-截图, recording-录音, video-视频',
  `title` VARCHAR(128) DEFAULT NULL COMMENT '标题（如 最兴奋照片）',
  `file_url` VARCHAR(512) DEFAULT NULL COMMENT '文件资源URL（截图可空，改用 image_data）',
  `image_data` LONGTEXT NULL COMMENT '截图/照片 base64（JPEG）；file_url 为空时使用',
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
宠物就诊、用药、症状、复查记录，含医院名称电话。查询"啾啾的病历""就诊记录""用药""复查事项"用此表。

### DDL语句
## 宠物病历记录表 pet_medical_record

CREATE TABLE `pet_medical_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` VARCHAR(64) NOT NULL COMMENT '病历业务编号',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '逻辑关联 pet_profile.pet_id',
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
饲养支出记录，按宠物/用户/日期/标签分类。查询"养鹦鹉花了多少""医疗支出""本月开销""按标签汇总花费"用此表。

### DDL语句
## 宠物记账记录表 pet_ledger_record

CREATE TABLE `pet_ledger_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` VARCHAR(64) NOT NULL COMMENT '账单业务编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 sys_user.id',
  `pet_id` VARCHAR(64) NOT NULL COMMENT '逻辑关联 pet_profile.pet_id',
  `expense_date` DATE NOT NULL COMMENT '消费日期',
  `category` VARCHAR(64) NOT NULL DEFAULT '其他' COMMENT '消费标签：主粮/医疗/用品/日常用品/玩具/零食',
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

# 表 16: 鹦鹉行为识别表（parrot_behavior_record）

### 表说明
AI 行为与种类识别历史，每次识别落一条，含行为标签、种类标签与置信度。查询"今天鹦鹉在干嘛""行为统计""种类识别结果"用此表。

### DDL语句
## 鹦鹉行为识别表 parrot_behavior_record

CREATE TABLE `parrot_behavior_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `image_url` VARCHAR(512) NOT NULL COMMENT '截图路径/URL',
  `parrot_detected` TINYINT NOT NULL DEFAULT 0 COMMENT '是否检测到鹦鹉：1-是, 0-否',
  `parrot_confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '鹦鹉检测置信度（0.00-1.00）',
  `behavior` VARCHAR(64) DEFAULT NULL COMMENT '行为标签：进食/饮水/梳理羽毛/飞翔/攀爬/睡觉',
  `behavior_confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '行为置信度（0.00-1.00）',
  `species` VARCHAR(64) DEFAULT NULL COMMENT '种类标签：虎皮/玄凤/牡丹/…',
  `species_confidence` DECIMAL(3,2) DEFAULT NULL COMMENT '种类置信度（0.00-1.00）',
  `checked_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_checked` (`device_id`, `checked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鹦鹉行为识别记录表';

# 表 17: 食物安全查询表（food_safety_query）

### 表说明
食物可食用性查询历史，记录食物名、种类、结果与建议。查询"苹果能喂吗""查过的食物""哪些食物 unsafe"用此表。

### DDL语句
## 食物安全查询表 food_safety_query

CREATE TABLE `food_safety_query` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `query_id` VARCHAR(64) NOT NULL COMMENT '查询业务编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 sys_user.id',
  `food_name` VARCHAR(128) NOT NULL COMMENT '食物名称',
  `food_category` VARCHAR(64) DEFAULT NULL COMMENT '食物种类：蔬菜/水果/肉类/昆虫/谷物',
  `result` VARCHAR(64) DEFAULT NULL COMMENT '查询结果：safe-可食用, limited-少量食用, unsafe-不建议, unknown-未知',
  `advice` VARCHAR(500) DEFAULT NULL COMMENT '建议说明',
  `queried_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查询时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_query_id` (`query_id`),
  KEY `idx_user_food` (`user_id`, `food_name`),
  KEY `idx_queried_at` (`queried_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物安全查询记录表';

# 表 18: 告警时间线表（alarm_timeline）

### 表说明
告警生命周期事件（触发/联动启动/联动结束/人工处理/自动恢复/视觉复核），与 alarm_record 一对多。查询"那次告警怎么处理的""告警处理过程""联动了哪些设备"用此表。

### DDL语句
## 告警时间线表 alarm_timeline

CREATE TABLE `alarm_timeline` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` VARCHAR(64) NOT NULL COMMENT '关联告警编号',
  `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型：trigger-告警触发, linkage_start-联动启动, linkage_end-联动结束, handle-人工处理, auto_resolve-自动恢复, vision_check-视觉复核',
  `event_desc` VARCHAR(255) NOT NULL COMMENT '事件描述',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人（系统触发为 system）',
  `extra_data` JSON DEFAULT NULL COMMENT '扩展信息（JSON格式，如 {"deviceType":"fan","status":"on"}）',
  `event_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_alarm_id` (`alarm_id`),
  KEY `idx_event_time` (`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警处理时间线表';


# 用户问题：
{{开始.question}}


# 回答要求：
- 生成的SQL语句必须符合 MySQL 数据库的语法规范。
- 不要使用 Markdown 和 SQL 语法格式输出，禁止添加语法标准、备注、说明等信息。
- 直接输出符合 MySQL 标准的 SQL 语句，用 txt 纯文本格式展示即可。
- 当前数据库中的时间类字段（record_time、triggered_at、created_at、last_heartbeat 等）均为 DATETIME 类型，可直接用时间函数比较，例如：`WHERE triggered_at >= CURDATE()`、`WHERE record_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)`。
- 仅允许 SELECT 查询，禁止生成 INSERT、UPDATE、DELETE、DROP、ALTER 等写操作和 DDL 语句。
- 查询"最新""当前"数据时使用 `ORDER BY record_time DESC LIMIT 1` 或 `ORDER BY triggered_at DESC LIMIT N`。
- 查询"今日"数据使用 `>= CURDATE()`；查询"最近 N 小时"使用 `>= DATE_SUB(NOW(), INTERVAL N HOUR)`。
- 涉及设备编号默认用 SMK-001；涉及联动设备类型字段值为 buzzer/alarm_light/fan/air_purifier；涉及风险等级字段值为 normal/low/medium/high；涉及告警状态字段值为 pending/processing/resolved。
- 涉及宠物编号默认用 PET-001、宠物名默认"啾啾"、笼舍编号默认 CAGE-A01、用户编号默认 1；宠物关联查询用 pet_profile.pet_id 关联 pet_weight_record / pet_daily_report / pet_media_record / pet_medical_record 的 pet_id，用 pet_profile.user_id 关联 sys_user.id 与 pet_ledger_record.user_id；用 pet_profile.name 关联用户用名字提问的查询。
- 涉及媒体类型字段值为 photo/screenshot/recording/video；病历记录类型为 symptom/diagnosis/medication/recheck/other；记账标签为主粮/医疗/用品/日常用品/玩具/零食；食物查询结果为 safe/limited/unsafe/unknown；行为标签为进食/饮水/梳理羽毛/飞翔/攀爬/睡觉；告警时间线事件类型为 trigger/linkage_start/linkage_end/handle/auto_resolve/vision_check。


# 示例：

- 示例1：自然语言描述："当前烟雾浓度是多少"
SELECT device_id, smoke_value, risk_level, record_time FROM smoke_data ORDER BY record_time DESC LIMIT 1


- 示例2：自然语言描述："今天有哪些告警"
SELECT alarm_id, device_id, alarm_type, smoke_value, risk_level, status, triggered_at FROM alarm_record WHERE triggered_at >= CURDATE() ORDER BY triggered_at DESC


- 示例3：自然语言描述："设备是否在线"
SELECT device_id, name, online, current_smoke_value, current_risk_level, current_alarm_status, last_heartbeat FROM smoke_device


- 示例4：自然语言描述："SMK-001 的排风扇和蜂鸣器开了吗"
SELECT control_type, control_name, status, last_operated_at, last_operated_by FROM device_control WHERE device_id = 'SMK-001'


- 示例5：自然语言描述："当前温度和湿度各是多少"
SELECT (SELECT temperature_value FROM temperature_data ORDER BY record_time DESC LIMIT 1) AS temperature, (SELECT humidity_value FROM humidity_data ORDER BY record_time DESC LIMIT 1) AS humidity


- 示例6：自然语言描述："啾啾的宠物档案信息"
SELECT pet_id, name, species, birthday, sex, weight_grams, current_status, device_id FROM pet_profile WHERE name = '啾啾'


- 示例7：自然语言描述："过去24小时烟雾浓度变化趋势"
SELECT smoke_value, risk_level, record_time FROM smoke_data WHERE record_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) ORDER BY record_time ASC


- 示例8：自然语言描述："今天各风险等级告警分别有多少条"
SELECT risk_level, COUNT(*) AS num FROM alarm_record WHERE triggered_at >= CURDATE() GROUP BY risk_level


- 示例9：自然语言描述："系统告警阈值是多少"
SELECT setting_key, setting_value, description FROM system_setting WHERE setting_group = 'threshold'


- 示例10：自然语言描述："SMK-001 最近5条告警记录"
SELECT alarm_id, alarm_type, smoke_value, risk_level, status, triggered_at FROM alarm_record WHERE device_id = 'SMK-001' ORDER BY triggered_at DESC LIMIT 5


- 示例11：自然语言描述："当前有多少条未处理的告警"
SELECT COUNT(*) AS pending_count FROM alarm_record WHERE status IN ('pending', 'processing')


- 示例12：自然语言描述："查询所有设备及其最新一条烟雾浓度"
SELECT d.device_id, d.name, d.online, d.current_smoke_value, d.current_risk_level, s.record_time FROM smoke_device d LEFT JOIN (SELECT device_id, MAX(record_time) AS record_time FROM smoke_data GROUP BY device_id) t ON d.device_id = t.device_id LEFT JOIN smoke_data s ON s.device_id = t.device_id AND s.record_time = t.record_time


- 示例13：自然语言描述："啾啾最近5次体重记录"
SELECT w.weight_grams, w.measured_at, w.source FROM pet_weight_record w JOIN pet_profile p ON w.pet_id = p.pet_id WHERE p.name = '啾啾' ORDER BY w.measured_at DESC LIMIT 5


- 示例14：自然语言描述："啾啾今天的成长报告"
SELECT report_date, health_score, sleep_minutes, vocalization_count, feeding_count, excretion_count, dust_avg_value, temp_avg_value, humidity_avg_value FROM pet_daily_report WHERE pet_id = 'PET-001' AND report_date = CURDATE()


- 示例15：自然语言描述："啾啾本月养它花了多少钱，按标签汇总"
SELECT category, SUM(amount) AS total, COUNT(*) AS cnt FROM pet_ledger_record WHERE pet_id = 'PET-001' AND expense_date >= DATE_FORMAT(NOW(), '%Y-%m-01') GROUP BY category ORDER BY total DESC


- 示例16：自然语言描述："啾啾的病历记录"
SELECT record_date, record_type, title, hospital_name, hospital_phone FROM pet_medical_record WHERE pet_id = 'PET-001' ORDER BY record_date DESC


- 示例17：自然语言描述："今天识别到鹦鹉的行为统计"
SELECT behavior, COUNT(*) AS cnt FROM parrot_behavior_record WHERE checked_at >= CURDATE() AND parrot_detected = 1 GROUP BY behavior ORDER BY cnt DESC


- 示例18：自然语言描述："苹果能喂鹦鹉吗"
SELECT food_name, food_category, result, advice, queried_at FROM food_safety_query WHERE food_name = '苹果' ORDER BY queried_at DESC LIMIT 1


- 示例19：自然语言描述："ALM-20260705-001 这次告警的处理过程"
SELECT event_type, event_desc, operator, event_time FROM alarm_timeline WHERE alarm_id = 'ALM-20260705-001' ORDER BY event_time ASC


- 示例20：自然语言描述："我的笼舍和绑定的设备"
SELECT c.cage_id, c.cage_name, c.location, d.device_id, d.online, d.current_smoke_value, d.current_risk_level FROM pet_cage c LEFT JOIN smoke_device d ON c.device_id = d.device_id WHERE c.user_id = 1 AND c.enabled = 1
