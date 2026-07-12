# 智慧鹦鹉养护系统

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

- **宠物笼舍实时环境监测**：硬件传感器采集烟雾/粉尘、温度、湿度 → MQTT 上报 → 后端入库 → 前端监控卡片每 500ms 拉取实时接口呈现。
- **实时视频通话监控**：前端模拟实时视频画面，支持全屏、截图归档到宠物成长相册、麦克风与音量控制。
- **环境指标仪表盘**：温度、湿度、粉尘浓度实时卡片，点击可查看仪表盘详情；温湿度按舒适区间展示「偏低/适宜/偏高」。
- **历史浓度趋势可视化**：自研 SVG/Canvas 折线图，支持实时 / 6h / 12h / 24h / 7d 多时间范围切换，叠加中风险与高风险阈值线。
- **阈值告警与风险分级**：按 ppm 自动映射 `normal` / `low` / `medium` / `high` 四级，超阈值自动生成告警记录；支持温湿度异常与设备离线告警。
- **设备联动控制**：危险状态自动联动蜂鸣器 / 报警灯 / 排风扇 / 空气净化器，前端可手动控制开关。
- **宠物档案管理**：支持多只宠物档案、头像、品种、生日、体重、性别、笼舍绑定，支持新增/编辑/切换宠物。
- **成长报告**：日报 / 周报 / 月报，展示健康评分、睡眠、鸣叫、进食、排泄指标及温度/湿度/粉尘/体重曲线。
- **医疗助手**：外在表现问卷智能问诊、附近异宠医院查询、病历记录增删改查。
- **饲养手册**：教程库（Markdown 教程，支持列表 + 详情页）、食物安全查询、拍照识鸟。
- **登录鉴权**：账号密码注册 / 登录（用户名或已绑定手机号 + 密码均可登录）、短信验证码登录、`GET /auth/me` 获取真实用户资料；登录态守卫，未登录跳转登录页。
- **用户设置**：展示真实用户名、角色、绑定手机号/邮箱、用户 ID 和位置信息，支持编辑头像（**圆形裁剪**，`vue-advanced-cropper` 裁剪后以 base64 存 `sys_user.avatar_image`）、账号、手机、邮箱、位置及通知/主题偏好；刷新页面后自动从后端拉取并恢复用户资料。
- **记账本**：按宠物记录饲养支出，支持日期、标签、描述、金额，支持编辑与汇总。
- **MQTT 数据自动入库**：`device/getData` 订阅公网 MQTT `group23`，解析 `ppm`、`℃`、`%RH` 并分别写入三张传感数据表。
- **告警全生命周期管理**：告警触发 → 处理中 → 已处理，支持处理人备注与时间线追溯。
- **🔥 AI 鹦鹉行为识别**：截图经 YOLO 检测鸟 → 裁剪 → CLIP 零样本分类行为（进食/饮水/梳理羽毛/飞翔/攀爬/睡觉）与种类（虎皮/玄凤/牡丹/绿颊锥尾等 10 类），落库 `parrot_behavior_record`，并提供实时 WebSocket 异常行为检测（失踪/静止/拔羽/数量异常）。模型（`yolov8n.onnx` + `clip.pt` + `synset.txt` + `tokenizer.json`）已内置，开箱即跑。
- **🔥 Qwen-VL 多模态识别**：`POST /api/parrot/vision/vlm` 基于通义千问视觉模型，对 3D 虚拟笼舍绿颊锥尾做多模态行为识别，API Key 支持 `system_setting` 动态覆盖。
- **🔐 用户自配置 API Key（AES-256-GCM 加密）**：在「设置 → API Key 设置」中可视化配置 **通义千问（视觉识别）** 与 **DeepSeek（QQ 机器人）** 的密钥；保存时后端经 `ApiKeyEncryptor` 以 AES-256-GCM 加密（密文前缀 `ENC:`）落库 `system_setting`（`qwen_api_key` / `deepseek_api_key`），读取时自动解密并**脱敏**（仅保留前 3 后 4 位，中间 `****`）返回前端；库中的密钥优先级高于 `application.yml` 中的值，无需重启即可生效。详见 [用户 API Key 管理](#用户-api-key-管理--api-key-management)。
- **👥 QQ 白名单自助管理**：在「设置」中可视化维护 QQ 机器人可交互的 QQ 号白名单（逗号分隔），落库 `system_setting.qq_white_list`，**优先级高于 `application.yml` 的 `qq.onebot.allowed-users`**，可免重启收紧/放宽交互范围。详见 [QQ 白名单自助管理](#qq-白名单自助管理--qq-whitelist)。
- **🆕 QQ Agent 助手**：通过 NapCat（OneBot v11）接入 QQ，告警实时推送到手机；用户私聊即可查询实时数据、控制联动设备（二次确认防误操作）、咨询鹦鹉养护知识（MaxKB 兜底），支持每日环境晨报 / 宠物日报 / 设备离线提醒定时推送。详见 [QQ 机器人集成](#qq-机器人集成--qq-bot-integration)。

> 项目需求与第一阶段目标见 [docs/PROJECT_REQUIREMENTS.md](docs/PROJECT_REQUIREMENTS.md)。

---

## 技术栈 / Tech Stack

| 模块 | 路径 | 技术栈 | 端口 | 作用 |
|---|---|---|---|---|
| 后端 Backend | [backend/](backend/) | Java 17 · Spring Boot 3.3.5 · Maven · MySQL Connector/J · Redis · Spring Data JPA · WebSocket · SmartJavaAI vision 1.1.2 · Lombok · Validation | `8080` | 业务 API、数据入库、风险判断、告警生成 |
| 前端 Frontend | [frontend/](frontend/) | Vue ^3.5.0 · Vite ^7.0.0 · **axios** ^1.18.1（HTTP 调用）· **ECharts** ^6.1.0 · three.js ^0.185.1（3D 鹦鹉）· **vue-advanced-cropper** ^2.8.9（头像圆形裁剪），无 vue-router，图表为 SVG/Canvas 与 ECharts | `5173` (dev) | 宠物照护大屏、实时监控、成长报告 |
| 设备端·数据消费 | [device/getData/](device/getData/) | Java 8 · Spring Boot 2.3.5 · Spring Integration MQTT · JDBC · Hutool 5.8.16 | — | 订阅 `group23`，写入烟雾、温度、湿度表 |
| 设备端·控制转发 | [device/postData/](device/postData/) | Java 8 · Spring Boot 2.3.5 · Paho MQTT · JDBC | — | 读取 `device_control` 状态并转发到 MQTT 控制主题 |
| 设备端·数据模拟 | [device/simulate/](device/simulate/) | Java 8 · Spring Boot 2.3.5 · Paho MQTT | — | 每秒发布正态分布温湿度数据 |
| 设备端·MQTT 工具 | [device/MQTT/mqtt01-master/](device/MQTT/mqtt01-master/) | Java 8 · Spring Boot 2.3.5 · Paho MQTT · Web · Hutool 5.8.16 | `9091`（HTTP 服务；MQTT Broker 端口为 `1883`） | MQTT 收发工具 + REST API 控制设备 |

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
              │  解析 ppm / ℃ / %RH   │   │ 同步 buzzer/alarm_light/switch │
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
6. 告警触发后除写入 `alarm_record` 外，还通过 Spring 事件 `AlarmTriggeredEvent`（见 `AlarmEventListener`）解耦触发 **QQ 实时推送**（见 [QQ 机器人集成](#qq-机器人集成--qq-bot-integration)）。另设鹦鹉行为实时通道 `ParrotWebSocketHandler`（`/ws/parrot`），用于推送鹦鹉异常行为事件至前端。
7. `device/MQTT` 工具模块提供 REST 接口，用于向设备下发控制指令（开关蜂鸣器/报警灯/排风扇）；注意其默认控制主题为 `smoke/control`，与固件实际订阅主题不同，见 [设备端·MQTT 工具](#设备端mqtt-工具)。

### 鉴权与多用户数据隔离 / Auth & Data Isolation

系统采用**混合制**隔离策略：宠物数据与用户偏好按用户独立（需登录），设备/传感器数据全局共享（无需登录）。

#### 登录鉴权流程

1. 用户通过 `/api/auth/login`（账号密码）、`/api/auth/sms-code` + `/api/auth/sms-login`（手机验证码）、`/api/auth/register`（注册）登录；登录后可通过 `/api/auth/me` 获取资料、`/api/auth/change-password` 改密、`DELETE /api/auth/account` 注销。后端返回 token。
2. token 格式为 `smoke-token-{userId}-{expiresAt}-{uuid}`，前端存入 `localStorage.parrotAuthToken`，由 `frontend/src/api/request.js` 在每次请求的 `Authorization: Bearer` 头中自动携带。
3. 后端 `AuthInterceptor`（`backend/.../config/AuthInterceptor.java`）在请求开始时解析 token，取出用户 ID 写入线程上下文 `UserContext`（ThreadLocal），请求结束后自动清理；当前为宽松模式，**未登录不会拦截请求**，是否强制登录由具体业务决定。
4. `UserContext` 提供两个取值方法：`getCurrentUserId()`（未登录返回 null）、`requireUserId()`（未登录抛 401）。

#### 已隔离（按用户独立，需登录）

| 模块 | 接口 | 关键表 | 说明 |
|---|---|---|---|
| 宠物照护 | `/api/parrots/**` | `pet_profile` / `pet_weight_record` / `pet_medical_record` / `pet_ledger_record` / `pet_media_record` | 列表只返回当前用户的档案；按 `petId` 查询时校验归属，不属于当前用户的档案返回 404（`findByPetIdAndUserId`）；体重/病历/记账/照片通过 `requireProfile` 校验 `petId + userId` 归属后读写。 |
| 用户偏好 | `/api/user/preferences` | `user_preference` | 所有读写以 `userId` 隔离，按唯一键 `user_id + pref_key` 增量更新。 |

**源码位置**：`profileRepository` 新增 `findByUserIdAndEnabledTrueOrderByUpdatedAtDesc` / `findByPetIdAndUserId` / `existsByPetIdAndUserId`；5 个 Service 实现类（`PetProfileServiceImpl`、`PetWeightServiceImpl`、`PetMedicalRecordServiceImpl`、`PetLedgerRecordServiceImpl`、`PetPhotoServiceImpl`、`UserPreferenceServiceImpl`）均使用 `UserContext.requireUserId()`，原写死的 `DEFAULT_USER_ID = 1L` 已全部移除。

#### 全局共享（无需登录，按设备归属）

系统只有一个物理烟雾传感器，传感器数据为公共资源。以下接口与数据**不做用户隔离**，按 `device_id` 关联：

- 烟雾/温湿度数据（`smoke_data` / `temperature_data` / `humidity_data`）
- 设备状态与控制（`smoke_device` / `device_control`）
- 告警与时间线（`alarm_record` / `alarm_timeline`）
- AI 视觉复核 / 鹦鹉行为识别（`vision_check` / `parrot_behavior_record`）
- 阈值设置（`system_setting`）

> 关于"为什么烟雾数据不做隔离"：一个传感器同一时刻只能属于一个用户；若按用户隔离，其他登录用户将看不到任何烟雾数据。若未来需要"每个用户独立传感数据"，可通过 `device/simulate` 为每用户配独立虚拟设备来实现，此时按需扩展即可。当前 `smoke_device` 表**未**新增 `user_id` 列，无需改库。

---

## 仓库结构 / Repository Structure

```
Chinasoft-Project-group23/
├── backend/                      # 后端 Spring Boot 服务 (Java 17)
│   ├── pom.xml
│   ├── README.md                 # 后端说明
│   └── src/main/java/com/chinasoft/smokesensor/
│       ├── SmokeSensorApplication.java
│       ├── common/               # ApiResult / BusinessException / GlobalExceptionHandler / UserContext / CacheKeys
│       ├── config/               # *Properties 配置类（OneBot / Vision / Parrot / Llm / MaxKB / Redis / WebSocket）
│       ├── client/               # OneBotClient / LlmClient / MaxKBClient（外部服务调用）
│       ├── controller/           # 15 个控制器：Auth / Smoke / Environment / Alarm / Device / Devices / System / Runtime / Settings / Preferences / Parrots / Parrot / Vision / Sensor / QQ
│       ├── dto/                  # 请求/响应 DTO
│       ├── entity/               # Device / SensorData / AlarmRecord / DeviceControl / SystemSetting / VisionCheck / TemperatureData / HumidityData / Pet* / User* 等
│       ├── repository/           # JPA Repository
│       ├── service/              # 业务接口（含 qq/ parrot/ vision/ alarm/ 子包）
│       └── service/impl/         # 业务实现
├── frontend/                     # 前端 Vue 3 + Vite 宠物照护大屏
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── App.vue               # 主组件（宠物照护首页+详情路由、登录态守卫、用户设置）
│       ├── api/                  # API 调用层：request / auth / smoke / alarm / device / care / parrot / preferences / environment（业务模块均由 App.vue 按需调用）
│       ├── components/           # CurrentBirdCard / EntryCard / LedgerCharts / LoginView / MedicalCharts / MonitorCard / ParrotCage3D / ParrotVisual
│       ├── data/mockDashboard.js # mock 数据与业务配置（成长报告 mock 已停用）
│       ├── utils/                # markdown 解析等工具
│       ├── three/                # three.js 3D 鹦鹉场景/模型/状态机（ParrotCage3D 模块）
│       ├── public/tutorials/     # 饲养手册教程 Markdown 源文件
│       ├── styles.css            # 主题与组件样式（day-theme / night-theme）
│       └── main.js
├── device/                       # 设备端
│   ├── MQTT/mqtt01-master/       # MQTT 收发工具 + REST API（HTTP 端口 9091）
│   ├── getData/                  # MQTT 数据订阅与三类数据入库服务
│   ├── postData/                 # 数据库控制状态转发到 MQTT
│   └── simulate/                 # 温湿度正态分布 MQTT 模拟器
│       └── README.md
├── docs/                         # 项目需求文档
│   └── PROJECT_REQUIREMENTS.md
├── 文档/                          # API / 数据库设计文档（注：原「系统架构设计.md」已并入本 README 的[系统架构](#系统架构--architecture)章节）
│   ├── 智慧烟感API接口文档.md
│   └── 智慧烟感数据库表结构设计.md
├── smartjavaai-models/             # YOLO、CLIP 与配套词表模型文件
├── 交付/                           # 阶段交付物（含 3D鹦鹉模型代码-20260711 / .zip）
├── 知识库/                         # MaxKB 知识库（7 篇 Markdown）
├── 原型设计/                       # 原型图资源
├── group23-struct.sql              # 21 张基表的 DDL、索引与约束（不含视图）
├── group23-data.sql                # 从 dream28 导出的表数据快照（不含 DDL）
├── start-local.ps1 / start-local.bat # 本地开发链路启动脚本
├── 思维导图2.jpg                   # 项目思维导图
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
| MySQL | 8.0+（后端使用 connector-j 8.x；设备端模块仍用 mysql-connector-java 5.1.37，兼容 5.7/8.0） | 数据存储 |
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
# 启动后 HTTP 服务监听 http://localhost:9091（其 /on、/off 向 MQTT 控制主题 smoke/control 发指令）
```

---

## 开发指南 / Development Guide

### 后端 Backend

- **包根**：`com.chinasoft.smokesensor`
- **分层规范**：`controller`（仅接收请求/返回结果）→ `service/impl`（业务逻辑）→ `repository`（数据库操作）→ `entity`（表映射）→ `dto`（请求/响应载体）。业务逻辑**不得**写在 Controller 中。
- **统一响应**：所有接口返回 [common/ApiResult.java](backend/src/main/java/com/chinasoft/smokesensor/common/ApiResult.java) 包装的 `{code, message, data}` 结构。
- **异常处理**：业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一捕获。
- **数据库**：`spring.jpa.hibernate.ddl-auto: none`，**不自动建表**。初始化空库时先执行根目录 [group23-struct.sql](group23-struct.sql)，再执行 [group23-data.sql](group23-data.sql)；字段与索引设计见 [文档/智慧烟感数据库表结构设计.md](文档/智慧烟感数据库表结构设计.md)。
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
- **设备在线判定**：后端 `DeviceController` 通过 Redis 键（如 `qq:device:online:SMK-001`，由定时任务/心跳维护）判断 `smoke_device` 在线状态，而非单纯依赖 `last_heartbeat` 字段；QQ 离线提醒同样读此键的在线翻转。
- ⚠️ **文档与实现的轮询差**：README 多处提及「每 3 秒轮询」，但前端实际为 `MonitorCard` 每 **500ms** 调用 `/api/smoke/realtime`、`App.vue` 大屏/照护每 **5000ms** 轮询；后端无 3 秒定时。

### 用户 API Key 管理 / API Key Management

> 提交 `a101dd9` 新增：用户可在前端「设置 → API Key 设置」自助配置第三方密钥，后端加密存储。

- **加密机制**：`ApiKeyEncryptor`（`backend/.../config/ApiKeyEncryptor.java`）使用 **AES-256-GCM**（IV 12 字节 + 128 位 Tag），密钥由配置项 `app.api-key-secret` 经 SHA-256 派生；密文以 `ENC:` 前缀标识，非 `ENC:` 的值按明文兼容（向后兼容旧数据）。
- **存储与优先级**：密钥存于 `system_setting` 表（`qwen_api_key` 通义千问视觉 / `deepseek_api_key` DeepSeek 大模型），`setting_group='keys'`；**库中密钥一旦设置即覆盖 `application.yml` 中的 `qwen.vision.api-key` / `qq.llm.api-key`**，且调用时实时解密，无需重启。
- **读取脱敏**：`GET /api/settings/api-keys` 返回 `ApiKeysResponse`（仅 `qwenApiKey` / `deepseekApiKey` 两个脱敏字段，如 `sk-****a1b2`），不泄露完整密钥。
- **保存逻辑**：`POST /api/settings/api-keys`（`ApiKeysRequest`：`qwenApiKey` / `deepseekApiKey` 可选），后端对每个非空值：含 `*` 视为「未修改、保留原值」，否则用 `ApiKeyEncryptor.encrypt` 加密后 `saveOrUpdateSetting`；传空字符串则清空该键。
- **密钥注入**：`app.api-key-secret` 默认 `parrot-care-default-secret-2026`，**生产务必通过环境变量 `API_KEY_SECRET` 注入自定义值**（注意 `backend/.env.example` 当前未包含此变量，需自行补充）。
- **前端交互**：`App.vue` 的 `openApiKeysModal` 拉取脱敏值回填，`saveApiKeys` 调 `POST /settings/api-keys`；支持中/英/西/日多语言文案，保存成功/失败有提示。

### QQ 白名单自助管理 / QQ Whitelist

> 工作区未提交改动新增：管理员可在前端「设置」中自助维护 QQ 机器人可交互的 QQ 号白名单。

- **接口**：`GET /api/settings/qq-whitelist`（返回 `QqWhitelistResponse`）、`POST /api/settings/qq-whitelist`（`QqWhitelistRequest`：`qqWhitelist` 为逗号分隔的 QQ 号字符串）。
- **存储**：落库 `system_setting`（`setting_key='qq_white_list'`，`setting_group='keys'`）；空字符串表示清空。
- **判定优先级**：`OneBotMessageRouter.isAllowed(userId)` 先查数据库 `qq_white_list`——非空则仅放行列表中的 QQ 号；**数据库为空时回退到 `application.yml` 的 `qq.onebot.allowed-users`**；两者皆空则放行所有人（调试友好）。因此数据库白名单优先级高于配置文件，可无需重启即可收紧/放宽交互范围。
- **前端交互**：`App.vue` 的 `qqWhitelist` 弹窗拉取当前白名单回填、保存时调 `POST /settings/qq-whitelist`，中/英/西/日多语言文案。

### 前端 Frontend

- **开发端口**：`5173`，已配置 `--host 0.0.0.0` 供局域网访问。
- **HTTP 客户端**：使用 `axios`（见 `src/api/request.js` 封装的 `request()`，统一注入 `Authorization: Bearer` 与 `ApiResult` 解包）。无 `vue-router`，页面切换由 `App.vue` 中的 `detailViews` 字符串路由 + 登录态守卫实现。
- **主题**：在 [src/styles.css](frontend/src/styles.css) 中定义 `day-theme` / `night-theme` 两套，由 `App.vue` 的 `APP_THEME` 通过 `:class` 绑定到 `<main class="app-shell">` 切换，并尊重用户偏好 `systemPrefs.theme`。
- **数据轮询**：实时监控卡片 `MonitorCard` 通过 `refreshRealtime` 每 **500ms** 调用 `/api/smoke/realtime`；`App.vue` 大屏/照护轮询为每 **5000ms**（非 3 秒）。
- **API 调用层**：[src/api/](frontend/src/api/) 下 `smoke.js`（实时/历史）、`care.js`（宠物档案/体重/病历/账本/相片，已接入 `App.vue`）、`parrot.js`、`environment.js`（`getEnvironmentReport` 接通成长报告真实环境数据）、`device.js`（仅 `listDevices` 接入，单设备控制/状态接口尚未接入组件）、`auth.js`、`preferences.js`、`alarm.js` 均已封装。
- **3D 鹦鹉模块**：`src/three/`（three.js 场景/模型/状态机）+ `src/components/ParrotCage3D.vue`（含 `useParrotVision` 调用 `/api/parrot/vision/vlm`、`ParrotAbnormalDetector` 异常行为判断）。注意 `src/components/parrot3d/` 目录为空，请勿与 3D 模块混淆。
- **头像上传/裁剪**：「设置」中选图后由 `vue-advanced-cropper`（`CircleStencil` 圆形裁剪）裁剪，导出 base64 data URI 经 `PUT /auth/me` 的 `avatarImage` 字段保存到 `sys_user.avatar_image`；登录态与设置页均从 `GET /auth/me` 拉取并在 `localStorage.parrotUserAvatar` 缓存。
- **成长报告**：已接通后端 `/api/environment/report` 真实环境历史（温/湿/粉尘），`mockDashboard.js` 中的 `reportStats`/`reportCurveSets` 已清空并标注「不再使用 mock」。
- **mock 数据与业务配置**：[src/data/mockDashboard.js](frontend/src/data/mockDashboard.js) 中定义宠物、入口卡片、医疗模块、饲养手册等配置（成长报告相关 mock 已停用）。

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

- **HTTP 服务端口**：`9091`（MQTT Broker 端口为 `1883`）
- **REST 接口**：
  - `GET /publishTopic?sendMessage=xxx` — 向默认主题 `group23` 发布消息
  - `GET /on` · `POST /on` — 向控制主题 `smoke/control` 发送 `1`（开启联动设备）
  - `GET /off` · `POST /off` — 向控制主题 `smoke/control` 发送 `0`（关闭联动设备）
  - `POST /login` — 登录接口

> ⚠️ 该工具默认控制主题为 `smoke/control`，与设备固件实际订阅的 `group23-s-to-h` 不同，因此 `/on`、`/off` 不会真正驱动硬件；控制硬件请走后端 `POST /api/device/control`（经 `device/postData` 转发到 `group23-s-to-h`）。

---

## 外部服务与凭据 / External Services

> ⚠️ **以下为开发环境凭据，已在各文档与配置中暴露，仅供开发联调使用，请勿用于生产环境。生产部署请通过环境变量注入。**
>
> 密钥（NapCat / MaxKB / DeepSeek）请通过 [backend/.env.example](backend/.env.example) 复制为 `.env` 以环境变量注入（当前 `.env.example` 含 `ONEBOT_ACCESS_TOKEN` / `MAXKB_APP_ID` / `MAXKB_API_KEY` / `DEEPSEEK_API_KEY`；**注意 `application.yml` 还引用了 `QWEN_API_KEY` 与 `API_KEY_SECRET`，二者尚未写入 `.env.example`**，请勿硬编码到配置中；其中 `API_KEY_SECRET` 用于派生 AES-256-GCM 密钥保护用户自配置的 API Key）。`ddl-auto: none`，21 张基表由 `group23-struct.sql` 维护，数据快照独立保存于 `group23-data.sql`。
>
> ⚠️ 当前 `application.yml` 中 `qq.onebot.allowed-users` / `push-target-user` 写入了真实 QQ 号、`qq.maxkb.base-url` 为内网地址，属开发联调配置；生产部署请改为环境变量注入或移除。

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

## QQ 机器人集成 / QQ Bot Integration

> **创新点**：打通「设备 -> 后端 -> QQ」闭环，告警秒级触达用户手机；用户通过 QQ 私聊即可查询数据、控制设备、咨询养护知识，对标 openclaw 式 agent 助手。

### 架构

```
用户QQ ──> QQ服务器 ──> NapCatQQ(本地:3000) ──HTTP POST上报──> 后端 /api/qq/callback
                                                                │ (意图识别: 规则 -> MaxKB兜底)
用户QQ <──发消息── NapCatQQ <──HTTP API调用── 后端 (查询/控制/问答)
```

- **接入层**：NapCatQQ（OneBot v11 协议），本地部署登录小号，HTTP POST 上报 + HTTP API 收发，无需公网回调 / 加解密
- **Agent 逻辑**：DeepSeek function calling agent（自然语言理解 + 工具调用，启用时优先）+ 规则意图回退（关键词/控制）+ MaxKB 兜底（三层降级，实际顺序为 `LLM agent → 规则 → MaxKB`）
- **推送通道**：Spring 事件 `AlarmTriggeredEvent` 解耦告警触发（不侵入业务代码）+ 3 个定时任务

### 支持的指令

| 类别 | 示例 | 说明 |
|---|---|---|
| 查询 | 状态 / 实时 / 在线吗 / 告警 / 最近告警 | 实时浓度温湿度、设备状态、告警统计与列表 |
| 控制 | 开蜂鸣器 / 关报警灯 / 开总开关 | 触发二次确认，回复“确认”执行（60秒内有效） |
| 问答 | 鹦鹉能吃辣椒吗 / 烟雾超标怎么办 | MaxKB 智能问答（规则未命中时兜底） |
| 帮助 | 帮助 | 显示指令菜单 |

### 推送场景

- **告警实时推送**：烟雾超标时秒级推送到 QQ（事件驱动，告警入库事务提交后触发）
- **每日环境晨报**：每天 8:30 推送当前浓度温湿度 + 今日告警统计
- **宠物成长日报**：每天 9:00 推送宠物档案摘要
- **设备离线提醒**：每 60s 检测，在线状态翻转时推送（避免重复刷屏）

### 部署步骤

1. 本机安装 QQNT 桌面客户端 + [NapCatQQ](https://github.com/NapNeko/NapCatQQ)
2. 启动 NapCat，用**小号**扫码登录（规避风控）
3. NapCat WebUI 配置：开启 HTTP 服务（端口 3000）+ HTTP POST 上报（地址 `http://127.0.0.1:8080/api/qq/callback`）+ 设置 access_token
4. `backend/src/main/resources/application.yml` 的 `qq.onebot` 配置：`enabled: true`、`access-token`、`push-target-user`（小号 QQ）、`allowed-users`（测试用户 QQ）
5. 启动后端，用测试 QQ 加小号为好友，私聊发「状态」验证

### 配置项

| 配置 | 说明 |
|---|---|
| `qq.onebot.enabled` | 是否启用 QQ 机器人（**当前 `application.yml` 默认 `true`**，未部署 NapCat 时建议改 `false`，否则启动会尝试连接并告警） |
| `qq.onebot.base-url` | NapCat HTTP API 地址（默认 `http://localhost:3000`） |
| `qq.onebot.access-token` | NapCat 鉴权 token（Bearer） |
| `qq.onebot.allowed-users` | 交互白名单 QQ 号列表（为空时放行所有人，生产建议配置；当前 `application.yml` 已写入真实 QQ 号） |
| `qq.onebot.push-target-user` | 主动推送目标 QQ 号（当前 `application.yml` 已写入真实 QQ 号） |
| `qq.maxkb.*` | MaxKB 智能问答配置（`enabled` 当前默认 `true`，`base-url` 当前为内网地址） |
| `qq.llm.enabled` | 是否启用 DeepSeek function calling agent（**当前 `application.yml` 默认 `true`**）。须 `true` 且环境变量 `DEEPSEEK_API_KEY` 已设置，二者缺一不可；否则静默降级为「规则 → MaxKB」模式，DeepSeek 不会被调用 |
| `qq.llm.base-url` | DeepSeek API 地址（OpenAI 兼容，默认 `https://api.deepseek.com`） |
| `qq.llm.api-key` | DeepSeek API Key，**通过环境变量 `DEEPSEEK_API_KEY` 注入**，勿硬编码到配置 |
| `qq.llm.model` | 模型名，须填 DeepSeek 官方支持的模型；填不存在的模型名会导致调用失败 |
| `qq.llm.max-rounds` | function calling 最大轮数（默认 5，防止无限循环） |

> **三层降级与启用条件**：消息处理顺序为 `LLM agent → 规则（关键词/控制意图）→ MaxKB 兜底`。LLM 仅在 `qq.llm.enabled=true` 且 `DEEPSEEK_API_KEY` 非空时才真正发起请求；未启用时 `OneBotMessageRouter` 会自动跳过 agent，直接走规则 / MaxKB 兜底，**不会产生任何 DeepSeek 请求**。排查「没配置却调用了 DeepSeek」时，先确认 `qq.llm.enabled` 是否被改成 `true`、以及环境变量 `DEEPSEEK_API_KEY` 是否已设置。
>
> ⚠️ **当前 `application.yml` 各集成默认开启**：`qq.onebot.enabled=true`、`qq.maxkb.enabled=true`、`qq.llm.enabled=true`、`qwen.vision.enabled=true`（与旧版「默认 false」不同，未部署 NapCat/未配密钥时可能启动告警或调用失败，请按需改为 `false`）。同时 `qq.llm.model`（`deepseek-v4-flash`）与 `qwen.vision.model`（`qwen3.5-flash`）并非对应服务商的真实模型 ID（DeepSeek 实为 `deepseek-chat`/`deepseek-reasoner`，DashScope 为 `qwen-vl-plus`/`qwen-vl-max` 等），**须改为真实模型名否则调用会失败**——README 此处仅为文档说明，实际值以 `application.yml` 为准并请自行修正。此外 `allowed-users` 与 `push-target-user` 为真实 QQ 号、`maxkb.base-url` 为内网地址，已直接写于配置中（非纯环境变量注入）。

### 相关代码

| 模块 | 文件 |
|---|---|
| 接入客户端 | [OneBotClient.java](backend/src/main/java/com/chinasoft/smokesensor/client/OneBotClient.java) · [MaxKBClient.java](backend/src/main/java/com/chinasoft/smokesensor/client/MaxKBClient.java) |
| 回调入口 | [OneBotCallbackController.java](backend/src/main/java/com/chinasoft/smokesensor/controller/OneBotCallbackController.java) |
| Agent 核心 | [OneBotMessageRouter.java](backend/src/main/java/com/chinasoft/smokesensor/service/qq/OneBotMessageRouter.java) · [OneBotControlService.java](backend/src/main/java/com/chinasoft/smokesensor/service/qq/OneBotControlService.java) |
| 推送与定时 | [OneBotPushService.java](backend/src/main/java/com/chinasoft/smokesensor/service/qq/OneBotPushService.java) · [OneBotPushScheduler.java](backend/src/main/java/com/chinasoft/smokesensor/service/qq/OneBotPushScheduler.java) |
| 事件驱动 | [AlarmTriggeredEvent.java](backend/src/main/java/com/chinasoft/smokesensor/service/alarm/AlarmTriggeredEvent.java) · [AlarmEventListener.java](backend/src/main/java/com/chinasoft/smokesensor/service/alarm/AlarmEventListener.java) |

---

## 数据库设计 / Database Design

当前远程库 `dream28` 使用 MySQL 5.7.44、`utf8mb4` 与 InnoDB，包含 **21 张基表 + 2 个视图**：`dataSize`、`threeDataCounter`。详细字段、索引与逻辑关系见 [文档/智慧烟感数据库表结构设计.md](文档/智慧烟感数据库表结构设计.md)。

### 结构与数据备份

为避免结构脚本和大体量传感器历史数据混在同一文件中，数据库备份已拆分为两个文件：

| 文件 | 内容 | 导入顺序与注意事项 |
|---|---|---|
| [group23-struct.sql](group23-struct.sql) | 21 张基表的 `CREATE TABLE`、索引和约束；不含表数据。 | 在空数据库中**先执行**。文件按“仅表结构”约定，不包含两个视图。 |
| [group23-data.sql](group23-data.sql) | 从远程 `dream28` 以一致性事务导出的 21 张表数据；不含 DDL。 | 在结构导入成功后执行。包含真实业务快照，可能含账户密码哈希、头像 Base64、加密配置和传感器历史数据。 |

因此，完整恢复顺序为：**导入 `group23-struct.sql` → 导入 `group23-data.sql` → 如业务依赖视图，再单独维护 `dataSize` 与 `threeDataCounter` 的定义**。结构与数据不再合并在单个 SQL 文件中。

| # | 表名 | 中文名 | 说明 |
|---|---|---|---|
| 1 | `sys_user` | 用户表 | 登录账号与权限（admin / viewer）；含 `avatar_image`（`LONGTEXT`，base64 头像，由 `文档/screenshot_storage_alter.sql` 追加） |
| 2 | `user_preference` | 用户偏好表 | 主题、字体、字号、语言、通知开关等 |
| 3 | `pet_cage` | 宠物笼舍表 | 笼舍/监测区域，绑定监测设备 |
| 4 | `smoke_device` | 烟感设备表 | 设备基本信息与当前状态（含冗余的最新浓度字段） |
| 5 | `device_control` | 联动设备表 | 蜂鸣器/报警灯/排风扇/空气净化器状态与自动联动标识 |
| 6 | `system_setting` | 系统设置表 | KV 形式存储阈值、心跳超时、环境舒适区间、第三方 API Key 等全局配置 |
| 7 | `smoke_data` | 烟雾/粉尘数据表 | 历史浓度数据，量最大，按设备+时间索引 |
| 8 | `temperature_data` | 温度数据表 | 温度历史数据，单位 ℃，按设备+时间索引 |
| 9 | `humidity_data` | 湿度数据表 | 相对湿度历史数据，单位 %RH，按设备+时间索引 |
| 10 | `alarm_record` | 告警记录表 | 告警事件主表，状态 pending→processing→resolved |
| 11 | `alarm_timeline` | 告警时间线表 | 告警生命周期事件（触发/联动/处理/恢复） |
| 12 | `vision_check` | 视觉复核表 | AI 摄像头复核结果（YOLO 火焰/烟雾检测，需自备模型） |
| 12a | `parrot_behavior_record` | 鹦鹉行为识别表 | AI 鹦鹉种类+行为识别历史（🟢 完整实现，模型已就位） |
| 13 | `pet_profile` | 宠物档案表 | 宠物资料及所属用户、笼舍关联 |
| 14 | `pet_weight_record` | 宠物体重记录表 | 历史体重与测量时间，用于体重趋势 |
| 15 | `pet_daily_report` | 宠物成长日报表 | 健康评分、睡眠、鸣叫、进食、排泄指标 |
| 16 | `pet_media_record` | 宠物媒体记录表 | 照片、截图、录音、视频记录（成长相册） |
| 17 | `pet_medical_record` | 宠物病历记录表 | 就诊、用药、症状与复查记录 |
| 18 | `pet_ledger_record` | 宠物记账记录表 | 按宠物记录饲养支出，支持日期和标签查询 |
| 19 | `food_safety_query` | 食物安全查询表 | 食物可食用性查询历史 |
| 20 | `environment_report_hourly` | 环境小时报表 | 温/湿/粉尘按小时聚合，支撑成长报告与晨报（🟢 已落地） |

> **图例**：🟢 有完整后端实现 | 🔴 仅有建表 SQL / 无后端接口 | ⚪ 概念表

**核心关系**：`pet_cage` 1:1 `smoke_device`，1:N `pet_profile` / `alarm_record` / `pet_media_record`；`smoke_device` 1:N `smoke_data` / `temperature_data` / `humidity_data` / `alarm_record` / `device_control` / `parrot_behavior_record`；`alarm_record` 1:N `alarm_timeline` / `vision_check`；`sys_user` 1:N `pet_profile` / `user_preference` / `pet_ledger_record` / `food_safety_query`；`pet_profile` 1:N `pet_weight_record` / `pet_daily_report` / `pet_media_record` / `pet_medical_record` / `pet_ledger_record`。以上均为逻辑关联，不创建数据库外键。

---

## API 接口 / API Reference

后端 BaseURL：`http://<服务器IP>:8080/api`，统一返回 `{code, message, data}`。涵盖鉴权、烟感、告警、设备、环境、宠物照护、AI 识别、QQ 机器人等模块；当前代码共有 **68 个 HTTP 请求映射**（15 个 `@RestController`）。完整字段与错误码见 [文档/智慧烟感API接口文档.md](文档/智慧烟感API接口文档.md)，接口可用性以 Controller 源码为准。

> 🔌 **实时通道**：除 HTTP 外，鹦鹉行为识别提供 WebSocket 实时流（`/ws/parrot`，`ParrotWebSocketHandler`），用于推送 MISSING/STATIC/PLUCKING/COUNT 等异常行为事件；告警经 Spring 事件 `AlarmTriggeredEvent` 由后端主动推送到 QQ（见 [QQ 机器人集成](#qq-机器人集成--qq-bot-integration)），前端告警不依赖轮询。

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 鉴权 Auth | POST | `/auth/login` | 账号密码登录，返回 token |
| 鉴权 Auth | POST | `/auth/sms-code` | 发送短信验证码 |
| 鉴权 Auth | POST | `/auth/sms-login` | 手机验证码登录 |
| 鉴权 Auth | POST | `/auth/register` | 注册账号 |
| 鉴权 Auth | GET | `/auth/me` | 获取当前用户资料（含 `avatarImage` 头像） |
| 鉴权 Auth | PUT | `/auth/me` | 更新用户资料（可选 `avatarImage` base64 头像，空表示不修改） |
| 鉴权 Auth | POST | `/auth/change-password` | 修改密码 |
| 鉴权 Auth | DELETE | `/auth/account` | 注销账号 |
| 系统 System | GET | `/system/status` | 系统在线状态、当前时间、在线设备数 |
| 运行态 Runtime | GET | `/runtime/link-snapshot` | 连接快照，用于页面初始化 |
| 烟雾数据 Smoke | GET | `/smoke/latest` | 最新浓度、风险等级、报警状态 |
| 烟雾数据 Smoke | GET | `/smoke/realtime` | 实时浓度 + 温度 + 湿度 |
| 烟雾数据 Smoke | GET | `/smoke/history` | 历史浓度趋势（范围/起止/来源筛选） |
| 烟雾数据 Smoke | POST | `/smoke/simulate` | 模拟烟雾/粉尘升高（课堂演示） |
| 烟雾数据 Smoke | POST | `/smoke/restore` | 恢复正常环境，解除告警 |
| 传感器 Sensor | POST | `/sensor/upload` | 硬件上传烟雾数据 |
| 环境 Environment | GET | `/environment/history` | 温/湿/粉尘原始时序（按 range 或起止时间） |
| 环境 Environment | GET | `/environment/hourly` | 按小时聚合报表（支撑成长报告） |
| 环境 Environment | GET | `/environment/report` | 日报 / 周报 / 月报自然环境数据（`environment_report_hourly` 落库） |
| 告警 Alarm | GET | `/alarm/stat/today` | 今日告警次数与较昨日变化 |
| 告警 Alarm | GET | `/alarm/logs` | 告警记录列表（分页/筛选） |
| 告警 Alarm | POST | `/alarm/handle` | 处理告警，填写处理人备注 |
| 设备 Device | GET | `/device/status` | 设备在线状态与各受控设备开关 |
| 设备 Device | GET | `/device/info` | 单设备详情 |
| 设备 Device | POST | `/device/control` | 控制蜂鸣器/报警灯/排风扇开关 |
| 设备管理 Devices | GET / POST | `/devices` | 设备列表（筛选/搜索）/ 新增设备 |
| 设备管理 Devices | PUT / DELETE | `/devices/{deviceId}` | 编辑 / 解绑设备 |
| 系统设置 Settings | GET / POST | `/settings/threshold` | 读取 / 保存风险阈值 |
| 系统设置 Settings | GET / POST | `/settings/api-keys` | 读取 / 保存第三方 API Key（Qwen / DeepSeek）；读取返回脱敏值，保存以 AES-256-GCM 加密落库，覆盖 `application.yml` |
| 系统设置 Settings | GET / POST | `/settings/qq-whitelist` | 读取 / 保存 QQ 机器人聊天白名单（逗号分隔的 QQ 号，存 `system_setting.qq_white_list`，优先级高于 `application.yml` 的 `allowed-users`） |
| 用户偏好 Preferences | GET / PUT | `/user/preferences` | 读取 / 保存用户偏好（主题、通知等） |
| 宠物照护 Parrots | GET / POST | `/parrots` | 宠物档案列表 / 新增 |
| 宠物照护 Parrots | GET / PUT / DELETE | `/parrots/{petId}` | 单宠物档案查询 / 更新 / 删除 |
| 宠物照护 Parrots | GET / POST / PUT | `/parrots/{petId}/weights[/{id}]` | 体重记录查询 / 新增 / 更新 |
| 宠物照护 Parrots | GET / POST / PUT / DELETE | `/parrots/{petId}/medical-records[/{recordId}]` | 病历记录增删改查 |
| 宠物照护 Parrots | GET / POST / PUT / DELETE | `/parrots/{petId}/ledger-records[/{ledgerId}]` | 记账记录增删改查 |
| 宠物照护 Parrots | GET / POST / DELETE | `/parrots/{petId}/photos[/{mediaId}]` | 相片记录查询 / 新增 / 删除 |
| 宠物照护 Parrots | GET / POST / DELETE | `/parrots/{petId}/recordings[/{mediaId}]` | 录音记录查询 / 新增 / 删除 |
| 鹦鹉识别 Parrot | GET | `/parrot/behavior` | 配置快照（模型启用状态、行为/种类清单） |
| 鹦鹉识别 Parrot | POST | `/parrot/behavior` | 上传截图 → YOLO 检测 bird + CLIP 行为/种类分类（落库） |
| 鹦鹉识别 Parrot | POST | `/parrot/vision/vlm` | Qwen-VL 多模态识别（base64 图 + hint，3D 虚拟笼舍场景） |
| 鹦鹉识别 Parrot | GET | `/parrot/behavior/today-stats` | 当日各行为分组计数统计 |
| 鹦鹉识别 Parrot | GET | `/parrot/behavior/stats` | 指定时间范围内的行为统计 |
| 鹦鹉识别 Parrot | POST | `/parrot/chat` | 鹦鹉养护与行为相关对话入口 |
| 视觉复核 Vision | GET | `/vision/check` | AI 摄像头截图与识别结果（火焰/烟雾 YOLO 复核） |
| QQ 机器人 QQ | POST | `/qq/callback` | NapCat（OneBot v11）HTTP POST 上报入口 |
| QQ 机器人 QQ | GET | `/qq/test/send` | 主动推送测试 |

> ℹ️ **截至 2026-07-12 后端实现状态**：核心 P0 接口（系统状态、烟雾数据/历史/模拟/恢复、传感器上传、告警日志/统计、设备状态/信息）已全部落地；**鉴权全套 8 个端点**（含 `PUT /auth/me` 资料更新）均已实现；**宠物照护 `/parrots/**` 完整实现 22 个端点**（档案、体重、病历、账本、相片、录音；病历与账本均含 DELETE），前端 `src/api/care.js` 已接入 `App.vue` 真实调用打通；**环境报表三端点**（`/environment/history` `/hourly` `/report`）已落地并支撑成长报告；QQ 回调 `/qq/callback`、视觉复核 `/vision/check`、鹦鹉识别 `/parrot/behavior` 及其 `vlm`/`today-stats`/`stats`/`chat` 子端点均已落地。详见 [开发进度](#开发进度--project-status) 与 [API 文档](文档/智慧烟感API接口文档.md)。

---

## 部署说明 / Deployment

当前项目以**本地开发模式**运行，暂不使用容器化或 K8s 部署。开发时推荐用 [`start-local.ps1`](#一键启动本地开发链路--one-click-local-dev-chain)（或双击 `start-local.bat`）一键拉起 `getData + simulate + backend`；前端仍单独进入 `frontend/` 执行 `npm run dev`。如需单独调试某个模块，可参见 [各模块启动](#各模块启动--start-each-module) 章节手动进入对应目录执行。

> 如后续需要容器化部署，可自行编写前后端 Dockerfile 并配置 docker-compose 或编排工具。

---

## 开发进度 / Project Status

> 如实反映截至 2026-07-11 的开发状态，供团队成员与答辩参考。

| 模块 | 状态 | 说明 |
|---|---|---|
| 后端·骨架 | ✅ 已完成 | entity / repository / service / dto / ApiResult / 全局异常处理 |
| 后端·Controller | ✅ 已完成 | 共 15 个 `@RestController`、68 个请求映射；核心 P0 接口（系统状态、烟雾数据/历史/模拟/恢复、传感器上传、告警日志/统计、设备状态/信息）已落地；**环境报表 3 端点**（`/environment/history` `/hourly` `/report`）已落地；鹦鹉照护 `22` 个 `/parrots/**` 端点已落地（含录音、病历/账本 DELETE）；鉴权 8 端点（含 `PUT /auth/me`）已完成；**系统设置包含 `/settings/api-keys`（AES-256-GCM 加解密）与 `/settings/qq-whitelist`（自助白名单）端点**；QQ 回调 `/qq/callback`、视觉复核 `/vision/check`、鹦鹉识别 `/parrot/behavior`（含 `vlm`/`today-stats`/`stats`/`chat`）已落地；剩余：告警详情 `alarm/{id}` 后端接口、`pet_cage`/`alarm_timeline` 子资源接口 |
| 前端 | ✅ 已完成 | 宠物智能照护大屏，含实时监控、环境指标、宠物档案、成长报告、医疗助手、记账本、饲养手册、3D 鹦鹉等模块；登录/注册/短信登录/改密/注销落地，设置页由 `GET /auth/me` 驱动真实用户资料；`care.js`/`parrot.js`/`environment.js` 已接入 `App.vue`，**成长报告已接通后端 `/api/environment/report` 真实环境数据**（mock 已停用）；3D 鹦鹉 `ParrotCage3D` + `useParrotVision`（调 `/api/parrot/vision/vlm`）+ 异常行为引擎 |
| 设备端·getData | ✅ 已完成 | MQTT 订阅 → 三类消息解析 → 分流写入三张数据表，含单元测试 |
| 设备端·postData | ✅ 已完成 | 读取 `device_control` 状态变化并转发到 `group23-s-to-h` |
| 设备端·simulate | ✅ 已完成 | 每秒发布限定范围内的正态分布温湿度数据 |
| 设备端·MQTT 工具 | ✅ 已完成 | 收发消息 + REST API（`/publishTopic` `/on` `/off` `/login`，HTTP 端口 9091） |
| 数据库表 | ✅ 已完成 | 21 张基表 + `dataSize` / `threeDataCounter` 两个视图；表结构与实时数据已拆分为 `group23-struct.sql`、`group23-data.sql`。`sys_user` / `user_preference` / `pet_profile` 全系 / `environment_report_hourly` 等均已落地后端接口；`pet_cage` / `alarm_timeline` / `pet_daily_report` / `food_safety_query` 仅有建表 SQL 暂无独立后端接口 |
| 温湿度数据链路 | ✅ 已完成 | MQTT 模拟、解析、入库、后端查询 `/smoke/realtime` 返回真实温湿度均已完成 |
| SmartJavaAI 视觉复核 | 🟡 已实现·待模型 | `/api/vision/check` YOLO 火焰/烟雾复核代码已接入 SmartJavaAI；`smartjavaai.vision.enabled=false` 且未配自定义 YOLO 模型，**默认不运行**（返回 5001），需自备火焰/烟雾模型后开启 |
| 鹦鹉行为识别 | 🟢 已完整运行 | `/api/parrot/behavior` 已实现：截图 → YOLO 检测 bird → 裁剪 → CLIP 零样本行为/种类分类 → 落库；**模型二进制 `yolov8n.onnx` + `clip.pt` + `synset.txt` + `tokenizer.json` 已就位于 `smartjavaai-models/`，`parrot.detection`/`parrot.clip` 均为 `enabled=true`，开箱即可运行** |
| Qwen-VL 多模态识别 | 🟢 已实现 | `POST /api/parrot/vision/vlm`（QwenVisionClient，DashScope 兼容模式）对 3D 虚拟笼舍绿颊锥尾做多模态识别；API Key 可由 `system_setting` 的 `qwen_api_key` 覆盖（用户可在设置页自助配置并 AES 加密存储）；`qwen.vision.enabled=true` |
| 用户 API Key 管理 | 🟢 已实现 | 设置页可视化配置 Qwen / DeepSeek 密钥；后端 `ApiKeyEncryptor`（AES-256-GCM）加密落库 `system_setting`，读取脱敏返回，库中值覆盖 `application.yml`；密钥 `app.api-key-secret` 默认占位，**生产需用 `API_KEY_SECRET` 覆盖** |
| QQ 白名单自助管理 | 🟢 已实现 | 设置页可视化维护 `qq.onebot.allowed-users`（逗号分隔 QQ 号），落库 `system_setting.qq_white_list`，**优先级高于 `application.yml`**，可免重启调整交互范围 |
| 用户头像上传 | 🟢 已实现 | 设置页选图经 `vue-advanced-cropper` 圆形裁剪 → base64 经 `PUT /auth/me` 存 `sys_user.avatar_image`（`LONGTEXT`），登录态与设置页从 `GET /auth/me` 读取并缓存 `localStorage.parrotUserAvatar` |

**下一步 TODO**：

1. 补全告警详情 `alarm/{id}` 后端接口。
2. 落地 `pet_cage` / `alarm_timeline` 子资源接口（已有建表 SQL）。
3. SmartJavaAI 火焰/烟雾视觉复核：补充自定义 YOLO 模型并置 `smartjavaai.vision.enabled=true` + `model-path`。
4. 修正 `application.yml` 中的真实模型 ID（`qq.llm.model`、`qwen.vision.model`）与按需关闭未部署集成（`qq.onebot`/`qq.llm` 等默认 `true` 会启用外部调用）。

> 已落地能力速览（供答辩）：Ai 视觉鹦鹉识别全流程可跑；Qwen-VL 多模态识别；QQ 机器人（LLM 意图识别 + 规则 + MaxKB 兜底 + 二次确认控制 + 告警/晨报/日报/离线定时推送）；环境小时报表落库支撑成长报告；多用户数据隔离（宠物/偏好按用户、传感/设备/告警全局共享）。

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

- [docs/PROJECT_REQUIREMENTS.md](docs/PROJECT_REQUIREMENTS.md) — 后端项目需求与第一阶段目标
- [文档/智慧烟感API接口文档.md](文档/智慧烟感API接口文档.md) — 接口字段与错误码说明（当前 Controller 源码共 68 个请求映射）
- [文档/智慧烟感数据库表结构设计.md](文档/智慧烟感数据库表结构设计.md) — 字段、索引与逻辑关系设计
- [group23-struct.sql](group23-struct.sql) — 21 张基表的结构脚本
- [group23-data.sql](group23-data.sql) — 与结构脚本分离的数据快照
- [知识库/](知识库/) — MaxKB 知识库（7 篇 Markdown）：`parrot-knowledge-base`（鹦鹉养护）/`告警应急处理`/`季节养护专题`/`笼舍清洁与消毒`/`系统使用指南`/`鹦鹉急救指南`/`MaxKB数据库工具提示词`
- [docs/LOGIN_API.md](docs/LOGIN_API.md) — 登录 / 注册 / 短信验证码 / `/auth/me` 接口对接说明（`account` 字段约定、token 格式、演示环境验证码日志）

### 子模块 README

- [backend/README.md](backend/README.md) — 后端技术栈与构建说明
- [device/getData/README.md](device/getData/README.md) — MQTT 数据接收服务说明
- [device/postData/README.md](device/postData/README.md) — 控制信号转发服务说明
- [device/simulate/README.md](device/simulate/README.md) — 温湿度 MQTT 模拟器说明
- [smartjavaai-models/README.md](smartjavaai-models/README.md) — 鹦鹉检测与 CLIP 模型文件说明

---

## 学习路径 / Learning Path

![学习路径图示](学习路径图示.jpg)

> 大家有什么好的想法或者好的主意，可以提交到这个仓库，可以提交 txt 文件，也可以提交 markdown 文件。如遇到问题，可以在仓库中发起 **Issue** 或联系仓库管理员。
