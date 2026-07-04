/*
 * 智慧烟感 - 非阻塞报警模块版
 *
 * 报警模块 (状态机, 非阻塞):
 *   Alarm_On()        打开蜂鸣器, 标记报警中
 *   Alarm_Off()       关闭蜂鸣器+LED
 *   Alarm_Tick()      每 100ms 调用, 报警中则翻转 LED
 *
 *   状态: alarm_active (当前是否报警)
 *         alarm_mode   (0=自动模式, 1=F1强制模式)
 *
 *   F1 按键: alarm_mode=1, Alarm_On()   → 强制报警, 无视 ppm
 *   F2 按键: alarm_mode=0, Alarm_Off()  → 切回自动
 *   ppm>阈值 (且 alarm_mode==0): Alarm_On()
 *   ppm≤阈值 (且 alarm_mode==0): Alarm_Off()
 *
 *   主循环以 100ms 为基本节拍:
 *     每次迭代: Alarm_Tick() + 处理按键
 *     每 10 次 (1s): 读 ppm + 自动报警判断 + MQTT 发布
 *
 * 数据流:  MQ2(ADC) -> 算ppm -> cJSON打包成 {"ppm":xxx} -> MQTT发布 -> broker
 *
 * 后端控制 (下行主题 group23-s-to-h):
 *   {"sensor":0/1}    0=关传感器(只收MQTT不读不上报), 1=恢复
 *   {"threshold":N}    调报警阈值 ppm (默认100, 范围1..10000)
 *
 * 硬件:  BearPi-HM Nano (Hi3861) + E53_SF1 扩展板 (MQ2 + 蜂鸣器)
 *        板载LED(GPIO_2), F1按键(GPIO_11), F2按键(GPIO_12)
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <math.h>

#include "ohos_init.h"
#include "cmsis_os2.h"

#include "wifi_connect.h"
#include "MQTTClient.h"
#include "E53_SF1.h"
#include "cJSON.h"
#include "wifiiot_gpio.h"
#include "wifiiot_gpio_ex.h"

/* ============== 用户配置区 (改这里) ============== */
#define WIFI_SSID        "WIFI"             /* WiFi 名 (必须 2.4GHz) */
#define WIFI_PWD         "PASSWORD"         /* WiFi 密码 */

#define BROKER_IP        "47.108.58.107"    /* 老师给的 MQTT 服务器 */
#define BROKER_PORT      1883
#define PUB_TOPIC        "group23"          /* 发布主题, 后端订阅这个名字 */
#define CLIENT_ID        "bearpi_group23"   /* 客户端ID, 别和别人重复 */

#define SMOKE_THRESHOLD  100                /* 默认报警阈值 ppm (后端可经 {"threshold":N} 在线调) */
#define REPORT_INTERVAL  1                  /* 上报间隔 (秒) */
#define ALARM_BLINK_MS   100                /* 报警灯闪烁间隔 (毫秒) */
/* ================================================= */

/* 板载硬件引脚 (固定, 不改) */
#define LED_GPIO   WIFI_IOT_IO_NAME_GPIO_2   /* 用户LED */
#define F1_GPIO    WIFI_IOT_IO_NAME_GPIO_11  /* F1 按键 */
#define F2_GPIO    WIFI_IOT_IO_NAME_GPIO_12  /* F2 按键 */

/* 100ms 一tick, 多少次 tick = 1 次上报 */
#define TICKS_PER_REPORT  ((REPORT_INTERVAL * 1000) / ALARM_BLINK_MS)

#define TASK_STACK_SIZE  10240
#define TASK_PRIO        25

/* ISR与主任务共享的标志 (ISR只写, 主任务读后清零) */
static volatile int g_key_flag = 0;  /* 0=无按键, 1=F1, 2=F2 */

