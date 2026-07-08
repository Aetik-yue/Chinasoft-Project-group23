# Smart Smoke Sensor Backend

This directory contains the Spring Boot backend for the smart smoke sensor system.

## Tech Stack

- Java 17
- Maven
- Spring Boot 3
- Spring Web
- Spring Data JPA
- MySQL Driver
- Lombok
- Validation

## Package

Base package:

```text
com.chinasoft.smokesensor
```

## Local Build

```bash
mvn clean package
```

## Local Run

Update `src/main/resources/application.yml` with your local MySQL database, username, and password, then run:

```bash
mvn spring-boot:run
```

The default server port is `8080`.

## 与模拟器联动 / 一键链路

要让前端拿到**模拟的实时数据**，仅启动后端不够——还需要 `device/getData`（订阅 MQTT 入库）和 `device/simulate`（每秒发模拟数据）。项目根目录提供了一键脚本，按依赖顺序同时拉起这三个服务，`Ctrl+C` 一起停止：

```powershell
.\start-local.ps1      # PowerShell
# 或双击 start-local.bat
```

详见根目录 [README.md](../README.md)「一键启动本地开发链路」章节。
