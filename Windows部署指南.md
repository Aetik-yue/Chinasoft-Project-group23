# Windows 部署指南

> 适用范围：在一台待部署的 Windows 电脑上运行本项目的**核心监测链路**。本指南复用项目当前的远程 MySQL 和 MQTT Broker，在部署电脑本机运行 Redis、后端、前端、MQTT 数据接收服务和温湿度模拟器。
>
> 本指南不会修改远程数据库结构或数据，也不会在文档中重复记录密码、Token、API Key 等敏感信息。

## 1. 部署结果与边界

完成后，部署电脑上会运行以下服务：

```text
device/simulate --发布--> MQTT topic: group23
                                |
device/getData <--订阅-----------+
       |
       +--写入--> 远程 MySQL
                       |
Redis（本机） <---- backend:8080 ----> frontend:5173（本机浏览器）
```

根目录的 `start-local.ps1` 会启动 `device/getData`、`device/simulate`、`backend` 和 `frontend`。`device/postData` 是将数据库控制状态转发给实际硬件的独立服务，默认不在核心启动链路内，见[可选：真实硬件控制](#10-可选真实硬件控制)。

以下能力属于可选扩展，不影响核心链路启动：本地 YOLO/CLIP 鹦鹉识别模型、Qwen/DeepSeek、MaxKB 和 QQ/NapCat。

## 2. 前置条件

部署电脑需要能访问：当前远程 MySQL、MQTT Broker、Maven Central 和 npm Registry。请先确认已获得项目源码及远程服务的合法访问授权。

安装下列软件，并**关闭后重新打开 PowerShell**，使 PATH 生效。

| 软件 | 建议版本 | 用途 |
| --- | --- | --- |
| Git | 当前稳定版 | 获取或更新源码 |
| JDK | 17（64 位） | 构建并运行主后端；也兼容构建 Java 8 的 device 子模块 |
| Maven | 3.8+ | 构建并启动 Java 模块 |
| Node.js | 20 LTS（含 npm） | 安装并启动 Vue/Vite 前端 |
| Docker Desktop | 当前稳定版，使用 Linux containers | 运行本机 Redis |

在 PowerShell 中验证：

```powershell
git --version
java -version
mvn -version
node --version
npm --version
docker version
```

`java -version` 应显示 17；若系统同时安装多个 JDK，请将 JDK 17 的 `bin` 置于 PATH 前面，再重新执行检查。

## 3. 获取源码与准备依赖

将完整项目目录复制到部署电脑，或从项目实际远程仓库克隆。以下以 `C:\workspace` 为例；路径可自行调整，但后续命令要保持一致。

```powershell
New-Item -ItemType Directory -Force C:\workspace | Out-Null
Set-Location C:\workspace

# 二选一：克隆实际项目仓库
# git clone <项目仓库地址> Chinasoft-Project-group23

# 或：将已获得的项目压缩包解压至 C:\workspace\Chinasoft-Project-group23
Set-Location C:\workspace\Chinasoft-Project-group23
```

确认目录中存在 `backend`、`frontend`、`device`、`start-local.ps1`、`group23-structure.sql` 和 `group23-data.sql`。

安装前端的锁定版本依赖，并提前下载各 Java 模块所需依赖：

```powershell
Set-Location C:\workspace\Chinasoft-Project-group23\frontend
npm ci

Set-Location ..\backend
mvn -DskipTests package

Set-Location ..\device\getData
mvn -DskipTests package

Set-Location ..\simulate
mvn -DskipTests package

# 仅在要联调真实硬件控制时执行
Set-Location ..\postData
mvn -DskipTests package
```

上述构建生成的 `target` 目录是本机构建产物，无需提交到版本库。

## 4. 启动本机 Redis

Docker Desktop 必须处于运行状态。首次执行会拉取官方 Redis 镜像：

```powershell
docker run -d --name smart-smoke-redis --restart unless-stopped -p 127.0.0.1:6379:6379 redis:7-alpine
docker exec smart-smoke-redis redis-cli ping
```

第二条命令返回 `PONG` 即表示 Redis 可用。以后电脑重启后，Docker 会自动恢复该容器；如未运行，可执行：

```powershell
docker start smart-smoke-redis
docker exec smart-smoke-redis redis-cli ping
```

## 5. 核对远程服务和本机配置

### 5.1 远程 MySQL 与 MQTT

项目配置默认已提供远程 MySQL、MQTT 地址、主题和数据库名。部署者不需要导入 SQL，也不需要把密码再写入文档。

先检查部署电脑能到达当前配置的远程端口：

```powershell
Test-NetConnection 47.108.58.107 -Port 3306
Test-NetConnection 47.108.58.107 -Port 1883
```

两次结果中的 `TcpTestSucceeded` 都应为 `True`。若远程服务迁移或使用另一套授权凭据，请只在**当前 PowerShell 会话**中覆盖环境变量后再启动服务：

```powershell
$env:MYSQL_URL = 'jdbc:mysql://<MySQL主机>:3306/<数据库名>?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
$env:MYSQL_USERNAME = '<数据库用户名>'
$env:MYSQL_PASSWORD = '<数据库密码>'
$env:MQTT_HOST_URL = 'tcp://<MQTT主机>:1883'
$env:MQTT_USERNAME = '<MQTT用户名>'     # 无认证时删除或留空
$env:MQTT_PASSWORD = '<MQTT密码>'       # 无认证时删除或留空
$env:MQTT_DATA_TOPIC = 'group23'
```

不要把这些值写入 Git 跟踪文件。关闭 PowerShell 后，以上临时变量会自动失效。

### 5.2 前端必须指向本机后端

仓库的 `frontend/.env` 可能保留团队局域网后端地址。部署电脑运行自身后端时，请将其中的 `VITE_BACKEND_HOST` 改为 `localhost`，其他已有地图配置按实际需要保留：

```dotenv
VITE_BACKEND_HOST=localhost
```

该项必须在运行 `npm run dev` 或 `start-local.ps1` **之前**修改；修改后重启前端。它同时决定前端的 API 代理和 WebSocket 地址。

### 5.3 数据库脚本不是默认步骤

本指南复用既有远程库，因此**不要执行** `group23-structure.sql` 或 `group23-data.sql`。其中结构脚本含 `DROP TABLE`，导入会删除目标库中的已有表；数据脚本也可能覆盖或重复既有演示数据。

它们仅用于经负责人授权的“新建空库/灾难恢复”场景，并应在隔离数据库中先验证。

## 6. 一键启动核心链路

确认 Redis 为 `PONG`、前端依赖已安装、`VITE_BACKEND_HOST=localhost` 后，在项目根目录执行：

```powershell
Set-Location C:\workspace\Chinasoft-Project-group23
.\start-local.ps1
```

若 PowerShell 执行策略阻止脚本，使用仓库提供的批处理入口：

```powershell
.\start-local.bat
```

脚本会打开四个命令窗口：

| 窗口标题 | 模块 | 成功特征 |
| --- | --- | --- |
| `dev-getData` | MQTT 数据接收 | 已连接 MQTT 并订阅 `group23`，随后持续收到消息 |
| `dev-simulate` | 温湿度模拟器 | MQTT 连接成功，持续输出模拟数据已发送 |
| `dev-backend` | Spring Boot 主后端 | `Tomcat started on port 8080` |
| `dev-frontend` | Vite 前端 | 显示本地访问地址 `http://127.0.0.1:5173/` |

脚本会等待后端 8080 就绪后再启动前端。MQTT 接收和模拟器并行启动，前几秒模拟数据可能在接收服务订阅完成前丢失；模拟器会持续发送，链路稳定后会正常入库。

在**部署电脑本机**浏览器打开：

- 前端：<http://127.0.0.1:5173/>
- 后端：<http://127.0.0.1:8080/>

Vite 当前仅监听 `127.0.0.1`，默认只供部署电脑本机访问。

## 7. 手动分模块启动

需要定位故障时，可关闭一键脚本后，在四个独立 PowerShell 窗口执行：

```powershell
# 窗口 1：接收 MQTT 并写入远程 MySQL
Set-Location C:\workspace\Chinasoft-Project-group23\device\getData
mvn spring-boot:run
```

```powershell
# 窗口 2：持续发布温湿度模拟数据
Set-Location C:\workspace\Chinasoft-Project-group23\device\simulate
mvn spring-boot:run
```

```powershell
# 窗口 3：主后端；Redis 必须已启动
Set-Location C:\workspace\Chinasoft-Project-group23\backend
mvn spring-boot:run
```

```powershell
# 窗口 4：前端
Set-Location C:\workspace\Chinasoft-Project-group23\frontend
npm run dev
```

启动顺序建议为 Redis → getData → simulate → backend → frontend；模拟器必须在 getData 成功订阅后运行，才能从第一条数据开始完整接收。

## 8. 验收清单

### 8.1 本机服务

在新的 PowerShell 窗口执行：

```powershell
docker exec smart-smoke-redis redis-cli ping
Test-NetConnection 127.0.0.1 -Port 8080
Test-NetConnection 127.0.0.1 -Port 5173
```

预期 Redis 返回 `PONG`，两个端口检查均为 `TcpTestSucceeded : True`。

### 8.2 MQTT 与数据库入库

观察 `dev-getData`：应先显示 MQTT 订阅成功，随后显示接收到 MQTT 消息。观察 `dev-simulate`：应持续输出向 `group23` 发送温度和湿度数据。

使用具有远程数据库只读权限的客户端执行以下 SQL，确认时间最新的记录持续变化：

```sql
SELECT id, device_id, temperature_value, record_time, source
FROM temperature_data
ORDER BY record_time DESC
LIMIT 10;

SELECT id, device_id, humidity_value, record_time, source
FROM humidity_data
ORDER BY record_time DESC
LIMIT 10;
```

以上字段名与仓库中的当前结构脚本一致；如远程数据库经过单独演进导致客户端提示列名不匹配，可先执行 `DESCRIBE temperature_data;` 或 `DESCRIBE humidity_data;`，不应为了验收修改远程表。

### 8.3 前端功能

打开前端后，确认页面能加载，不出现 API 502 或 WebSocket 连接失败；等待一到两分钟，确认环境监测数据随模拟器刷新。后端日志不应持续出现 Redis 连接拒绝或 MySQL/MQTT 连接失败。

## 9. 停止与日常维护

在运行 `start-local.ps1` 的主窗口按 `Ctrl+C`，脚本会结束它创建的四个命令窗口及其子进程。手动启动时，在每个模块窗口按 `Ctrl+C`。

Redis 默认随 Docker 启动；若需要临时停止：

```powershell
docker stop smart-smoke-redis
```

再次启动：

```powershell
docker start smart-smoke-redis
```

## 10. 可选：真实硬件控制

仅在连接了兼容硬件、确认现场安全并获得授权时启动 `postData`：

```powershell
Set-Location C:\workspace\Chinasoft-Project-group23\device\postData
mvn spring-boot:run
```

该服务每秒读取远程 `device_control` 中设备 `SMK-001` 的控制状态变化，并向 MQTT 主题 `group23-s-to-h` 发布控制信号。启动后会同步当前状态，实际硬件可能立即动作；不要为了测试随意修改远程 `device_control`。

## 11. 可选：AI 与第三方扩展

### 本地 YOLO/CLIP

`smartjavaai-models` 中的 `yolov8n.onnx` 和 `clip.pt` 被 `.gitignore` 排除，正常克隆后通常不存在。它们缺失时主后端可以启动，但本地鹦鹉识别接口不可用。

如需启用，按 `smartjavaai-models/README.md` 获取模型，并将模型文件与已在仓库中的 `synset.txt`、`tokenizer.json` 放在同一目录。随后将 `backend/src/main/resources/application.yml` 内的 `parrot.snapshot-path`、`parrot.detection.model-path`、`parrot.clip.model-path` 改为部署电脑上的实际绝对路径，再重启后端。

### Qwen、DeepSeek、MaxKB、QQ/NapCat

- Qwen 与 DeepSeek：通过 `QWEN_API_KEY`、`DEEPSEEK_API_KEY` 环境变量注入有效密钥。
- MaxKB：需要可访问的 MaxKB 服务、应用 ID 与 API Key；未配置时问答能力降级，不阻断核心服务。
- QQ/NapCat：需要本机安装 QQNT 与 NapCat，并在 NapCat 中配置回调 `http://127.0.0.1:8080/api/qq/callback` 和访问令牌。

所有密钥应只保存于部署电脑的受控环境变量或私有配置系统，不能提交到仓库或写入截图、日志和公开文档。

## 12. 常见问题

### PowerShell 无法执行脚本

优先使用 `start-local.bat`。仅需临时绕过时，也可在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-local.ps1
```

### `mvn`、`node` 或 `docker` 不是内部命令

对应工具未安装，或安装后终端未重开。重新安装并关闭所有 PowerShell 窗口后再执行第 2 节的版本检查。

### 8080 或 5173 被占用

先找出占用进程，再停止确认无关的进程：

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :5173
Get-Process -Id <PID>
```

不要直接结束不明系统进程。

### 后端提示 Redis 连接被拒绝

确认 Docker Desktop 已运行，并重新执行：

```powershell
docker start smart-smoke-redis
docker exec smart-smoke-redis redis-cli ping
```

### `getData` 或 `simulate` 无法连接 MQTT / 数据不入库

依次检查：远程 1883、3306 端口是否可达；`MQTT_*` 与 `MYSQL_*` 环境变量是否一致；`getData` 是否已订阅 `group23`；模拟器与接收服务是否使用相同的 `MQTT_DATA_TOPIC`。不要通过导入 SQL 或修改远程表结构处理连接问题。

### 前端白屏、502 或提示找不到 Vite

确认在 `frontend` 目录执行过 `npm ci`；确认 `frontend/.env` 的 `VITE_BACKEND_HOST=localhost`；确认 8080 已监听。修改 `.env` 后必须重启 `npm run dev`。

### AI 接口不可用或模型文件找不到

核心监测功能不依赖本地模型。若要启用 AI，按第 11 节下载缺失模型、修正 Windows 绝对路径，并配置相应 API Key；不要把模型二进制或密钥作为普通源码提交。
