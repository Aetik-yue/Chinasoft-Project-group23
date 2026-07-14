# 鹦鹉日记 Windows 完整部署指南

本指南用于在一台 Windows 10/11 电脑上部署仓库中的全部可用功能：Web 前后端、Redis、MQTT 数据接收与模拟、真实 BearPi 烟感硬件、数据库到硬件的控制下发、NapCat QQ 机器人、MaxKB 知识库问答、Qwen/DeepSeek 和本地鹦鹉识别模型。

> 当前项目默认复用既有的远程 MySQL 与 MQTT Broker。部署者必须已获得这些服务及第三方 API 的合法访问权限。不要将密码、Token、API Key、QQ 号白名单或 Wi-Fi 密码提交到 Git、截图或公开文档。

## 1. 架构、模块与启动顺序

```text
BearPi + MQ2 ── MQTT group23 ──> device/getData ──> MySQL
温湿度模拟器 ── MQTT group23 ──^                     │
                                                     v
浏览器 <── Vite:5173 <── Spring Boot:8080 <── Redis:6379
                         │          │
                         │          ├── NapCat HTTP:3000 <──> QQ
                         │          ├── MaxKB:18080/chat
                         │          └── Qwen / DeepSeek API
MySQL device_control + system_setting ──> device/postData
                                               │
                                               └── MQTT group23-s-to-h ──> BearPi
```

| 模块 | 职责 | 是否必须 |
| --- | --- | --- |
| `backend` | REST API、WebSocket、宠物档案、告警、AI/QQ 集成 | 是 |
| `frontend` | Vue 3 Web 页面 | 是 |
| Redis | 后端缓存、告警与 QQ 会话辅助状态 | 是 |
| `device/getData` | 订阅 `group23`，将烟雾/温度/湿度写入 MySQL | 是 |
| `device/simulate` | 每秒向 `group23` 发布温湿度，便于无硬件演示 | 建议 |
| `device/postData` | 从数据库读取控制状态，发布到 `group23-s-to-h` | 有真实硬件时必须 |
| `smoke_mqtt_group23` | BearPi-HM Nano 烟雾传感器固件 | 有真实硬件时必须 |
| NapCat | QQ 私聊收发、告警推送 | QQ 功能必须 |
| MaxKB | 鹦鹉养护知识库问答 | 知识库问答必须 |
| Qwen / DeepSeek | 图像识别、自然语言 Agent | 对应 AI 功能必须 |

启动顺序：Redis、MaxKB/NapCat（如启用）→ `getData` → `postData`（如有硬件）→ `simulate` 或真实硬件 → `backend` → `frontend`。

## 2. 一次性安装的软件

| 软件 | 版本/要求 | 用途 |
| --- | --- | --- |
| Git | 当前稳定版 | 获取源码 |
| JDK | 17 x64 | 主后端；可兼容构建 Java 8 的 device 模块 |
| Maven | 3.8+ | 构建 Java 模块 |
| Node.js | 20 LTS（含 npm） | Vue/Vite 前端 |
| Docker Desktop | Linux containers、WSL 2 后端 | Redis、MaxKB |
| MySQL Client | 8.x 命令行工具，建议安装 | 只读验收与故障定位 |
| 浏览器 | Edge/Chrome | 使用 Web 管理端 |

