# 数据表结构

> 本提示词用于 MaxKB 数据库工具（Text-to-SQL），让智能体根据用户自然语言问题生成 MySQL 查询语句，查询「智慧宠物烟感安全系统」的实时数据。
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
