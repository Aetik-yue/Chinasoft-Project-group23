# 全技术栈环境验证项目

这是一个独立的 Spring Boot 冒烟项目，用来验证智慧烟感后端开发环境。它不包含业务功能，也不会把密码写入文件。

## 覆盖范围

- Java 21、Maven、Spring Boot 3.5
- Git、Docker Desktop、WSL2 Ubuntu 22.04
- 远程 MySQL `dream26`
- 本地 Redis、MaxKB、DataEase
- 远程 MQTT 发布/订阅
- SmartJavaAI Vision 与 ONNX Runtime 初始化

## 一键验证

在 PowerShell 中执行：

```powershell
cd .\testEnvironment
.\verify-environment.ps1
```

脚本会安全提示输入 MySQL 密码。密码只存在于当前进程，验证结束后会被清除。

如果 PowerShell 阻止本地脚本，可仅对当前进程临时放行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\verify-environment.ps1
```

## 手动运行测试

```powershell
$env:MYSQL_PASSWORD = Read-Host 'MySQL password'
mvn test
Remove-Item Env:MYSQL_PASSWORD
```

## 启动验证接口

```powershell
$env:MYSQL_PASSWORD = Read-Host 'MySQL password'
mvn spring-boot:run
```

访问：

```text
GET http://localhost:8099/api/environment/check
```

响应包含每项检查的状态和耗时，但不会包含数据库密码。总体为 `UP` 表示全部通过；总体为 `DOWN` 时查看 `checks` 中的失败项。

## 可选环境变量

可用变量见 `.env.example`。该文件只提供变量名称和非敏感默认值，不应向仓库提交真实密码。

## 说明

- MySQL 检查只执行查询，不修改远程数据库。
- Redis 检查创建一个带 30 秒过期时间的随机键，读取后立即删除。
- MQTT 检查使用随机 Topic 和 QoS 1，完成发布/订阅后断开连接。
- SmartJavaAI 只验证目标检测类和 ONNX 原生运行时，不下载或运行具体模型。