/* 后端 sensor 开关 (MQTT回调线程写, 主任务读; 沿用 ISR 套路避免竞态) */
static volatile int g_sensor_enabled = 1;  /* 1=正常工作, 0=只收MQTT不读不上报 */
static volatile int g_sensor_changed = 0;   /* 开关状态变化, 置1后由主任务处理副作用 */

/* 后端可调报警阈值 (回调线程写, 主任务每秒读; 纯数值无副作用, 直接用即可) */
static volatile int g_smoke_threshold = SMOKE_THRESHOLD;  /* ppm */

static unsigned char sendBuf[1000];
static unsigned char readBuf[1000];
static Network network;

/* ================================================================
 * 报警模块 (状态机, 非阻塞 — 后续加新功能只改这里)
 * ================================================================ */

static int alarm_active = 0;   /* 当前是否处于报警 */
static int alarm_led    = 0;   /* LED 当前电平状态 */
static int alarm_mode   = 0;   /* 0=自动(看ppm), 1=F1强制 */

/** 打开报警 (仅在状态变化时执行, 避免重复调用) */
static void Alarm_On(void)
{
    if (!alarm_active) {
        alarm_active = 1;
        alarm_led = 0;
        Beep_StatusSet(ON);
    }
}

/** 关闭报警 */
static void Alarm_Off(void)
{
    if (alarm_active) {
        alarm_active = 0;
        Beep_StatusSet(OFF);
        GpioSetOutputVal(LED_GPIO, 0);
    }
}

/** 每 100ms 调用一次: 报警中则翻转 LED */
static void Alarm_Tick(void)
{
    if (alarm_active) {
        alarm_led = !alarm_led;
        GpioSetOutputVal(LED_GPIO, alarm_led);
    }
}

/* ================================================================
 * 按键中断 (ISR, 只写标志, 不阻塞)
 * ================================================================ */
static void F1_Pressed(char *arg) { (void)arg; g_key_flag = 1; }
static void F2_Pressed(char *arg) { (void)arg; g_key_flag = 2; }

/* 初始化 F1/F2 按键 (下降沿触发中断) */
static void Button_Init(void)
{
    IoSetFunc(F1_GPIO, WIFI_IOT_IO_FUNC_GPIO_11_GPIO);
    GpioSetDir(F1_GPIO, WIFI_IOT_GPIO_DIR_IN);
    IoSetPull(F1_GPIO, WIFI_IOT_IO_PULL_UP);
    GpioRegisterIsrFunc(F1_GPIO, WIFI_IOT_INT_TYPE_EDGE,
                        WIFI_IOT_GPIO_EDGE_FALL_LEVEL_LOW, F1_Pressed, NULL);

    IoSetFunc(F2_GPIO, WIFI_IOT_IO_FUNC_GPIO_12_GPIO);
    GpioSetDir(F2_GPIO, WIFI_IOT_GPIO_DIR_IN);
    IoSetPull(F2_GPIO, WIFI_IOT_IO_PULL_UP);
    GpioRegisterIsrFunc(F2_GPIO, WIFI_IOT_INT_TYPE_EDGE,
                        WIFI_IOT_GPIO_EDGE_FALL_LEVEL_LOW, F2_Pressed, NULL);
}

/* ================================================================
 * MQTT 连接 (连上后启动后台收包线程 + 订阅下行主题)
 * ================================================================ */

/* MQTT 下行回调: 解析 {"sensor":0/1} 控制传感器开关
 * 注意: 此函数跑在 paho 后台线程, 不能阻塞; 只置标志, 副作用交给主任务 */
