# MQTT 烟感数据接收服务

该服务订阅公网 MQTT 的 `group23` 主题，并且只向 MySQL 的
`smoke_data` 表插入烟感历史数据。代码不包含建表、删表或更新其他表的 SQL。

## 消息格式

```json
{
  "ppm": 86.5
}
```

- `ppm` 必须是 0–999 的 JSON 数值，支持整数和浮点数，不能加引号。
- `device_id` 由服务端固定为 `SMK-001`。
- `record_time` 由 MySQL 在插入时通过 `NOW()` 生成。
- `source` 由服务端固定为 `sensor`。
- 消息中的其他字段不会影响上述服务端固定值。

## 运行

```powershell
mvn test
mvn spring-boot:run
```

所有连接参数都可以通过环境变量覆盖：

| 环境变量 | 默认值 |
|---|---|
| `MQTT_HOST_URL` | `tcp://47.108.58.107:1883` |
| `MQTT_DATA_TOPIC` | `group23` |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | 空 |
| `MYSQL_URL` | README 中的 `dream26` JDBC 地址 |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | README 中的数据库账号 |

远端联调测试不会被普通 `mvn test` 自动执行。需要向 `group23` 发布
一条 `ppm` 浮点测试记录时，单独执行：

```powershell
mvn -Dtest=RemoteMqttIntegrationIT test
```
