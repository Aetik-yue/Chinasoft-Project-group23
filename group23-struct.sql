-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 47.108.58.107    Database: dream28
-- ------------------------------------------------------
-- Server version	5.7.44-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alarm_record`
--

DROP TABLE IF EXISTS `alarm_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` varchar(64) NOT NULL COMMENT '告警编号（业务唯一，如 ALM-20260630-001）',
  `device_id` varchar(64) NOT NULL COMMENT '设备编号',
  `cage_id` varchar(64) DEFAULT NULL COMMENT '逻辑关联 pet_cage.cage_id',
  `alarm_type` varchar(64) NOT NULL COMMENT '告警类型：smoke_high/device_offline',
  `smoke_value` int(11) DEFAULT NULL COMMENT '触发时浓度（ppm，离线告警为NULL）',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级：low/medium/high',
  `status` varchar(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/resolved',
  `handler` varchar(64) DEFAULT NULL COMMENT '处理人用户名',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  `remark` varchar(1000) DEFAULT NULL COMMENT '处理备注',
  `triggered_at` datetime NOT NULL COMMENT '告警触发时间',
  `resolved_at` datetime DEFAULT NULL COMMENT '告警恢复时间',
  `is_simulated` tinyint(4) NOT NULL DEFAULT '0' COMMENT '1模拟触发，0真实触发',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `alarm_message` varchar(500) DEFAULT NULL,
  `alarm_value` double DEFAULT NULL,
  `create_time` datetime(6) NOT NULL,
  `threshold_value` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_id` (`alarm_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_triggered_at` (`triggered_at`),
  KEY `idx_alarm_type` (`alarm_type`),
  KEY `idx_composite` (`device_id`,`status`,`triggered_at`),
  KEY `idx_cage_id` (`cage_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感告警记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `alarm_timeline`
--

DROP TABLE IF EXISTS `alarm_timeline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alarm_timeline` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` varchar(64) NOT NULL COMMENT '关联智慧烟感告警编号',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_desc` varchar(255) NOT NULL COMMENT '事件描述',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `extra_data` json DEFAULT NULL COMMENT '扩展信息',
  `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alarm_id` (`alarm_id`),
  KEY `idx_event_time` (`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感告警处理时间线表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `device_control`
--

DROP TABLE IF EXISTS `device_control`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_control` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(64) NOT NULL COMMENT '所属烟感设备编号',
  `control_type` varchar(64) NOT NULL COMMENT 'buzzer/alarm_light/fan',
  `control_name` varchar(128) DEFAULT NULL COMMENT '控制设备名称',
  `status` varchar(32) NOT NULL DEFAULT 'off' COMMENT 'on/off',
  `auto_linkage` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否自动联动',
  `last_operated_at` datetime DEFAULT NULL COMMENT '最后操作时间',
  `last_operated_by` varchar(64) DEFAULT NULL COMMENT '最后操作人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_control` (`device_id`,`control_type`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感联动控制设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `environment_report_hourly`
--

DROP TABLE IF EXISTS `environment_report_hourly`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `environment_report_hourly` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `device_id` varchar(64) NOT NULL,
  `hour_time` datetime NOT NULL,
  `avg_temperature` float DEFAULT NULL,
  `avg_humidity` float DEFAULT NULL,
  `avg_dust` float DEFAULT NULL,
  `sample_count` int(11) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_hour` (`device_id`,`hour_time`),
  KEY `idx_hour_time` (`hour_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1245 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `food_safety_query`
--

DROP TABLE IF EXISTS `food_safety_query`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `food_safety_query` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `query_id` varchar(64) NOT NULL COMMENT '查询业务编号',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '逻辑关联sys_user.id',
  `food_name` varchar(128) NOT NULL COMMENT '食物名称',
  `food_category` varchar(64) DEFAULT NULL COMMENT '食物种类',
  `result` varchar(64) DEFAULT NULL COMMENT 'safe/limited/unsafe/unknown',
  `advice` varchar(500) DEFAULT NULL COMMENT '建议说明',
  `queried_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查询时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_query_id` (`query_id`),
  KEY `idx_user_food` (`user_id`,`food_name`),
  KEY `idx_queried_at` (`queried_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物安全查询记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `humidity_data`
--

DROP TABLE IF EXISTS `humidity_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `humidity_data` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(64) NOT NULL COMMENT '设备编号',
  `humidity_value` float NOT NULL COMMENT '相对湿度（%RH）',
  `record_time` datetime NOT NULL COMMENT '数据记录时间（传感器上报时间）',
  `source` varchar(32) DEFAULT 'sensor' COMMENT '数据来源：sensor-真实传感器, simulate-模拟',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`,`record_time`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB AUTO_INCREMENT=466601 DEFAULT CHARSET=utf8mb4 COMMENT='湿度历史数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `parrot_behavior_record`
--

DROP TABLE IF EXISTS `parrot_behavior_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parrot_behavior_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(64) NOT NULL COMMENT '设备编号',
  `image_url` varchar(512) NOT NULL COMMENT '截图路径/URL',
  `parrot_detected` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否检测到鹦鹉',
  `parrot_confidence` decimal(3,2) DEFAULT NULL COMMENT '鹦鹉检测置信度0.00-1.00',
  `behavior` varchar(64) DEFAULT NULL COMMENT '行为标签',
  `behavior_confidence` decimal(3,2) DEFAULT NULL COMMENT '行为置信度0.00-1.00',
  `checked_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  `species` varchar(64) DEFAULT NULL COMMENT '种类标签（虎皮/玄凤/牡丹/…）',
  `species_confidence` decimal(3,2) DEFAULT NULL COMMENT '种类置信度0.00-1.00',
  PRIMARY KEY (`id`),
  KEY `idx_device_checked` (`device_id`,`checked_at`)
) ENGINE=InnoDB AUTO_INCREMENT=5015 DEFAULT CHARSET=utf8mb4 COMMENT='鹦鹉行为识别记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pet_cage`
--

DROP TABLE IF EXISTS `pet_cage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_cage` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '逻辑关联 sys_user.id',
  `cage_id` varchar(64) NOT NULL COMMENT '笼舍业务编号',
  `cage_name` varchar(128) NOT NULL COMMENT '笼舍名称',
  `location` varchar(255) DEFAULT NULL COMMENT '安装位置',
  `device_id` varchar(64) DEFAULT NULL COMMENT '逻辑关联 smoke_device.device_id',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `enabled` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1启用，0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cage_id` (`cage_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物笼舍表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pet_daily_report`
--

DROP TABLE IF EXISTS `pet_daily_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_daily_report` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `pet_id` varchar(64) NOT NULL COMMENT 'Logical reference to pet_profile.pet_id',
  `report_date` date NOT NULL COMMENT 'Report date',
  `health_score` tinyint(3) unsigned NOT NULL COMMENT 'Health score from 0 to 100',
  `sleep_minutes` smallint(5) unsigned NOT NULL DEFAULT '0' COMMENT 'Total sleep duration in minutes',
  `vocalization_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'Number of vocalizations',
  `feeding_count` smallint(5) unsigned NOT NULL DEFAULT '0' COMMENT 'Number of feeding events',
  `excretion_count` smallint(5) unsigned NOT NULL DEFAULT '0' COMMENT 'Number of excretion events',
  `dust_avg_value` int(11) DEFAULT NULL COMMENT '当日平均粉尘浓度（ppm）',
  `temp_avg_value` float DEFAULT NULL COMMENT '当日平均温度（℃）',
  `humidity_avg_value` float DEFAULT NULL COMMENT '当日平均湿度（%RH）',
  `source` varchar(32) NOT NULL DEFAULT 'system' COMMENT 'system/manual/import',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Report remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_report_date` (`pet_id`,`report_date`),
  KEY `idx_report_date` (`report_date`),
  KEY `idx_health_score` (`health_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Pet daily growth report table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pet_ledger_record`
--

DROP TABLE IF EXISTS `pet_ledger_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_ledger_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` varchar(64) NOT NULL COMMENT '账单业务编号',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '逻辑关联sys_user.id',
  `pet_id` varchar(64) NOT NULL COMMENT '逻辑关联pet_profile.pet_id',
  `expense_date` date NOT NULL COMMENT '消费日期',
  `category` varchar(64) NOT NULL DEFAULT '其他' COMMENT '消费标签',
  `description` varchar(255) NOT NULL COMMENT '消费描述',
  `amount` decimal(10,2) unsigned NOT NULL COMMENT '支出金额',
  `currency` char(3) NOT NULL DEFAULT 'CNY' COMMENT 'ISO 4217币种代码',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ledger_id` (`ledger_id`),
  KEY `idx_pet_expense_date` (`pet_id`,`expense_date`),
  KEY `idx_user_expense_date` (`user_id`,`expense_date`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COMMENT='宠物饲养记账记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pet_media_record`
--

DROP TABLE IF EXISTS `pet_media_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_media_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `media_id` varchar(64) NOT NULL COMMENT '媒体业务编号',
  `pet_id` varchar(64) NOT NULL COMMENT '逻辑关联pet_profile.pet_id',
  `cage_id` varchar(64) DEFAULT NULL COMMENT '逻辑关联pet_cage.cage_id',
  `media_type` varchar(32) NOT NULL COMMENT 'photo/screenshot/recording/video',
  `title` varchar(128) DEFAULT NULL COMMENT '标题',
  `file_url` varchar(512) DEFAULT NULL COMMENT '文件资源URL（截图可空，改用 image_data）',
  `image_data` longtext COMMENT '截图 base64（JPEG）；fileUrl 为空时使用',
  `thumbnail_url` varchar(512) DEFAULT NULL COMMENT '缩略图URL',
  `duration_seconds` int(11) DEFAULT NULL COMMENT '录音或视频时长',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签',
  `captured_at` datetime NOT NULL COMMENT '拍摄或录制时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_id` (`media_id`),
  KEY `idx_pet_type_time` (`pet_id`,`media_type`,`captured_at`),
  KEY `idx_captured_at` (`captured_at`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COMMENT='宠物媒体记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pet_medical_record`
--

DROP TABLE IF EXISTS `pet_medical_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_medical_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` varchar(64) NOT NULL COMMENT '病历业务编号',
  `pet_id` varchar(64) NOT NULL COMMENT '逻辑关联pet_profile.pet_id',
  `record_date` date NOT NULL COMMENT '记录日期',
  `record_type` varchar(32) NOT NULL DEFAULT 'symptom' COMMENT 'symptom/diagnosis/medication/recheck/other',
  `title` varchar(128) DEFAULT NULL COMMENT '标题摘要',
  `content` text NOT NULL COMMENT '详细内容',
  `hospital_name` varchar(128) DEFAULT NULL COMMENT '医院名称',
  `hospital_phone` varchar(32) DEFAULT NULL COMMENT '医院电话',
  `attachments` json DEFAULT NULL COMMENT '附件列表',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_id` (`record_id`),
  KEY `idx_pet_date` (`pet_id`,`record_date`),
  KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='宠物病历记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pet_profile`
--

DROP TABLE IF EXISTS `pet_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_profile` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `pet_id` varchar(64) NOT NULL COMMENT 'Pet business identifier, for example PET-001',
  `user_id` bigint(20) unsigned NOT NULL COMMENT 'Logical reference to sys_user.id',
  `cage_id` varchar(64) DEFAULT NULL COMMENT '逻辑关联pet_cage.cage_id',
  `device_id` varchar(64) DEFAULT NULL COMMENT 'Logical reference to smoke_device.device_id',
  `name` varchar(64) NOT NULL COMMENT 'Pet name',
  `species` varchar(64) NOT NULL COMMENT 'Pet species or breed',
  `birthday` date DEFAULT NULL COMMENT 'Date of birth',
  `sex` varchar(16) NOT NULL DEFAULT 'unknown' COMMENT 'male/female/unknown',
  `weight_grams` decimal(8,2) DEFAULT NULL COMMENT '当前体重（克）',
  `feather_color` varchar(64) DEFAULT NULL COMMENT '羽毛颜色',
  `sterilized` tinyint(4) DEFAULT '0' COMMENT '1是，0否或未知',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT 'Avatar resource URL',
  `current_status` varchar(32) DEFAULT NULL COMMENT 'Current status, such as standing/eating/sleeping',
  `enabled` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1 enabled, 0 disabled',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_id` (`pet_id`),
  KEY `idx_user_enabled` (`user_id`,`enabled`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_cage_id` (`cage_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COMMENT='Pet profile table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pet_weight_record`
--

DROP TABLE IF EXISTS `pet_weight_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_weight_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `pet_id` varchar(64) NOT NULL COMMENT 'Logical reference to pet_profile.pet_id',
  `weight_grams` decimal(8,2) NOT NULL COMMENT 'Weight in grams',
  `measured_at` datetime NOT NULL COMMENT 'Measurement time',
  `source` varchar(32) NOT NULL DEFAULT 'manual' COMMENT 'manual/device/import',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pet_measured_at` (`pet_id`,`measured_at`),
  KEY `idx_measured_at` (`measured_at`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COMMENT='Pet weight history table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smoke_data`
--

DROP TABLE IF EXISTS `smoke_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `smoke_data` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(64) NOT NULL COMMENT '设备编号',
  `smoke_value` float NOT NULL COMMENT '烟雾浓度（ppm）',
  `risk_level` varchar(32) NOT NULL COMMENT '风险等级：normal/low/medium/high',
  `record_time` datetime NOT NULL COMMENT '数据记录时间（传感器上报时间）',
  `source` varchar(32) DEFAULT 'sensor' COMMENT '数据来源：sensor-真实传感器, simulate-模拟',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`,`record_time`),
  KEY `idx_record_time` (`record_time`),
  KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB AUTO_INCREMENT=25618 DEFAULT CHARSET=utf8mb4 COMMENT='烟雾浓度历史数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smoke_device`
--

DROP TABLE IF EXISTS `smoke_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `smoke_device` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(64) NOT NULL COMMENT '设备业务编号',
  `name` varchar(128) NOT NULL COMMENT '设备名称',
  `cage_id` varchar(64) DEFAULT NULL COMMENT '逻辑关联 pet_cage.cage_id',
  `location` varchar(255) DEFAULT NULL COMMENT '安装位置',
  `online` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1在线，0离线',
  `last_heartbeat` datetime DEFAULT NULL COMMENT '最近心跳时间',
  `current_smoke_value` int(11) DEFAULT '0' COMMENT '当前烟雾浓度ppm',
  `current_risk_level` varchar(32) DEFAULT 'normal' COMMENT 'normal/low/medium/high',
  `current_alarm_status` varchar(32) DEFAULT 'safe' COMMENT 'safe/alarm/offline',
  `enabled` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1启用，0禁用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_online` (`online`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_alarm_status` (`current_alarm_status`),
  KEY `idx_cage_id` (`cage_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_setting`
--

DROP TABLE IF EXISTS `system_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_setting` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `setting_key` varchar(128) NOT NULL COMMENT '配置键',
  `setting_value` varchar(512) NOT NULL COMMENT '配置值',
  `setting_group` varchar(64) DEFAULT 'general' COMMENT '配置分组',
  `description` varchar(255) DEFAULT NULL COMMENT '配置说明',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`setting_key`),
  KEY `idx_group` (`setting_group`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感系统设置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '加密密码',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `role` varchar(32) NOT NULL DEFAULT 'viewer' COMMENT '角色：admin/viewer',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `avatar_image` longtext COMMENT '头像 base64（JPEG data URI）',
  `location` varchar(255) DEFAULT NULL COMMENT '用户位置信息',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='智慧烟感用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `temperature_data`
--

DROP TABLE IF EXISTS `temperature_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temperature_data` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(64) NOT NULL COMMENT '设备编号',
  `temperature_value` float NOT NULL COMMENT '温度（℃）',
  `record_time` datetime NOT NULL COMMENT '数据记录时间（传感器上报时间）',
  `source` varchar(32) DEFAULT 'sensor' COMMENT '数据来源：sensor-真实传感器, simulate-模拟',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`,`record_time`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB AUTO_INCREMENT=486347 DEFAULT CHARSET=utf8mb4 COMMENT='温度历史数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_preference`
--

DROP TABLE IF EXISTS `user_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preference` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '逻辑关联 sys_user.id',
  `pref_key` varchar(128) NOT NULL COMMENT '偏好项键名',
  `pref_value` varchar(512) NOT NULL COMMENT '偏好项值',
  `pref_group` varchar(64) DEFAULT 'general' COMMENT '偏好分组',
  `description` varchar(255) DEFAULT NULL COMMENT '说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pref_key` (`user_id`,`pref_key`),
  KEY `idx_user_group` (`user_id`,`pref_group`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vision_check`
--

DROP TABLE IF EXISTS `vision_check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vision_check` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` varchar(64) NOT NULL COMMENT '关联智慧烟感告警编号',
  `device_id` varchar(64) NOT NULL COMMENT '设备编号',
  `image_url` varchar(512) NOT NULL COMMENT '摄像头截图URL',
  `ai_result` varchar(64) DEFAULT NULL COMMENT 'AI识别结果',
  `confidence` decimal(3,2) DEFAULT NULL COMMENT '置信度0.00-1.00',
  `confirmed` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否人工确认',
  `confirmed_by` varchar(64) DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `checked_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_id` (`alarm_id`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI视觉复核记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-12 17:22:18
