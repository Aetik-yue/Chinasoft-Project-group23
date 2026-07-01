-- =====================================================================
-- 智慧烟感预警系统 - 数据库建表脚本
-- 版本: v1.0  日期: 2026-06-30
-- 说明: 7 张业务表 + 初始数据，对应《智慧烟感API接口文档.md》
-- 执行方式: 在已装好MySQL的环境，用任意客户端(Navicat/DataGrip/Workbench)
--          或命令行 mysql -u root -p < 本文件  执行即可。
-- =====================================================================

-- 1. 建库（若已建库可删掉这行）
CREATE DATABASE IF NOT EXISTS smoke_sensor
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE smoke_sensor;

-- 2. 建表 ----------------------------------------------------------------

-- 2.1 用户表（登录用）
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
  password    VARCHAR(128) NOT NULL        COMMENT '密码，建议存bcrypt哈希值',
  role        VARCHAR(16)  NOT NULL DEFAULT 'viewer' COMMENT '角色: admin/viewer',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- 2.2 烟感设备表
CREATE TABLE IF NOT EXISTS device (
  device_id       VARCHAR(32)  PRIMARY KEY              COMMENT '设备编号, 如 SMK-001',
  name            VARCHAR(64)  NOT NULL                 COMMENT '设备名称',
  location        VARCHAR(128) DEFAULT NULL             COMMENT '安装位置',
  online          TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否在线 0/1',
  enabled         TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否启用 0/1',
  last_heartbeat  DATETIME     DEFAULT NULL             COMMENT '最近心跳时间',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烟感设备';

-- 2.3 烟雾浓度记录表（最新数据 + 历史数据都存这张，按时间区分）
CREATE TABLE IF NOT EXISTS smoke_reading (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  device_id     VARCHAR(32)  NOT NULL                  COMMENT '设备编号',
  smoke_value   INT          NOT NULL                  COMMENT '烟雾浓度(ppm)',
  unit          VARCHAR(8)   NOT NULL DEFAULT 'ppm',
  risk_level    VARCHAR(16)  NOT NULL                  COMMENT 'normal/low/medium/high',
  risk_score    INT          NOT NULL DEFAULT 0         COMMENT '风险分值0-100',
  alarm_status  VARCHAR(16)  NOT NULL DEFAULT 'safe'   COMMENT 'safe/alarm/offline',
  alarm_type    VARCHAR(32)  DEFAULT NULL              COMMENT 'smoke_high/device_offline',
  recorded_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  KEY idx_device_time (device_id, recorded_at),
  KEY idx_time (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烟雾浓度记录(最新+历史)';

-- 2.4 告警记录表
CREATE TABLE IF NOT EXISTS alarm (
  alarm_id    VARCHAR(48)  PRIMARY KEY                  COMMENT '告警ID, 如 ALM-20260630-001',
  device_id   VARCHAR(32)  NOT NULL,
  alarm_type  VARCHAR(32)  NOT NULL                     COMMENT 'smoke_high/device_offline',
  smoke_value INT          DEFAULT NULL                 COMMENT '触发时浓度(ppm)',
  level       VARCHAR(16)  NOT NULL                    COMMENT 'normal/low/medium/high',
  status      VARCHAR(16)  NOT NULL DEFAULT 'pending'   COMMENT 'pending/processing/resolved',
  handler     VARCHAR(64)  DEFAULT NULL                 COMMENT '处理人',
  remark      VARCHAR(255) DEFAULT NULL                 COMMENT '处理备注',
  handled_at  DATETIME     DEFAULT NULL                 COMMENT '处理时间',
  alarm_time  DATETIME     NOT NULL                     COMMENT '告警触发时间',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_device (device_id),
  KEY idx_status (status),
  KEY idx_alarm_time (alarm_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录';

-- 2.5 告警处理时间线
CREATE TABLE IF NOT EXISTS alarm_timeline (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  alarm_id    VARCHAR(48)  NOT NULL                     COMMENT '关联告警ID',
  event       VARCHAR(128) NOT NULL                     COMMENT '事件描述',
  event_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_alarm (alarm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警处理时间线';

-- 2.6 设备控制开关状态表（蜂鸣器/报警灯/排风扇）
CREATE TABLE IF NOT EXISTS device_control (
  id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
  device_id    VARCHAR(32) NOT NULL                     COMMENT '设备编号',
  device_type  VARCHAR(32) NOT NULL                     COMMENT 'buzzer/alarm_light/fan',
  status       VARCHAR(8)  NOT NULL DEFAULT 'off'        COMMENT 'on/off',
  updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dev_type (device_id, device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备控制开关状态';

-- 2.7 系统设置表（阈值等，key-value结构便于扩展）
CREATE TABLE IF NOT EXISTS sys_setting (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  setting_key   VARCHAR(64)  NOT NULL UNIQUE            COMMENT '配置键',
  setting_value VARCHAR(255) NOT NULL                    COMMENT '配置值',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置';

-- 3. 初始数据 -----------------------------------------------------------

-- 3.1 默认管理员账号（密码123456的bcrypt示例值，生产请重新生成）
INSERT INTO sys_user (username, password, role) VALUES
  ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin');

-- 3.2 默认主设备
INSERT INTO device (device_id, name, location, online, enabled) VALUES
  ('SMK-001', '1号仓库烟感', 'A区1号仓库', 1, 1);

-- 3.3 主设备的三个受控设备初始关闭
INSERT INTO device_control (device_id, device_type, status) VALUES
  ('SMK-001', 'buzzer', 'off'),
  ('SMK-001', 'alarm_light', 'off'),
  ('SMK-001', 'fan', 'off');

-- 3.4 默认阈值配置（对应待定参数表: 中风险200 / 高危400）
INSERT INTO sys_setting (setting_key, setting_value) VALUES
  ('warning_threshold', '200'),
  ('danger_threshold', '400'),
  ('unit', 'ppm');

-- 3.5 一条示例安全浓度记录（让首页一进来就有数据）
INSERT INTO smoke_reading (device_id, smoke_value, unit, risk_level, risk_score, alarm_status, alarm_type, recorded_at)
VALUES ('SMK-001', 86, 'ppm', 'low', 86, 'safe', NULL, NOW());

-- =====================================================================
-- 执行完毕。验证:
--   SHOW TABLES;
--   SELECT * FROM device;
--   SELECT * FROM sys_setting;
-- =====================================================================
