# MQTT 控制信号转发服务

`postData` 每秒读取一次 `dream28.device_control` 中设备 `SMK-001` 的控制状态，
并把变化发布到 MQTT 主题 `group23-s-to-h`。本服务只读数据库，不包含任何 DDL 或写库 SQL。

## 控制字段映射

| 数据库 `control_type` | 数据库 `status` | MQTT 消息 |
| --- | --- | --- |
| `switch` | `on` / `off` | `{"switch":1}` / `{"switch":0}` |
| `buzzer` | `on` / `off` | `{"buzzer":1}` / `{"buzzer":0}` |
| `alarm_light` | `on` / `off` | `{"led":1}` / `{"led":0}` |

服务启动后会依次同步三路当前状态，随后只发布变化的字段。MQTT 使用 QoS 1，消息不保留。

## 启动

```powershell
cd C:\Users\Administrator\Desktop\Chinasoft-Project-group23\device\postData
mvn spring-boot:run
```

看到 `MQTT 连接成功` 和 `控制信号已发送` 日志即表示数据库到 MQTT 的链路正常。

## Paho 与数据库联调

1. 在 Paho 中连接 `MQTT_HOST_URL` 指向的 Broker（本地示例为 `tcp://localhost:1883`），订阅 `group23-s-to-h`，QoS 选择 1。
2. 先保存当前状态：

```sql
SELECT control_type, status
FROM dream28.device_control
WHERE device_id = 'SMK-001';
```

3. 修改其中一项，例如打开蜂鸣器：

```sql
UPDATE dream28.device_control
SET status = 'on', last_operated_at = NOW(), last_operated_by = 'manual-test'
WHERE device_id = 'SMK-001' AND control_type = 'buzzer';
```

Paho 应收到 `{"buzzer":1}`。测试完成后，用同样的 `UPDATE` 把状态恢复成步骤 2 记录的值。

> 更新控制表可能驱动在线硬件，请确认现场安全后再执行。

## 环境变量

| 环境变量 | 默认值 |
| --- | --- |
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/dream28...` |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `root` / 空，可按联调环境覆盖 |
| `MQTT_HOST_URL` | `tcp://localhost:1883` |
| `MQTT_CONTROL_TOPIC` | `group23-s-to-h` |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | 空 |
| `MQTT_QOS` | `1` |
| `CONTROL_POLL_INTERVAL_MS` | `1000` |

数据库表保存的是当前状态而不是事件队列；如果同一字段在一次轮询间隔内连续变化，
硬件最终会收到最新状态，但中间状态不会被重放。