void messageArrived(MessageData* data)
{
    char buf[128];
    int len = data->message->payloadlen;

    if (len <= 0 || len >= (int)sizeof(buf)) {
        printf("[MQTT] recv: payload len %d invalid (need < %d)\n",
               len, (int)sizeof(buf));
        return;
    }
    memcpy(buf, data->message->payload, len);   /* payload 无 '\0', 必须 copy */
    buf[len] = '\0';

    printf("[MQTT] recv topic:%.*s  payload:%s\n",
           data->topicName->lenstring.len, data->topicName->lenstring.data, buf);

    cJSON *root = cJSON_Parse(buf);
    if (root == NULL) {
        printf("[CTRL] json parse failed\n");
        return;
    }

    cJSON *sensor = cJSON_GetObjectItem(root, "sensor");
    if (cJSON_IsNumber(sensor)) {
        int v = sensor->valueint;
        if (v == 0 || v == 1) {
            g_sensor_enabled = v;
            g_sensor_changed = 1;   /* 通知主任务处理副作用 */
            printf("[CTRL] sensor -> %d\n", v);
        } else {
            printf("[CTRL] sensor must be 0 or 1, got %d\n", v);
        }
    }

    cJSON *threshold = cJSON_GetObjectItem(root, "threshold");
    if (cJSON_IsNumber(threshold)) {
        int t = threshold->valueint;
        if (t > 0 && t <= 10000) {
            g_smoke_threshold = t;
            printf("[CTRL] threshold -> %d ppm\n", t);
        } else {
            printf("[CTRL] threshold %d out of range (1..10000)\n", t);
        }
    }

    cJSON_Delete(root);
}

static int mqtt_connect(MQTTClient *client)
{
    int rc;
    NetworkInit(&network);

    while (1) {
        printf("[MQTT] connecting to %s:%d ...\n", BROKER_IP, BROKER_PORT);
        NetworkConnect(&network, BROKER_IP, BROKER_PORT);
        MQTTClientInit(client, &network, 2000,
                       sendBuf, sizeof(sendBuf),
                       readBuf, sizeof(readBuf));

        MQTTString clientId = MQTTString_initializer;
        clientId.cstring = CLIENT_ID;

        MQTTPacket_connectData conn = MQTTPacket_connectData_initializer;
        conn.clientID          = clientId;
        conn.willFlag          = 0;
        conn.MQTTVersion       = 3;
        conn.keepAliveInterval = 60;
        conn.cleansession      = 1;

        rc = MQTTConnect(client, &conn);
        if (rc == 0) {
            printf("[MQTT] connected!\n");
            MQTTStartTask(client);
            printf("[MQTT] background receive task started\n");
            rc = MQTTSubscribe(client, "group23-s-to-h", 1, messageArrived);
            if (rc == 0) {
                printf("[MQTT] subscribed to 'group23-s-to-h'\n");
            } else {
                printf("[MQTT] subscribe failed: %d\n", rc);
            }
            return 1;
        }
        printf("[MQTT] connect failed: %d, retry in 2s\n", rc);
        NetworkDisconnect(&network);
        MQTTDisconnect(client);
        sleep(2);
    }
}

/* ================================================================
 * 主任务 (100ms 节拍, 非阻塞)
 * ================================================================ */
