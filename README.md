# 智慧宠物烟感安全系统

> 基于 MQTT 物联网关 + Spring Boot + Vue 3 的「宠物笼舍环境监测 + 烟感安全告警 + 宠物成长照护」综合平台：以烟感/粉尘监测守护宠物（尤其是鹦鹉等小型宠物）的生活环境，烟雾或粉尘超标自动告警并联动排风扇/报警灯/蜂鸣器/空气净化器保护宠物，同时提供宠物档案、成长报告、医疗助手、记账本、饲养手册等一站式照护能力。

![学习路径图示](学习路径图示.jpg)

---

## 目录 / Table of Contents

- [功能特性 / Features](#功能特性--features)
- [技术栈 / Tech Stack](#技术栈--tech-stack)
- [系统架构 / Architecture](#系统架构--architecture)
- [仓库结构 / Repository Structure](#仓库结构--repository-structure)
- [快速开始 / Quick Start](#快速开始--quick-start)
- [开发指南 / Development Guide](#开发指南--development-guide)
- [外部服务与凭据 / External Services](#外部服务与凭据--external-services)
- [数据库设计 / Database Design](#数据库设计--database-design)
- [API 接口 / API Reference](#api-接口--api-reference)
- [部署说明 / Deployment](#部署说明--deployment)
- [开发进度 / Project Status](#开发进度--project-status)
- [协作规范 / Collaboration](#协作规范--collaboration)
- [相关文档 / Related Docs](#相关文档--related-docs)

---

## 功能特性 / Features

系统围绕「采集 → 入库 → 判断 → 告警 → 联动 → 展示 → 照护」闭环设计，核心能力包括：

- **宠物笼舍实时环境监测**：硬件传感器采集烟雾/粉尘、温度、湿度 → MQTT 上报 → 后端入库 → 前端每 3 秒轮询呈现。
- **实时视频通话监控**：前端模拟实时视频画面，支持全屏、截图归档到宠物成长相册、麦克风与音量控制。
- **环境指标仪表盘**：温度、湿度、粉尘浓度实时卡片，点击可查看仪表盘详情；温湿度按舒适区间展示「偏低/适宜/偏高」。
- **历史浓度趋势可视化**：ECharts 折线图，支持实时 / 6h / 12h / 24h / 7d 多时间范围切换，叠加中风险与高风险阈值线。
- **阈值告警与风险分级**：按 ppm 自动映射 `normal` / `low` / `medium` / `high` 四级，超阈值自动生成告警记录；支持温湿度异常与设备离线告警。
- **设备联动控制**：危险状态自动联动蜂鸣器 / 报警灯 / 排风扇 / 空气净化器，前端可手动控制开关。
- **宠物档案管理**：支持多只宠物档案、头像、品种、生日、体重、性别、笼舍绑定，支持新增/编辑/切换宠物。
- **成长报告**：日报 / 周报 / 月报，展示健康评分、睡眠、鸣叫、进食、排泄指标及温度/湿度/粉尘/体重曲线。
- **医疗助手**：外在表现问卷智能问诊、附近异宠医院查询、病历记录增删改查。
- **饲养手册**：教程库、食物安全查询、拍照识鸟。
- **记账本**：按宠物记录饲养支出，支持日期、标签、描述、金额，支持编辑与汇总。
- **MQTT 数据自动入库**：`device/getData` 订阅公网 MQTT `group23`，解析 `ppm`、`℃`、`%RH` 并分别写入三张传感数据表。
- **告警全生命周期管理**：告警触发 → 处理中 → 已处理，支持处理人备注与时间线追溯。
- **可扩展加分项**：预留 AI 视觉复核（SmartJavaAI）与警情智能问答（MaxKB / RAG）接口。

> 完整用户故事与业务流程见 [03_智慧烟感_基本功能清单.md](03_智慧烟感_基本功能清单.md)。

---

## 技术栈 / Tech Stack

| 模块 | 路径 | 技术栈 | 端口 | 作用 |
|---|---|---|---|---|
| 后端 Backend | [backend/](backend/) | Java 17 · Spring Boot 3.3.5 · Maven · MySQL 5.7 · Spring Data JPA · Lombok · Validation | `8080` | 业务 API、数据入库、风险判断、告警生成 |
| 前端 Frontend | [frontend/](frontend/) | Vue 3.5.17 · Vite 6.3.5 · ECharts 5.6.0 | `5173` (dev) | 宠物照护大屏、实时监控、成长报告 |
| 设备端·数据消费 | [device/getData/](device/getData/) | Java 8 · Spring Boot 2.3.5 · Paho MQTT · JDBC · Hutool | — | 订阅 `group23`，写入烟雾、温度、湿度表 |
| 设备端·控制转发 | [device/postData/](device/postData/) | Java 8 · Spring Boot 2.3.5 · Paho MQTT · JDBC | — | 读取 `device_control` 状态并转发到 MQTT 控制主题 |
| 设备端·数据模拟 | [device/simulate/](device/simulate/) | Java 8 · Spring Boot 2.3.5 · Paho MQTT | — | 每秒发布正态分布温湿度数据 |
| 设备端·MQTT 工具 | [device/MQTT/mqtt01-master/](device/MQTT/mqtt01-master/) | Java 8 · Spring Boot 2.3.5 · Paho MQTT · Web · Hutool | `1883` | MQTT 收发工具 + REST API 控制设备 |

---

## 系统架构 / Architecture

```
                          ┌──────────────────────┐
                          │   硬件传感器 / 小熊派  │
                          └──────────┬───────────┘
                                     │ 硬件发布烟雾/粉尘
                                     ▼
                          ┌──────────────────────┐
                          │   MQTT Broker 公网    │
                          │ 47.108.58.107:1883    │
                          │ topic: group23        │
                          │ control: group23-s-to-h│
                          └──────┬───────────┬───┘
                                 │           │
                  订阅 group23   │           │  读取 device_control
                                 │           │  发布到 group23-s-to-h
                                 ▼           ▼
              ┌──────────────────────┐   ┌──────────────────────┐
              │  device/getData      │   │  device/postData      │
              │  (数据消费服务)       │   │  (控制信号转发服务)    │
              │  解析 ppm / ℃ / %RH   │   │  同步 buzzer/led/fan  │
              │  写入三张传感数据表   │   │  状态到硬件           │
              └────────┬─────────────┘   └──────────────────────┘
                       │ INSERT smoke_data
                       │ temperature_data / humidity_data
                       ▼
              ┌──────────────────────┐
              │     MySQL 5.7        │   ← backend (Spring Data JPA)
              │ 47.108.58.107:3306   │     查询/入库/告警判断/宠物照护
              │  database: dream28    │
              └──────────┬───────────┘
                         │ HTTP API (port 8080)
                         ▼
              ┌──────────────────────┐
              │   frontend Vue 3     │
              │   宠物照护大屏 (5173) │
              └──────────────────────┘
```

**数据流说明**：
1. 硬件传感器向 `group23` 发布烟雾/粉尘浓度；`device/simulate` 向同一主题发布模拟温度和湿度。
2. `device/getData` 解析 `ppm`、`℃`、`%RH`，分别写入 `smoke_data`、`temperature_data`、`humidity_data`。
3. `device/postData` 每秒读取 `device_control` 表状态变化，转发到 `group23-s-to-h` 控制主题驱动硬件执行。
4. `backend` Spring Boot 服务通过 JPA 读写 MySQL，提供烟雾/粉尘实时与历史数据、温湿度查询、告警、设备控制、宠物档案等接口。
5. `frontend` Vue 页面以宠物为中心，展示实时监控视频、环境指标、成长报告、医疗助手、记账本、饲养手册等模块。
6. `device/MQTT` 工具模块提供 REST 接口，用于向设备下发控制指令（开关蜂鸣器/报警灯/排风扇）。

---

## 仓库结构 / Repository Structure

```
Chinasoft-Project-group23/
├── backend/                      # 后端 Spring Boot 服务 (Java 17)
│   ├── pom.xml
│   ├── README.md                 # 后端说明
│   └── src/main/java/com/chinasoft/smokesensor/
│       ├── SmokeSensorApplication.java
│       ├── common/               # ApiResult / BusinessException / GlobalExceptionHandler
│       ├── config/               # VisionProperties 等配置
│       ├── controller/           # Smoke / Alarm / Device / System / Runtime / Vision / DeviceData / Settings
│       ├── dto/                  # 请求/响应 DTO
│       ├── entity/               # Device / SensorData / AlarmRecord / DeviceControl / SystemSetting / VisionCheck / TemperatureData / HumidityData
│       ├── repository/           # JPA Repository
│       ├── service/              # 业务接口
│       └── service/impl/         # 业务实现
├── frontend/                     # 前端 Vue 3 + Vite 宠物照护大屏
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── App.vue               # 主组件（宠物照护首页+详情路由）
│       ├── api/                  # API 调用层（smoke / alarm / device / request）
│       ├── components/           # CurrentBirdCard / EntryCard / MonitorCard / ParrotVisual
│       ├── data/mockDashboard.js # mock 数据与业务配置
│       ├── styles.css            # 主题与组件样式
│       └── main.js
├── device/                       # 设备端
│   ├── MQTT/mqtt01-master/       # MQTT 收发工具 + REST API
│   ├── getData/                  # MQTT 数据订阅与三类数据入库服务
│   ├── postData/                 # 数据库控制状态转发到 MQTT
│   └── simulate/                 # 温湿度正态分布 MQTT 模拟器
│       └── README.md
├── docs/                         # 项目需求文档
│   └── PROJECT_REQUIREMENTS.md
├── 文档/                          # 架构 / API / 数据库设计文档
│   ├── 智慧烟感系统架构设计.md
│   ├── 智慧烟感API接口文档.md
│   └── 智慧烟感数据库表结构设计.md
├── 原型设计/                      # 原型图资源
├── 03_智慧烟感_基本功能清单.md
├── 智慧烟感数据库表结构设计.md      # 根目录副本（智慧宠物烟感安全系统 v2.0）
├── 生成考勤与开发日志表.py
├── .gitignore
└── README.md                     # 本文件
```

> `device/MQTT/__MACOSX/` 为 macOS 压缩包产生的元数据垃圾文件，可忽略。

---

## 快速开始 / Quick Start

### 环境要求 / Prerequisites

| 工具 | 版本 | 用途 |
|---|---|---|
| JDK | 17+ | 后端运行（设备端模块需 JDK 8+） |
| Maven | 3.8+ | Java 依赖管理与构建 |
| Node.js | 18+ | 前端运行 |
| MySQL | 8.0+ | 数据存储 |
| Git | 任意 | 版本控制 |

### 一键启动本地开发链路 / One-Click Local Dev Chain

> ⚠️ 启动前请先确认能访问下方 [外部服务](#外部服务与凭据--external-services) 中的公网 MQTT 与 MySQL，或在各 `application.yml` 中替换为你本地的地址。

要让前端看到**模拟的实时烟感数据**，需要整条链路都跑起来：`getData`（订阅 MQTT 入库）→ `simulate`（每秒发模拟数据）→ `backend`（REST API）。项目根目录提供了启动脚本，按依赖顺序拉起这三个服务，`Ctrl+C` 一起停止：

```powershell
# PowerShell
.\start-local.ps1
```

如果 PowerShell 执行策略限制运行 `.ps1`，双击 `start-local.bat` 即可（它会自动绕过策略调用同一个脚本）。

脚本会打开三个带标题的控制台窗口（`dev-getData` / `dev-simulate` / `dev-backend`），并在启动每个服务后等待一段就绪时间，避免下游还没连上 MQTT 数据就到了。

---

### 各模块启动 / Start Each Module

如需单独启动某个服务（例如调试单个模块），可手动进入对应目录执行：

**1. 后端 Backend**

```bash
cd backend
# 按需修改 src/main/resources/application.yml 中的数据库连接
mvn spring-boot:run
# 启动后监听 http://localhost:8080
```

**2. 前端 Frontend**

```bash
cd frontend
npm install
npm run dev
# 启动后监听 http://localhost:5173
```

**3. 设备端·数据消费 getData**

```bash
cd device/getData
mvn spring-boot:run
# 自动订阅 MQTT group23，解析烟雾、温度、湿度并分别入库
```

**4. 设备端·控制转发 postData**

```bash
cd device/postData
mvn spring-boot:run
# 每秒读取 device_control 状态变化并转发到 group23-s-to-h
```

**5. 温湿度模拟器**

```bash
cd device/simulate
mvn spring-boot:run
# 每秒向 group23 分别发布一条温度和湿度消息
```

**6. 设备端·MQTT 工具**

```bash
cd device/MQTT/mqtt01-master
mvn spring-boot:run
# 启动后监听 http://47.108.58.107:1883
```

---

## 开发指南 / Development Guide

### 后端 Backend

- **包根**：`com.chinasoft.smokesensor`
- **分层规范**：`controller`（仅接收请求/返回结果）→ `service/impl`（业务逻辑）→ `repository`（数据库操作）→ `entity`（表映射）→ `dto`（请求/响应载体）。业务逻辑**不得**写在 Controller 中。
- **统一响应**：所有接口返回 [common/ApiResult.java](backend/src/main/java/com/chinasoft/smokesensor/common/ApiResult.java) 包装的 `{code, message, data}` 结构。
- **异常处理**：业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一捕获。
- **数据库**：`spring.jpa.hibernate.ddl-auto: none`，**不自动建表**，需手动执行 [智慧烟感数据库表结构设计.md](智慧烟感数据库表结构设计.md) 中的建表 SQL。
- **风险等级阈值**（与设备端一致）：

  | ppm 区间 | 风险等级 |
  |---|---|
  | 0–100 | `normal` |
  | 101–199 | `low` |
  | 200–400 | `medium` |
  | >400 | `high` |

- **宠物环境舒适区间**：

  | 指标 | 偏低 | 适宜 | 偏高 |
  |---|---|---|---|
  | 温度 | <18℃ | 18–30℃ | >30℃ |
  | 湿度 | <40% | 40–70% | >70% |
  | 粉尘/羽粉 | <35 μg/m³ | 35–80 μg/m³ | >80 μg/m³ |

- **构建**：`mvn clean package`
- 详细说明见 [backend/README.md](backend/README.md)。

### 前端 Frontend

- **开发端口**：`5173`，已配置 `--host 0.0.0.0` 供局域网访问。
- **主题**：在 [src/styles.css](frontend/src/styles.css) 中定义 `.safe-theme` / `.low-theme` / `.medium-theme` / `.high-theme` 四套，通过 `document.body.className` 切换；同时支持白天/夜间模式。
- **数据轮询**：`MonitorCard` 每 3 秒调用一次 `/api/smoke/realtime` 接口驱动环境指标。
- **API 调用层**：[src/api/](frontend/src/api/)，`smoke.js` 的 `getRealtimeSmoke` 已接入后端（3 秒轮询）；设备控制（`device.js`）与鹦鹉照护（`care.js`）已封装但**尚未接入组件**。
- **mock 数据与业务配置**：[src/data/mockDashboard.js](frontend/src/data/mockDashboard.js) 中定义宠物、入口卡片、成长报告、医疗模块、饲养手册等配置。

### 设备端·数据消费 getData

- **消息格式**：独立发送 `{"ppm":86.5}`、`{"℃":25.3}` 或 `{"%RH":49.8}`。
- **固定字段**：`device_id` 固定 `SMK-001`，`record_time` 由 MySQL `NOW()` 生成；烟雾来源为 `sensor`，温湿度来源为 `simulate`。
- **环境变量覆盖**：支持 `MQTT_HOST_URL` / `MQTT_DATA_TOPIC` / `MQTT_USERNAME` / `MQTT_PASSWORD` / `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` 等，详见 [device/getData/README.md](device/getData/README.md)。
- **远端联调测试**（不会被普通 `mvn test` 自动执行）：

  ```powershell
  mvn -Dtest=RemoteMqttIntegrationIT test
  ```

### 设备端·控制转发 postData

- **功能**：每秒读取 `dream28.device_control` 中设备 `SMK-001` 的控制状态，并把变化发布到 MQTT 主题 `group23-s-to-h`。
- **控制字段映射**：`buzzer` → `{"buzzer":1/0}`，`alarm_light` → `{"led":1/0}`，`switch` → `{"switch":1/0}`。
- 详见 [device/postData/README.md](device/postData/README.md)。

### 设备端·MQTT 工具

- **端口**：`1883`
- **REST 接口**：
  - `GET /publishTopic?sendMessage=xxx` — 向默认主题 `group23` 发布消息
  - `GET /on` · `POST /on` — 向控制主题 `smoke/control` 发送 `1`（开启联动设备）
  - `GET /off` · `POST /off` — 向控制主题发送 `0`（关闭联动设备）
  - `POST /login` — 登录接口

---

## 外部服务与凭据 / External Services

> ⚠️ **以下为开发环境凭据，已在各文档与配置中暴露，仅供开发联调使用，请勿用于生产环境。生产部署请通过环境变量注入。**

### MQTT Broker

| 项 | 值 |
|---|---|
| 地址 | `47.108.58.107:1883` |
| 数据主题 | `group23` |
| 控制主题 | `group23-s-to-h` |
| 用户名/密码 | 空（公网匿名访问） |

### MySQL 数据库

| 项 | 值 |
|---|---|
| IP | `47.108.58.107` |
| 端口 | `3306` |
| 数据库 | `dream28` |
| 用户名 | `root` |
| 密码 | `c0765083cd3f57ab` |
| JDBC URL | `jdbc:mysql://47.108.58.107:3306/dream28?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true` |

### 参考工具与地址

- [小熊派 BearPi](https://gitee.com/bearpi/bearpi-hm_nano/tree/master) — 硬件开发板
- [DataEase](https://dataease.io/index.html) — 数据可视化工具
- [MaxKB](https://maxkb.cn/) — 智能体构造工具（用于警情问答）
- [SmartJavaAI](http://doc.numberone.ink/) — Java 视觉工具库（用于明火/烟雾/宠物异常识别）

---

## 数据库设计 / Database Design

共 **20 张表**（含 `parrot_behavior_record`），字符集 `utf8mb4`，引擎 `InnoDB`。详细建表 SQL 与索引策略见 [智慧烟感数据库表结构设计.md](智慧烟感数据库表结构设计.md)。

| # | 表名 | 中文名 | 说明 |
|---|---|---|---|
| 1 | `sys_user` | 用户表 | 登录账号与权限（admin / viewer） |
| 2 | `user_preference` | 用户偏好表 | 主题、字体、字号、语言、通知开关等 |
| 3 | `pet_cage` | 宠物笼舍表 | 笼舍/监测区域，绑定监测设备 |
| 4 | `smoke_device` | 烟感设备表 | 设备基本信息与当前状态（含冗余的最新浓度字段） |
| 5 | `device_control` | 联动设备表 | 蜂鸣器/报警灯/排风扇/空气净化器状态与自动联动标识 |
| 6 | `system_setting` | 系统设置表 | KV 形式存储阈值、心跳超时、环境舒适区间等全局配置 |
| 7 | `smoke_data` | 烟雾/粉尘数据表 | 历史浓度数据，量最大，按设备+时间索引 |
| 8 | `temperature_data` | 温度数据表 | 温度历史数据，单位 ℃，按设备+时间索引 |
| 9 | `humidity_data` | 湿度数据表 | 相对湿度历史数据，单位 %RH，按设备+时间索引 |
| 10 | `alarm_record` | 告警记录表 | 告警事件主表，状态 pending→processing→resolved |
| 11 | `alarm_timeline` | 告警时间线表 | 告警生命周期事件（触发/联动/处理/恢复） |
| 12 | `vision_check` | 视觉复核表 | AI 摄像头复核结果（加分项 P2） |
| 12a | `parrot_behavior_record` | 鹦鹉行为识别表 | AI 鹦鹉种类+行为识别历史（🟢 完整实现） |
| 13 | `pet_profile` | 宠物档案表 | 宠物资料及所属用户、笼舍关联 |
| 14 | `pet_weight_record` | 宠物体重记录表 | 历史体重与测量时间，用于体重趋势 |
| 15 | `pet_daily_report` | 宠物成长日报表 | 健康评分、睡眠、鸣叫、进食、排泄指标 |
| 16 | `pet_media_record` | 宠物媒体记录表 | 照片、截图、录音、视频记录（成长相册） |
| 17 | `pet_medical_record` | 宠物病历记录表 | 就诊、用药、症状与复查记录 |
| 18 | `pet_ledger_record` | 宠物记账记录表 | 按宠物记录饲养支出，支持日期和标签查询 |
| 19 | `food_safety_query` | 食物安全查询表 | 食物可食用性查询历史 |

> **图例**：🟢 有完整后端实现 | 🔴 仅有建表 SQL / 无后端接口 | ⚪ 概念表

**核心关系**：`pet_cage` 1:1 `smoke_device`，1:N `pet_profile` / `alarm_record` / `pet_media_record`；`smoke_device` 1:N `smoke_data` / `temperature_data` / `humidity_data` / `alarm_record` / `device_control` / `parrot_behavior_record`；`alarm_record` 1:N `alarm_timeline` / `vision_check`；`sys_user` 1:N `pet_profile` / `user_preference` / `pet_ledger_record` / `food_safety_query`；`pet_profile` 1:N `pet_weight_record` / `pet_daily_report` / `pet_media_record` / `pet_medical_record` / `pet_ledger_record`。以上均为逻辑关联，不创建数据库外键。

---

## API 接口 / API Reference

后端 BaseURL：`http://<服务器IP>:8080/api`，统一返回 `{code, message, data}`。共定义约 22 个接口（P0 必做 10 个 / P1 建议 8 个 / P2 加分 4 个），完整字段与错误码见 [文档/智慧烟感API接口文档.md](文档/智慧烟感API接口文档.md)。

| 模块 | 方法 | 路径 | 优先级 | 说明 |
|---|---|---|---|---|
| 鉴权 | POST | `/auth/login` | P1 | 账号密码登录，返回 token |
| 系统 | GET | `/system/status` | P0 | 系统在线状态、当前时间、在线设备数 |
| 运行态 | GET | `/runtime/link-snapshot` | P0 | 连接快照，用于页面初始化 |
| 烟雾数据 | GET | `/smoke/latest` | P0 | 最新浓度、风险等级、报警状态 |
| 烟雾数据 | GET | `/smoke/realtime` | P0 | 实时浓度 + 温度 + 湿度（3s 轮询） |
| 烟雾数据 | GET | `/smoke/history` | P0 | 历史浓度趋势（ECharts 折线图） |
| 烟雾数据 | POST | `/smoke/simulate` | P0 | 模拟烟雾/粉尘升高（课堂演示） |
| 烟雾数据 | POST | `/smoke/restore` | P0 | 恢复正常环境，解除告警 |
| 传感器 | POST | `/sensor/upload` | P0 | 硬件上传烟雾数据 |
| 告警 | GET | `/alarm/stat/today` | P0 | 今日告警次数与较昨日变化 |
| 告警 | GET | `/alarm/logs` | P0 | 告警记录列表（分页/筛选） |
| 告警 | GET | `/alarm/{id}` | P1 | 告警详情（含时间线与曲线片段） |
| 告警 | POST | `/alarm/handle` | P1 | 处理告警，填写处理人备注 |
| 设备 | POST | `/device/control` | P0 | 控制蜂鸣器/报警灯/排风扇开关 |
| 设备 | GET | `/devices` | P1 | 设备列表（筛选/搜索） |
| 设备 | GET | `/device/status` | P0 | 设备在线状态与各受控设备开关 |
| 设备 | GET | `/device/info` | P0 | 单设备详情 |
| 设备 | POST / PUT / DELETE | `/devices` / `/devices/{deviceId}` | P1 | 新增/编辑/解绑设备 |
| 系统设置 | GET/POST | `/settings/threshold` | P1 | 读取/保存风险阈值 |
| 宠物照护 | 15 个端点 | `/parrots/**` | P1 | 档案/体重/病历/账本/相片增删改查（详见 [API 文档 4.9](文档/智慧烟感API接口文档.md#49-鹦鹉照护数据模块)） |
| 智能问答 | POST | `/agent/chat` | P2 | 警情应急建议与知识库问答 |
| 视觉复核 | GET | `/vision/check` | P2 | AI 摄像头截图与识别结果 |

> ℹ️ **截至 2026-07-08 后端实现状态**：核心 P0 接口（系统状态、烟雾数据/历史/模拟/恢复、传感器上传、告警日志/统计、设备状态/信息）已全部落地；**鹦鹉照护 15 个 RESTful 接口**（`/parrots/**` 档案/体重/病历/账本/相片）也已全部实现；前端 `src/api/care.js` 已封装全部照护 API，但组件尚未完全从 mock 切换到真实调用。详见 [开发进度](#开发进度--project-status) 与 [API 文档 4.9](文档/智慧烟感API接口文档.md#49-鹦鹉照护数据模块)。

---

## 部署说明 / Deployment

当前项目以**本地开发模式**运行，暂不使用容器化或 K8s 部署。开发时推荐用 [`start-local.ps1`](#一键启动本地开发链路--one-click-local-dev-chain)（或双击 `start-local.bat`）一键拉起 `getData + simulate + backend`；前端仍单独进入 `frontend/` 执行 `npm run dev`。如需单独调试某个模块，可参见 [各模块启动](#各模块启动--start-each-module) 章节手动进入对应目录执行。

> 如后续需要容器化部署，可自行编写前后端 Dockerfile 并配置 docker-compose 或编排工具。

---

## 开发进度 / Project Status

> 如实反映截至 2026-07-08 的开发状态，供团队成员与答辩参考。

| 模块 | 状态 | 说明 |
|---|---|---|
| 后端·骨架 | ✅ 已完成 | entity / repository / service / dto / ApiResult / 全局异常处理 |
| 后端·Controller | ⚠️ 部分完成 | 已实现烟雾、告警、设备状态/信息、运行时快照、模拟/恢复、传感器上传等核心 P0 接口；鹦鹉照护 15 个 `/parrots/**` 接口已落地；剩余：`auth/login` 鉴权、告警详情 `alarm/{id}`、部分宠物表（成长日报/食物安全/用户偏好/笼舍）后端接口 |
| 前端 | ⚠️ 重构中 | 已重构为宠物智能照护首页，包含实时监控、环境指标、宠物档案、成长报告、医疗助手、记账本、饲养手册等模块；部分数据仍走 mock |
| 设备端·getData | ✅ 已完成 | MQTT 订阅 → 三类消息解析 → 分流写入三张数据表，含单元测试 |
| 设备端·postData | ✅ 已完成 | 读取 `device_control` 状态变化并转发到 `group23-s-to-h` |
| 设备端·simulate | ✅ 已完成 | 每秒发布限定范围内的正态分布温湿度数据 |
| 设备端·MQTT 工具 | ✅ 已完成 | 收发消息 + REST API（`/publishTopic` `/on` `/off` `/login`） |
| 数据库表 | ⚠️ 部分完成 | 20 张表设计已全部完成（含 `parrot_behavior_record`）；14 张有完整后端实现；`sys_user`/`user_preference`/`pet_cage`/`alarm_timeline` 仅有建表 SQL 无后端接口；`pet_daily_report`/`food_safety_query` 为概念表 |
| 温湿度数据链路 | ✅ 已完成 | MQTT 模拟、解析、JDBC 入库、后端查询 `/smoke/realtime` 返回真实温湿度均已完成 |
| SmartJavaAI 视觉复核 | 🚧 骨架已搭 | 仅引入 vision 模块 @1.1.2（精简 face/ocr/speech），`/api/vision/check` 火焰/烟雾复核骨架完成；待接入自定义 YOLO 模型 |
| 鹦鹉行为识别 | 🚧 骨架已搭 | `/api/parrot/behavior` 骨架完成（YOLO 检测 bird + CLIP 零样本行为分类）；待接入 COCO YOLO + CLIP 模型 |

**下一步 TODO**：

1. 将前端鹦鹉照护模块从 mock 切换到真实 `/parrots/**` 接口（`care.js` 已封装，组件接入即可）。
2. 补全 `auth/login` 鉴权与告警详情 `alarm/{id}` 后端接口。
3. 按优先级落地剩余后端接口：`sys_user`/`user_preference`/`pet_cage`/`alarm_timeline`（4 张仅有建表的表）。
4. 接入 SmartJavaAI 视觉复核与 MaxKB 智能问答（P2 加分项）。
   - ✅ SmartJavaAI 依赖已引入（仅 vision @1.1.2，精简 face/ocr/speech，见 [backend/pom.xml](backend/pom.xml)）。
   - ✅ `/api/vision/check` 火焰/烟雾复核骨架已搭（对接 SmartJavaAI YOLO 目标检测）。
   - ✅ `/api/parrot/behavior` 鹦鹉行为识别骨架已搭（YOLO 检测 bird → 裁剪 → CLIP 零样本行为分类）。
   - ⏳ 待接入：火焰/烟雾自定义 YOLO 模型 + COCO YOLO（bird 类）+ CLIP 模型，在 `application.yml` 配置 `smartjavaai.vision.*` / `parrot.*` 后启用。
   - ⏳ MaxKB 智能问答待接入。

---

## 协作规范 / Collaboration

### 克隆仓库 / Clone

```bash
# HTTPS（推荐新手）
git clone https://github.com/Aetik-yue/Chinasoft-Project-group23.git

# SSH（需先配置 SSH 密钥）
git clone git@github.com:Aetik-yue/Chinasoft-Project-group23.git

cd Chinasoft-Project-group23
```

首次提交前请配置用户信息：

```bash
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

### Git 工作流 / Workflow

1. 从 `main` 拉新分支开发，**不要直接在 `main` 上修改**：

   ```bash
   git checkout -b feature/你的功能名
   ```

2. 提交更改（建议使用清晰的中文 commit 信息）：

   ```bash
   git add .
   git commit -m "feat: 新增宠物档案接口"
   ```

3. 推送并发起 Pull Request：

   ```bash
   git push origin feature/你的功能名
   ```

4. 分叉分支合并时如遇冲突，参考 `git merge` 自动合并策略，冲突文件手动解决后 `git add` + `git commit` 完成。

### .gitignore 要点

仓库已忽略以下内容（见 [.gitignore](.gitignore)）：

- IDE：`.idea/` `*.iml` `.vscode/`
- Java：`target/` `*.class`
- 前端：`node_modules/` `dist/`
- 本地配置：`.env` `application-local.yml`
- 系统：`.DS_Store` `Thumbs.db` `*.log`

> 请勿提交本地凭据文件，敏感配置请放在 `application-local.yml`（已忽略）中。

---

## 相关文档 / Related Docs

### 设计文档

- [03_智慧烟感_基本功能清单.md](03_智慧烟感_基本功能清单.md) — 用户故事与业务流程
- [docs/PROJECT_REQUIREMENTS.md](docs/PROJECT_REQUIREMENTS.md) — 后端项目需求与第一阶段目标
- [文档/智慧烟感系统架构设计.md](文档/智慧烟感系统架构设计.md) — 系统架构设计
- [文档/智慧烟感API接口文档.md](文档/智慧烟感API接口文档.md) — 接口完整定义（v1.2，含鹦鹉照护 15 端点 + imageBase64 截图存库 + /smoke/realtime）
- [文档/智慧烟感数据库表结构设计.md](文档/智慧烟感数据库表结构设计.md) — 最新版（v2.2，含 image_data 列 + 后端实现状态表）

### 子模块 README

- [backend/README.md](backend/README.md) — 后端技术栈与构建说明
- [device/getData/README.md](device/getData/README.md) — MQTT 数据接收服务说明
- [device/postData/README.md](device/postData/README.md) — 控制信号转发服务说明
- [device/simulate/README.md](device/simulate/README.md) — 温湿度 MQTT 模拟器说明

---

## 学习路径 / Learning Path

![学习路径图示](学习路径图示.jpg)

> 大家有什么好的想法或者好的主意，可以提交到这个仓库，可以提交 txt 文件，也可以提交 markdown 文件。如遇到问题，可以在仓库中发起 **Issue** 或联系仓库管理员。