Docker Desktop 需先完成 WSL 2 初始化并启动；Docker 官方将 Desktop 作为 Windows 上获取 Docker Engine、CLI 与 Compose 的推荐方式。[Docker Desktop 安装说明](https://docs.docker.com/desktop/setup/install/windows-install/)

重新打开 PowerShell 后验证：

```powershell
git --version
java -version
mvn -version
node --version
npm --version
docker version
mysql --version
```

## 3. 获取源码与构建依赖

以下以 `C:\workspace\Chinasoft-Project-group23` 为例。将完整源码复制到该目录，或克隆实际项目仓库；大模型权重不在 Git 内，后续单独安装。

### 3.1 前端依赖：必须在 `frontend` 安装

前端不是免安装的静态页面。`frontend/package.json` 已锁定运行所需依赖，包含 Vue 3、Vite、Axios、ECharts、Three.js、高德地图加载器和图片裁剪组件；这些包均由 npm 安装，**不要手动复制其他电脑的 `node_modules`**。

```powershell
Set-Location C:\workspace\Chinasoft-Project-group23

Set-Location .\frontend
npm ci

Set-Location ..\backend
mvn -DskipTests package

Set-Location ..\device\getData
mvn -DskipTests package

Set-Location ..\simulate
mvn -DskipTests package

Set-Location ..\postData
mvn -DskipTests package
```

`npm ci` 会严格按 `frontend/package-lock.json` 安装，适合新电脑和验收环境；不要改用 `npm install`，否则可能更新锁定版本。安装结束后依次检查：

```powershell
Set-Location C:\workspace\Chinasoft-Project-group23\frontend
npm run test       # Three.js 行为/模型相关单元测试
npm run build      # 生成 dist，验证生产构建可用
npm run dev        # 开发模式，提供 5173 页面与 API/MaxKB 代理
```

`npm run preview` 只能预览已生成的 `dist`，且不会提供本项目在 `vite.config.js` 中实现的开发期 API/MaxKB 自定义代理；日常联调必须使用 `npm run dev`。若 `npm ci` 失败，先运行 `npm cache verify`，确认 Node 20 LTS、网络代理和 npm Registry 可用后重试；不要删除或提交锁文件来规避问题。

### 3.2 配置文件和环境变量的规则

前端只从 `frontend/.env`、`frontend/.env.local` 读取 `VITE_*` 变量：

- `frontend/.env` 是团队共享默认值，已被 Git 跟踪；不要把机器 IP、个人密钥或临时测试值直接写进去。
- `frontend/.env.local` 不提交版本库，优先级高于 `.env`；每台电脑都应使用它保存自己的后端/MaxKB 地址。
- 任何 `VITE_*` 变量会被 Vite 打包并暴露给浏览器。`VITE_AMAP_KEY`、`VITE_AMAP_SECURITY_CODE` 必须在高德控制台限制域名/IP；**绝不能放数据库密码、OneBot Token、MaxKB API Key、DeepSeek/Qwen Key**。
- 修改 `.env` 或 `.env.local` 后必须停止并重新运行 `npm run dev`；热更新不会重新读取环境变量。

后端和 device 模块读取操作系统环境变量。PowerShell 的 `$env:NAME='value'` 只对当前窗口及其子进程有效；根目录 `start-local.ps1` 新开的命令窗口会继承该窗口变量。含密钥的变量建议放进受控的 Windows 用户环境变量或密钥管理系统，并在设置后重开终端；不要把 `backend/.env.example` 直接改名为 `.env` 后期待 Spring Boot 自动读取，因为当前项目没有 dotenv 加载器。

### 3.3 按部署拓扑创建前端本地配置

在 `frontend` 下新建（或修改）`.env.local`。以下每种情况选择一份，不要将尖括号连同内容照抄。

**A. 单机完整部署（推荐）**：前端、后端、MaxKB 都在同一台电脑。

```dotenv
VITE_BACKEND_HOST=localhost
VITE_FALLBACK_HOST=localhost
VITE_MAXKB_HOST=localhost
VITE_MAXKB_PORT=18080
# 如需地图功能，再从高德控制台填写受限的浏览器端 Key
VITE_AMAP_KEY=<AMap浏览器端Key>
VITE_AMAP_SECURITY_CODE=<AMap安全密钥>
```

**B. 前端单独部署**：后端在局域网另一台电脑。例如后端 IP 为 `192.168.1.20`，MaxKB IP 为 `192.168.1.30`。

```dotenv
VITE_BACKEND_HOST=192.168.1.20
VITE_FALLBACK_HOST=192.168.1.20
VITE_MAXKB_HOST=192.168.1.30
VITE_MAXKB_PORT=18080
VITE_AMAP_KEY=<AMap浏览器端Key>
VITE_AMAP_SECURITY_CODE=<AMap安全密钥>
```

此模式下，前端运行电脑到后端 8080、MaxKB 18080 必须可达；前端的告警/鹦鹉 WebSocket 会直接连接 `VITE_BACKEND_HOST:8080`，因此不能只配置 Vite HTTP 代理而忽略后端防火墙。

**C. 只运行前端、使用团队主后端且保留本机备份后端**：

```dotenv
VITE_BACKEND_HOST=<团队主后端IP或DNS>
VITE_FALLBACK_HOST=localhost
VITE_MAXKB_HOST=<MaxKB所在IP或DNS>
VITE_MAXKB_PORT=18080
```

`vite.config.js` 每 10 秒探测主后端：主后端可达时优先使用它，连接失败后才尝试 `VITE_FALLBACK_HOST`。此模式必须同时在本机启动后端，才能使 fallback 真正有效。

若构建失败，先解决编译错误再启动。`mvn spring-boot:run` 会经过测试编译阶段，不能把 `testCompile` 失败误判为 MySQL、Redis 或网络问题。

## 4. 数据库、MQTT 与前端基础配置

### 4.1 当前远程服务模式

项目当前配置默认使用远程 MySQL 与 MQTT。先检查网络连通性：

```powershell
Test-NetConnection 47.108.58.107 -Port 3306
Test-NetConnection 47.108.58.107 -Port 1883
```

两项均须显示 `TcpTestSucceeded : True`。`backend/src/main/resources/application.yml` 读取当前 MySQL 配置；`getData`、`simulate`、`postData` 支持用下列临时环境变量覆盖 MQTT/MySQL 参数：

```powershell
$env:MYSQL_URL = 'jdbc:mysql://<host>:3306/<database>?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
$env:MYSQL_USERNAME = '<username>'
$env:MYSQL_PASSWORD = '<password>'
$env:MQTT_HOST_URL = 'tcp://<host>:1883'
$env:MQTT_USERNAME = '<username>'
$env:MQTT_PASSWORD = '<password>'
$env:MQTT_DATA_TOPIC = 'group23'
$env:MQTT_CONTROL_TOPIC = 'group23-s-to-h'
```

临时变量只对当前及其子进程生效。不要把真实密码写入 `.env`、批处理文件或仓库配置。

### 4.2 新建独立数据库（仅授权场景）

若不复用远程数据库，先在隔离 MySQL 5.7+/8.0+ 实例创建空库，并把 `backend/src/main/resources/application.yml` 的 `spring.datasource` 修改为新库地址、账号和密码；同时为三个 device 模块设置相同的 `MYSQL_*` 环境变量。

```powershell
mysql -u <管理员账号> -p -e "CREATE DATABASE <数据库名> DEFAULT CHARACTER SET utf8mb4;"
cmd /c "mysql -u <管理员账号> -p <数据库名> < group23-structure.sql"
cmd /c "mysql -u <管理员账号> -p <数据库名> < group23-data.sql"
```

**警告：**`group23-structure.sql` 含 `DROP TABLE`；只允许导入新建的空库，绝对不能对正在使用的远程库执行。导入数据脚本会写入演示数据。

若要完全离线独立运行，还需自行部署 MQTT Broker（如 Mosquitto），并让固件、`getData`、`simulate`、`postData` 使用同一主机、端口和主题；本仓库未提供本地 Broker 的 Compose 文件。

### 4.3 前端地址

不要直接改团队共享的 `frontend/.env`。按[3.3 按部署拓扑创建前端本地配置](#33-按部署拓扑创建前端本地配置)在 `frontend/.env.local` 写入本机配置。`VITE_BACKEND_HOST` 同时影响 API 代理和 WebSocket 地址。当前 Vite 只监听 `127.0.0.1:5173`，默认仅本机浏览器可访问。

### 4.4 后端、设备和扩展服务变量总表

下表明确变量应设置在哪台电脑。`backend` 当前数据库地址写在 `application.yml` 的 `spring.datasource` 中；若后端迁移到另一套数据库，需在该文件修改，而不是只设置 `MYSQL_*`。`MYSQL_*` 是 `getData`/`postData` 的配置入口。

| 变量/配置 | 设置位置 | 单机值 | 分机部署时的值 |
| --- | --- | --- | --- |
| `VITE_BACKEND_HOST` | 前端 `.env.local` | `localhost` | 后端电脑 IP/DNS |
| `VITE_FALLBACK_HOST` | 前端 `.env.local` | `localhost` | 本机备份后端 IP/DNS；没有备份则与主后端相同 |
| `VITE_MAXKB_HOST`、`VITE_MAXKB_PORT` | 前端 `.env.local` | `localhost`、`18080` | MaxKB 电脑 IP/DNS、映射端口 |
| `VITE_AMAP_KEY`、`VITE_AMAP_SECURITY_CODE` | 前端 `.env.local` | 高德浏览器端凭据 | 同左，并在高德控制台放行实际访问域名/IP |
| `spring.datasource.*` | 后端 `application.yml` | 当前远程库或本机 MySQL | 后端实际连接的 MySQL；所有业务表必须在同一库 |
| `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` | `getData`、`postData` 进程环境 | 与后端数据库相同 | 指向后端所用的同一 MySQL |
| `MQTT_HOST_URL`、`MQTT_USERNAME`、`MQTT_PASSWORD` | 三个 device 模块、硬件固件 | 当前 Broker | 所有设备与固件指向同一 Broker |
| `MQTT_DATA_TOPIC` | `getData`、`simulate`、硬件 | `group23` | 保持一致 |
| `MQTT_CONTROL_TOPIC` | `postData`、硬件 | `group23-s-to-h` | 保持一致 |
| `MAXKB_BASE_URL`、`MAXKB_APP_ID`、`MAXKB_API_KEY` | 后端进程环境 | `http://localhost:18080/chat` 等 | MaxKB 电脑地址，必须带 `/chat` |
| `ONEBOT_ACCESS_TOKEN` | 后端进程环境与 NapCat | 同一随机 Token | 同一随机 Token；NapCat HTTP API 地址需在后端配置中改为远端地址 |
| `QWEN_API_KEY`、`DEEPSEEK_API_KEY` | 后端进程环境 | 对应供应商 Key | 在运行后端的电脑设置 |
| `API_KEY_SECRET` | 后端进程环境 | 生成的高强度随机值 | 所有连接同一数据库的后端实例必须一致 |

在**后端与 NapCat 分机**的情况下，还要编辑后端 `application.yml`：将 `qq.onebot.base-url` 从 `http://localhost:3000` 改为 `http://<NapCat电脑IP>:3000`；同时在 NapCat 的 HTTP 客户端中把 callback 地址改为 `http://<后端电脑IP>:8080/api/qq/callback`。不要使用 `localhost`，因为它在每台电脑上都只指向自己。

在**后端与 MaxKB 分机**的情况下，前端和后端都必须使用 MaxKB 电脑的稳定局域网 IP 或 DNS：前端设 `VITE_MAXKB_HOST`，后端设 `MAXKB_BASE_URL=http://<MaxKB电脑IP>:18080/chat`。MaxKB 若只绑定 `127.0.0.1`，跨电脑访问会失败；此时应在受控局域网与防火墙规则下调整端口监听/映射，不能直接暴露到公网。

## 5. Redis 与核心服务

### 5.1 Redis

首次运行：

```powershell
docker run -d --name smart-smoke-redis --restart unless-stopped -p 127.0.0.1:6379:6379 redis:7-alpine
docker exec smart-smoke-redis redis-cli ping
```

返回 `PONG` 即可。以后使用：

```powershell
docker start smart-smoke-redis
docker exec smart-smoke-redis redis-cli ping
```

### 5.2 一键启动

根目录脚本只负责 `getData + simulate + backend + frontend`，不会启动 Redis、MaxKB、NapCat、`postData`。确认前述外部服务已启动后执行：

```powershell
Set-Location C:\workspace\Chinasoft-Project-group23
.\start-local.ps1
```

若执行策略阻止脚本，使用：

```powershell
.\start-local.bat
```

或仅在本次窗口绕过策略：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

成功窗口特征：

| 窗口 | 成功日志 |
| --- | --- |
| `dev-getData` | MQTT 连接成功，已订阅 `group23` |
| `dev-simulate` | MQTT 连接成功，持续发布温湿度 |
| `dev-backend` | `Tomcat started on port 8080` |
| `dev-frontend` | `http://127.0.0.1:5173/` |

访问：<http://127.0.0.1:5173/>；告警 WebSocket 为 `ws://localhost:8080/ws/alarm`，鹦鹉识别 WebSocket 为 `ws://localhost:8080/ws/parrot`。

### 5.3 手动启动

```powershell
# 每行在独立 PowerShell 窗口中执行
Set-Location C:\workspace\Chinasoft-Project-group23\device\getData; mvn spring-boot:run
Set-Location C:\workspace\Chinasoft-Project-group23\device\simulate; mvn spring-boot:run
Set-Location C:\workspace\Chinasoft-Project-group23\backend; mvn spring-boot:run
Set-Location C:\workspace\Chinasoft-Project-group23\frontend; npm run dev
```

停止一键脚本时在主窗口按 `Ctrl+C`；手动启动时逐个窗口按 `Ctrl+C`。停止 Redis：`docker stop smart-smoke-redis`。

## 6. 真实 BearPi 烟感硬件

### 6.1 硬件与契约

需要 BearPi-HM Nano（Hi3861）开发板、E53_SF1 扩展板（MQ2 烟感与蜂鸣器）、Type-C 数据线、CH340 驱动和 **2.4 GHz** Wi-Fi。引脚契约：MQ2 ADC 通道 6，蜂鸣器 GPIO_8/PWM1，板载 LED GPIO_2，F1 GPIO_11，F2 GPIO_12。

固件上行主题为 `group23`，每秒发布：

```json
{"ppm":123.4}
```

固件下行主题为 `group23-s-to-h`，接受：

```json
{"sensor":0}
{"sensor":1}
{"threshold":200}
{"sensor":1,"threshold":200}
```

`sensor=0` 停止读数、上报与报警，但仍保持 MQTT 接收以便恢复；`threshold` 只能是 1–10000 的整数，默认 100 ppm。F1 为强制报警，F2 恢复自动模式；传感器开关关闭时 F1/F2 被忽略。

### 6.2 编译与烧录

1. 按 [BearPi-HM Nano 官方仓库](https://gitee.com/bearpi/bearpi-hm_nano) 准备其指定的编译环境。仓库内 `smoke_mqtt_group23/快速上手.md` 记录了当前项目验证过的 BearPi 虚拟机、CH340、HiBurn、MobaXterm 流程。
2. 克隆官方工程，将本仓库整个 `smoke_mqtt_group23/` 复制到：

   ```text
   bearpi-hm_nano/applications/BearPi/BearPi-HM_Nano/sample/smoke_mqtt_group23/
   ```

3. 编辑官方工程的 `applications/BearPi/BearPi-HM_Nano/sample/BUILD.gn`，在 `features` 中只启用：

   ```gn
   "smoke_mqtt_group23:smoke_mqtt_group23",
   ```

   同时启用多个样例会产生编译冲突。
4. 编辑复制后的 `iot_smoke.c`：设置 `WIFI_SSID`、`WIFI_PWD`、`BROKER_IP`、`BROKER_PORT`、`PUB_TOPIC` 和不与其他设备重复的 `CLIENT_ID`。Wi-Fi 必须是 2.4 GHz。
5. 为避免下行 MQTT 回调解析 JSON 时 `MQTTTask stack overflow`，编辑官方工程 `third_party/paho_mqtt/MQTTClient-C/src/liteOS/MQTTLiteOS.c` 的 `ThreadStart()`：

   ```c
   attr.stack_size = 4096;  /* 原值为 2048 */
   ```

6. 在官方工程根目录编译：

   ```bash
   python build.py BearPi-HM_Nano
   ```

   成功后获取 `out/Hi3861_wifiiot_app_allinone.bin`。
7. Windows 侧用 HiBurn：选择 CH340 对应 COM 口，波特率 921600，选择该 `.bin`，勾选 Auto burn，点击 Connect 后按开发板 RESET，看到 `Execution Successful` 后 Disconnect。
8. 用 MobaXterm 或其他串口工具以 115200 打开同一 COM 口。应看到 Wi-Fi 成功、`MQTT connected!`、订阅 `group23-s-to-h` 和每秒 `publish to 'group23'`。

### 6.3 后端控制下发

在真实硬件已上电、已订阅下行主题且确认现场安全后启动：

```powershell
Set-Location C:\workspace\Chinasoft-Project-group23\device\postData
mvn spring-boot:run
```

`postData` 每秒查询一次：

| 数据来源 | 读取字段 | 发布字段 |
| --- | --- | --- |
| `device_control` | `device_id='SMK-001'`、`control_type='switch'` 的 `status` (`on/off`) | `sensor` (`1/0`) |
| `system_setting` | `setting_key='warning_threshold'` 的 `setting_value` | `threshold` |

首次读取会发布当前值，随后只在值变化时发布；发布失败不会记录为已发布，会在下一轮重试。验证时应由已授权人员通过页面改变设备总开关或告警阈值，观察 `postData` 日志、硬件串口和实际蜂鸣器/LED。不要直接在生产数据库随意更新控制字段。

## 7. NapCat QQ 机器人

### 7.1 安装并登录

在运行后端的同一台电脑安装 QQNT 与 NapCat。NapCat 官方提供 Windows 一键包，当前版本也说明 QQ 需使用其支持的版本；从 [NapCat Releases](https://github.com/NapNeko/NapCatQQ/releases) 下载并严格按其 [安装指南](https://napneko.github.io/guide/install) 完成安装。建议使用专用小号，不要使用个人主账号。

启动后从控制台日志打开 NapCat WebUI。新版 WebUI 默认端口通常为 6099，登录 Token 为随机值；首次登录后立即修改 WebUI 密钥，且不要将 WebUI 暴露到公网。[NapCat WebUI 配置说明](https://napneko.github.io/config/basic)

在 WebUI 的“QQ 登录”中扫描二维码登录机器人账号。

### 7.2 OneBot 网络配置

本项目需要**两条**连接，二者使用同一个强随机 Token：

| NapCat 配置 | 作用 | 本项目值 |
| --- | --- | --- |
| HTTP 服务端 | 后端调用 `/send_private_msg` 主动发送消息 | host `127.0.0.1`，port `3000`，启用，Token `<ONEBOT_ACCESS_TOKEN>` |
| HTTP 客户端 | NapCat 将 QQ 私聊事件 POST 到后端 | URL `http://127.0.0.1:8080/api/qq/callback`，启用，Token `<ONEBOT_ACCESS_TOKEN>`，消息格式 string 或 array |

HTTP 客户端上报 URL 必须精确包含 `/api/qq/callback`。后端只处理 `message_type=private` 的私聊事件，其他事件会忽略；HTTP 服务端和客户端均启用 Token。NapCat 支持 HTTP API 和 HTTP POST 事件上报，详见其 [OneBot 网络说明](https://napneko.github.io/onebot/network)。

在启动后端的 PowerShell 中设置 Token：

```powershell
$env:ONEBOT_ACCESS_TOKEN = '<与NapCat一致的随机Token>'
```

检查 `backend/src/main/resources/application.yml` 的 `qq.onebot`：`enabled: true`、`base-url: http://localhost:3000`、`allowed-users` 仅保留获授权 QQ 号、`push-target-user` 改为接收告警的 QQ 号。重启后端后验证：

```text
http://127.0.0.1:8080/api/qq/test/send?msg=QQ通道测试
```

浏览器访问该地址后，目标 QQ 应收到消息；再向机器人小号发送私聊文本，后端日志应出现 OneBot 上报。若不部署 NapCat，设置 `qq.onebot.enabled=false` 后重启后端。

## 8. MaxKB 知识库问答

### 8.1 部署 MaxKB

MaxKB 与项目后端分离运行。为避免和 Spring Boot 的 8080 冲突，本机映射到 18080。创建数据目录后启动：

```powershell
New-Item -ItemType Directory -Force C:\maxkb-data | Out-Null
docker pull 1panel/maxkb:latest
docker run -d --name maxkb --restart unless-stopped -p 127.0.0.1:18080:8080 -v C:\maxkb-data:/opt/maxkb 1panel/maxkb:latest
docker ps --filter name=maxkb
```

浏览器访问 <http://127.0.0.1:18080/>，按当前 MaxKB 首次启动向导创建管理员账号。若镜像版本的挂载目录或初始化方式有变化，以 [MaxKB 官方安装部署文档](https://maxkb.cn/docs/v2/faq/install_configuration/) 与当前发布包为准；官方说明 MaxKB V2 的 Docker Desktop 数据挂载路径为 `/opt/maxkb`。

> MaxKB 自身还需要配置一个可用的大语言模型供应商或本地模型，才能生成回答。该模型凭据只录入 MaxKB 管理台，不能写入本仓库。

### 8.2 导入知识与创建应用

1. 在 MaxKB 管理台配置模型供应商及聊天模型，先用管理台测试模型连通性。
2. 创建知识库，导入仓库 `知识库/` 下所有 Markdown 文件；如需要，补充项目专属文档。
3. 创建“应用/智能体”，关联该知识库与聊天模型，发布应用。
4. 从该应用页面复制 **应用 ID** 与 **API Key**；API Key 仅显示一次时立即存入密码管理器。

### 8.3 连接项目后端和前端

在启动后端的 PowerShell 中：

```powershell
$env:MAXKB_BASE_URL = 'http://localhost:18080/chat'
$env:MAXKB_APP_ID = '<MaxKB应用ID>'
$env:MAXKB_API_KEY = '<MaxKB应用API Key>'
```

`application.yml` 中 `qq.maxkb.enabled` 需为 `true`。项目后端会请求 `/chat/api/open?application_id=...` 和 `/chat/api/chat_message/{chatId}`；因此 `MAXKB_BASE_URL` 必须保留 `/chat` 后缀。前端 `VITE_MAXKB_HOST/PORT` 必须指向同一服务，Vite 会将 `/chat` 请求转发到 MaxKB。

重启后端和前端后，通过 QQ 发送非系统命令问题，或在前端调用聊天入口验证。若 MaxKB 未部署，可设置 `qq.maxkb.enabled=false`；核心监测不受影响。

## 9. Qwen、DeepSeek 与本地视觉模型

### 9.1 Qwen 视觉识别

在阿里云百炼/DashScope 创建 API Key，然后在启动后端前设置：

```powershell
$env:QWEN_API_KEY = '<DashScope API Key>'
```

保持 `qwen.vision.enabled: true`，并根据账户可用模型调整 `qwen.vision.model`。Qwen 用于 3D 鹦鹉画面和上传图片的多模态识别；未设置 Key 时，该接口返回“未启用/不可用”，不会阻止后端启动。

### 9.2 DeepSeek Agent

在 DeepSeek 平台创建 API Key：

```powershell
$env:DEEPSEEK_API_KEY = '<DeepSeek API Key>'
```

`qq.llm.enabled: true` 时，QQ Agent 的优先级为 DeepSeek function calling → MaxKB → 规则回复。若不希望产生外部调用费用，设置 `qq.llm.enabled=false` 并重启后端。

### 9.3 本地 YOLO + CLIP 鹦鹉识别

仓库不会提交 `*.onnx`、`*.pt`。按 `smartjavaai-models/README.md` 下载：

**注：本小组将会直接以压缩包形式上传全部源码，因此会自带.onnx`、`.pt`文件** 

| 文件 | 用途 | 配套文件 |
| --- | --- | --- |
| `yolov8n.onnx` | 检测 COCO 的 `bird` 目标 | 同目录 `synset.txt` |
| `clip.pt` | 鹦鹉行为、品种零样本分类 | 同目录 `tokenizer.json` |

把文件放入 `smartjavaai-models/`，然后把 `backend/src/main/resources/application.yml` 中的 Windows 绝对路径更新为实际路径：

```yaml
parrot:
  snapshot-path: "C:/workspace/Chinasoft-Project-group23/smartjavaai-models/parrot.png"
  detection:
    enabled: true
    model-path: "C:/workspace/Chinasoft-Project-group23/smartjavaai-models/yolov8n.onnx"
  clip:
    enabled: true
    model-path: "C:/workspace/Chinasoft-Project-group23/smartjavaai-models/clip.pt"
```

模型采用懒加载；缺失时后端仍可启动，但首次调用识别接口会失败。仅使用 Qwen 时，可将 `parrot.detection.enabled`、`parrot.clip.enabled` 改为 `false`。

### 9.4 火焰/烟雾视觉复核

后端还保留 `smartjavaai.vision` 的火焰/烟雾视觉复核能力。它默认关闭，启用前必须准备一个类别名与配置一致的自定义 ONNX 模型，并填写其绝对路径：

```yaml
smartjavaai:
  vision:
    enabled: true
    model-path: "C:/workspace/Chinasoft-Project-group23/smartjavaai-models/fire-smoke.onnx"
    model-enum: YOLOV12_CUSTOM_ONNX
    threshold: 0.5
    fire-classes: fire,flame
    smoke-classes: smoke
```

模型训练类别名称必须至少与 `fire-classes`、`smoke-classes` 对应；否则模型虽能加载，识别结果仍不会被归类为火焰或烟雾。未准备该自定义模型时保持 `enabled: false`，不影响传感器告警、鹦鹉功能或其他 AI 功能。

## 10. 全量验收

### 服务与端口

```powershell
docker exec smart-smoke-redis redis-cli ping
docker ps --filter name=maxkb
Test-NetConnection 127.0.0.1 -Port 8080
Test-NetConnection 127.0.0.1 -Port 5173
Test-NetConnection 127.0.0.1 -Port 18080
Test-NetConnection 127.0.0.1 -Port 3000  # 启用 NapCat 时
```

Redis 必须返回 `PONG`；启用的端口须显示 `TcpTestSucceeded : True`。

### MQTT 与数据库

确认 `getData` 已订阅 `group23`，然后启动模拟器或硬件。使用只读数据库账号检查：

```sql
SELECT id, device_id, temperature_value, record_time, source
FROM temperature_data ORDER BY record_time DESC LIMIT 10;

SELECT id, device_id, humidity_value, record_time, source
FROM humidity_data ORDER BY record_time DESC LIMIT 10;

SELECT id, device_id, smoke_value, risk_level, record_time, source
FROM smoke_data ORDER BY record_time DESC LIMIT 10;
```

真实硬件还应验证：串口看到 `{"ppm":...}` 上报；在已授权、安全的环境中更改总开关/阈值，`postData` 发布对应 `sensor/threshold`，板端收到并生效。

### AI 与 QQ

- MaxKB 管理台可回答知识库内容，项目 QQ 私聊也能得到同一类回答。
- 调用 `GET /api/qq/test/send` 后目标 QQ 收到消息；向机器人发送私聊后后端收到 callback。
- 有 Qwen Key 时图片识别接口返回结果；有 YOLO/CLIP 文件时本地识别可加载模型。

## 11. 常见故障

| 现象 | 处理 |
| --- | --- |
| `mvn` 启动失败 | 先查看是否在 `testCompile` 失败；运行 `mvn test` 找到具体测试/编译错误 |
| 后端 Redis 连接拒绝 | 启动 Docker Desktop，执行 `docker start smart-smoke-redis` 与 `redis-cli ping` |
| 前端 502/白屏 | 运行 `npm ci`；确认 `.env.local` 为 `VITE_BACKEND_HOST=localhost`；确认 8080 已监听；改 `.env.local` 后重启 Vite |
| MQTT 连接失败 | 检查 1883 可达、认证参数、主题一致性与客户端 ID；固件的 Wi-Fi 必须是 2.4 GHz |
| 数据未入库 | 先确认 `getData` 已订阅，再启动模拟器/硬件；检查 3306、数据库账号、表结构和 MySQL 日志 |
| `MQTTTask stack overflow` | 按第 6.2 节将 BearPi 官方 Paho `attr.stack_size` 从 2048 改为 4096 后完整重新编译烧录 |
| NapCat 无法回调/发消息 | 检查 HTTP 服务端 3000、HTTP 客户端 callback URL、两端 Token、`allowed-users` 及 8080 防火墙；不要把 WebUI 暴露公网 |
| MaxKB 502 | 检查容器、18080、`MAXKB_BASE_URL` 的 `/chat` 后缀、应用 ID/API Key 和 MaxKB 内部模型配置 |
| 模型找不到 | 检查 `.onnx/.pt` 是否存在、绝对路径用 `/` 或双反斜杠、配套 `synset.txt/tokenizer.json` 是否同目录 |
| 8080/5173/18080 被占用 | `netstat -ano | findstr :<端口>`，再用 `Get-Process -Id <PID>` 确认进程后处理 |

## 12. 安全与备份

- 生产部署为 MySQL、MQTT、NapCat、MaxKB 设置独立低权限账号和强密码；不要使用仓库示例或默认凭据。
- 仅开放必要端口；Redis、NapCat HTTP API、NapCat WebUI、MaxKB 管理台优先只绑定 `127.0.0.1`，远程管理使用 VPN 或受控反向代理。
- QQ 控制必须使用专用机器人账号和最小化 `allowed-users`；真实硬件控制前需进行现场安全确认。
- 定期备份 MySQL；备份前后验证可恢复性。MaxKB 数据目录 `C:\maxkb-data` 也应纳入备份，但应先停止容器或遵循 MaxKB 官方备份流程。
- API Key 使用操作系统环境变量或密钥管理系统；更换/泄露后立即在供应商控制台撤销旧 Key 并重启相关服务。
