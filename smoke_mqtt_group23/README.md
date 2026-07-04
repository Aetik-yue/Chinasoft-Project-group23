# 智慧烟感 - Group 23

## 功能说明

BearPi-HM Nano 开发板读取 MQ2 烟雾传感器 ppm 值，通过 MQTT 上报到 broker（47.108.58.107:1883）。

**主要特性：**
- 每秒上报一次 ppm 值（JSON 格式）
- ppm > 100 自动报警（蜂鸣器 + LED 100ms 快闪）
- F1 按键：强制报警模式（无视 ppm）
- F2 按键：恢复正常自动模式
- 订阅 `group23-s-to-h` 接收后端指令：`{"sensor":0}` 关闭传感器（停止读 ppm 与上报，仍可收 MQTT），`{"sensor":1}` 恢复全部功能

## 硬件连接

- **MQ2 烟雾传感器**：ADC 通道 6（E53_SF1 扩展板）
- **蜂鸣器**：GPIO_8 / PWM1（E53_SF1 扩展板）
- **板载 LED**：GPIO_2
- **F1 按键**：GPIO_11
- **F2 按键**：GPIO_12

## WiFi 配置

修改 `iot_smoke.c` 顶部宏定义：

```c
#define WIFI_SSID  "你的WiFi名称"    // 必须 2.4GHz
#define WIFI_PWD   "你的WiFi密码"
```

**注意**：Hi3861 只支持 2.4GHz，手机热点需在设置里切换频段。

## MQTT 配置

```c
#define BROKER_IP    "47.108.58.107"
#define BROKER_PORT  1883
#define PUB_TOPIC    "group23"              // 上报主题
#define CLIENT_ID    "bearpi_group23"
```

## 编译烧录

1. 在 `sample/BUILD.gn` 中启用本样例（取消注释）：
   ```gn
   "smoke_mqtt_group23:smoke_mqtt_group23",
   ```

2. 编译：
   ```bash
   python build.py BearPi-HM_Nano
   ```

3. 烧录：将编译输出 `out/` 目录下的 `Hi3861_wifiiot_app_allinone.bin` 烧录到开发板

> 完整的环境搭建 / 获取源码 / 烧录 / 排错步骤见 [快速上手.md](快速上手.md)。

## 串口输出示例

```
[WIFI] connect started...
[WIFI] state: 2, disconnect
[WIFI] state: 3, connect success
[MQTT] connecting to 47.108.58.107:1883 ...
[MQTT] connected!
[MQTT] background receive task started
[MQTT] subscribed to 'group23-s-to-h'
[SMOKE] sensor calibrated, start reporting...
[KEY] F1(GPIO_11) / F2(GPIO_12) registered
[MAIN] tick=100ms, report every 10 ticks (1s)
[SMOKE] ppm: 123.456
[SMOKE] ppm over threshold, alarm ON
[MQTT] publish to 'group23': {"ppm":123.456}
```

## JSON 数据格式

上报到 `group23` 主题：

```json
{"ppm": 123.4}
```

## 后端控制

固件订阅下行主题 `group23-s-to-h`，接收 JSON 指令（两个字段可单独发，也可同包发）：

| 指令 | 含义 |
|------|------|
| `{"sensor": 0}` | 关闭传感器：停止读 ppm、停止 MQTT 上报；但**仍可接收**下行 MQTT 指令。若正在报警会立即停止。 |
| `{"sensor": 1}` | 恢复全部功能（读 ppm + 自动报警 + 上报） |
| `{"threshold": N}` | 调报警阈值 ppm，N 为整数，范围 `1..10000`，默认 `100`。下一个上报节拍按新阈值判断。 |

- 切到 0 时，蜂鸣器/LED 立即关闭，F1/F2 按键在此期间被忽略。
- 切回 1 后从下一个上报节拍恢复。
- `threshold` 即使在 sensor=0 期间也可下发，恢复后生效。
- 越界值或非数字会被打印告警并丢弃，不改当前状态。

## 报警逻辑

| 状态 | 触发条件 | 行为 |
|------|---------|------|
| 自动报警 | ppm > 100 | 蜂鸣器响 + LED 100ms 快闪 |
| 强制报警 | 按 F1 | 蜂鸣器响 + LED 100ms 快闪（无视 ppm） |
| 恢复 | 按 F2 | 停止报警，回到自动模式 |

## 文件结构

```
smoke_mqtt_group23/
├── iot_smoke.c          # 主程序（MQTT + 报警状态机）
├── BUILD.gn             # 编译配置
├── README.md            # 本文件
├── include/
│   ├── E53_SF1.h        # MQ2 传感器驱动头文件
│   └── wifi_connect.h   # WiFi 连接头文件
└── src/
    ├── E53_SF1.c        # MQ2 传感器驱动实现
    └── wifi_connect.c   # WiFi 连接实现
```

## 依赖

- `//third_party/paho_mqtt` - MQTT 客户端库
- `//third_party/cJSON` - JSON 序列化库