static void SmokeMqttTask(void)
{
    int rc;
    float ppm;
    int tick = 0;           /* 100ms 计数器 */
    int report_counter = 0; /* 上报节拍计数器 */

    /* 1. 连 WiFi */
    WifiConnect(WIFI_SSID, WIFI_PWD);

    /* 2. 连 MQTT broker */
    MQTTClient client;
    if (!mqtt_connect(&client)) {
        printf("[MQTT] give up, please reset board\n");
        return;
    }

    /* 3. 初始化烟雾传感器并校准 (校准时环境必须是洁净空气) */
    Init_E53_SF1();
    usleep(1000000);
    MQ2_PPM_Calibration();
    printf("[SMOKE] sensor calibrated, start reporting...\n");

    /* 3.5 初始化板载 LED 和按键 */
    IoSetFunc(LED_GPIO, WIFI_IOT_IO_FUNC_GPIO_2_GPIO);
    GpioSetDir(LED_GPIO, WIFI_IOT_GPIO_DIR_OUT);
    GpioSetOutputVal(LED_GPIO, 0);
    Button_Init();
    printf("[KEY] F1(GPIO_11) / F2(GPIO_12) registered\n");
    printf("[MAIN] tick=%dms, report every %d ticks (%ds)\n",
           ALARM_BLINK_MS, TICKS_PER_REPORT, REPORT_INTERVAL);

    /* 4. 主循环 (100ms 节拍, 永远不阻塞) */
    while (1) {
        /* ---- 每个 tick (100ms) 都做: LED闪烁 + 按键 ---- */
        Alarm_Tick();

        /* 后端 sensor 开关变化: 在主任务里处理副作用, 避免回调线程动硬件竞态 */
        if (g_sensor_changed) {
            g_sensor_changed = 0;
            if (!g_sensor_enabled) {
                Alarm_Off();
                alarm_mode = 0;
                printf("[CTRL] sensor OFF: stop reading & publishing\n");
            } else {
                printf("[CTRL] sensor ON: resume all functions\n");
            }
        }

        if (g_key_flag == 1) {
            g_key_flag = 0;
            if (g_sensor_enabled) {
                alarm_mode = 1;   /* 强制模式 */
                Alarm_On();
                printf("[KEY] F1: forced alarm ON\n");
            } else {
                printf("[KEY] F1 ignored (sensor off)\n");
            }
        }
        if (g_key_flag == 2) {
            g_key_flag = 0;
            if (g_sensor_enabled) {
                alarm_mode = 0;   /* 切回自动 */
                Alarm_Off();
                printf("[KEY] F2: restore auto mode\n");
            }
        }

        /* ---- 每 TICKS_PER_REPORT 个 tick (1s) 做: 传感器+发布 ---- */
        report_counter++;
        if (report_counter >= TICKS_PER_REPORT) {
            report_counter = 0;

            if (g_sensor_enabled) {
                ppm = Get_MQ2_PPM();
                printf("[SMOKE] ppm: %.3f\n", ppm);

                /* 自动报警 (仅在非强制模式下) */
                if (alarm_mode == 0) {
                    if (ppm > g_smoke_threshold) {
                        Alarm_On();
                        printf("[SMOKE] ppm %.1f > %d, alarm ON\n", ppm, g_smoke_threshold);
                    } else {
                        Alarm_Off();
                    }
                }

                /* MQTT 发布 {"ppm": xxx} */
                cJSON *root = cJSON_CreateObject();
                if (root != NULL) {
                    cJSON_AddNumberToObject(root, "ppm", ppm);
                    char *json_str = cJSON_PrintUnformatted(root);

                    MQTTMessage message;
                    message.qos        = 1;
                    message.retained   = 0;
                    message.payload    = json_str;
                    message.payloadlen = strlen(json_str);

                    rc = MQTTPublish(&client, PUB_TOPIC, &message);
                    if (rc != 0) {
                        printf("[MQTT] publish failed: %d\n", rc);
                    } else {
                        printf("[MQTT] publish to '%s': %s\n", PUB_TOPIC, json_str);
                    }

                    cJSON_free(json_str);
                    cJSON_Delete(root);
                }
            }
        }

        usleep(ALARM_BLINK_MS * 1000);   /* 100ms 节拍 */
    }
}

/* ================================================================
 * 入口
 * ================================================================ */
static void SmokeMqttEntry(void)
{
    osThreadAttr_t attr = {0};
    attr.name       = "SmokeMqttTask";
    attr.attr_bits  = 0U;
    attr.cb_mem     = NULL;
    attr.cb_size    = 0U;
    attr.stack_mem  = NULL;
    attr.stack_size = TASK_STACK_SIZE;
    attr.priority   = TASK_PRIO;

    if (osThreadNew((osThreadFunc_t)SmokeMqttTask, NULL, &attr) == NULL) {
        printf("Failed to create SmokeMqttTask!\n");
    }
}

APP_FEATURE_INIT(SmokeMqttEntry);
