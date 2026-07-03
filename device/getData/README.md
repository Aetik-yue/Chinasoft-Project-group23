# MQTT 传感数据接收服务

该服务订阅公网 MQTT 的 `group23` 主题，将烟雾、温度和湿度分别写入
`smoke_data`、`temperature_data` 和 `humidity_data`。代码不包含建表、删表或修改其他表的 SQL。

## 消息格式

每条消息只能包含一种传感数据，数值不能加引号：

```json
{"ppm":86.5}
```

```json
{"℃":25.3}
```

```json
{"%RH":49.8}
```

| 字段 | 有效范围 | 目标表 | `source` |
|---|---:|---|---|
| `ppm` | `0–999` | `smoke_data` | `sensor` |
| `℃` | `0–50` | `temperature_data` | `simulate` |
| `%RH` | `1–99` | `humidity_data` | `simulate` |

- `device_id` 固定为 `SMK-001`。
- `record_time` 由 MySQL 在插入时通过 `NOW()` 生成。
- 烟雾数据继续计算 `normal` / `low` / `medium` / `high` 风险等级。
- 消息中的其他元数据不能覆盖服务端固定值；同时出现多个传感字段的消息会被拒绝。

## 运行

```powershell
mvn test
mvn spring-boot:run
```

| 环境变量 | 默认值 |
|---|---|
| `MQTT_HOST_URL` | `tcp://localhost:1883` |
| `MQTT_DATA_TOPIC` | `group23` |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | 空 |
| `MYSQL_URL` | 本地 `dream28` JDBC 地址，可按联调环境覆盖 |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `root` / 空，可按联调环境覆盖 |

远端联调测试不会被普通 `mvn test` 自动执行。需要显式运行：

```powershell
mvn -Dtest=RemoteMqttIntegrationIT test
```

该测试会向远端三张数据表各写入一条可识别记录，执行前请确认允许写入。
