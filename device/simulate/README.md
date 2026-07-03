# 温湿度 MQTT 模拟器

该服务每秒生成一组正态分布温湿度数据，并分别发布到 MQTT 的 `group23` 主题：

```json
{"℃":25.3}
{"%RH":49.8}
```

- 温度均值 `25℃`、标准差 `3℃`，范围限定为 `0–50℃`。
- 湿度均值 `50%RH`、标准差 `10%RH`，范围限定为 `1–99%RH`。
- 数值保留一位小数；采用范围内拒绝采样，不会产生越界值。
- QoS 为 1，消息不保留，发送失败的数据不会补发。

## 启动顺序

先在一个 PowerShell 窗口启动数据接收服务：

```powershell
cd C:\Users\Administrator\Desktop\Chinasoft-Project-group23\device\getData
mvn spring-boot:run
```

看到 getData 已连接并订阅 `group23` 后，在另一个窗口启动模拟器：

```powershell
cd C:\Users\Administrator\Desktop\Chinasoft-Project-group23\device\simulate
mvn spring-boot:run
```

按 `Ctrl+C` 停止模拟器。运行后可查询：

```sql
SELECT * FROM dream28.temperature_data ORDER BY id DESC LIMIT 10;
SELECT * FROM dream28.humidity_data ORDER BY id DESC LIMIT 10;
```

## 环境变量

| 环境变量 | 默认值 |
|---|---|
| `MQTT_HOST_URL` | `tcp://localhost:1883` |
| `MQTT_DATA_TOPIC` | `group23` |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | 空 |
| `MQTT_QOS` | `1` |
| `SIMULATION_INTERVAL_MS` | `1000` |
| `TEMPERATURE_MEAN` / `TEMPERATURE_STANDARD_DEVIATION` | `25.0` / `3.0` |
| `TEMPERATURE_MINIMUM` / `TEMPERATURE_MAXIMUM` | `0.0` / `50.0` |
| `HUMIDITY_MEAN` / `HUMIDITY_STANDARD_DEVIATION` | `50.0` / `10.0` |
| `HUMIDITY_MINIMUM` / `HUMIDITY_MAXIMUM` | `1.0` / `99.0` |
