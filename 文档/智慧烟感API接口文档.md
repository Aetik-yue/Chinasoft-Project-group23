# 智慧烟感预警系统 - API 接口文档

> 版本：v1.1
> 日期：2026-07-06
> 适用范围：前端与后端/硬件对接
> 维护人：待分配

---

## 0. 文档说明

本文档基于《智慧烟感前端功能与页面草图清单》《智慧烟感主页面待定参数建议表》整理，约定前端与后端之间的全部接口口径。所有接口均走 HTTP，前端**不直连 MySQL、不直连 MQTT 1883**，统一通过后端 API 获取与下发数据。

优先级标识：**P0**=必须完成（主页面演示核心）、**P1**=建议完成、**P2**=加分项。

接口统计：当前文档共 35 个接口；本次新增鹦鹉照护数据接口 15 个。

---

## 1. 概述

### 1.1 基础信息

| 项 | 值 | 说明 |
|---|---|---|
| BaseURL | `http://<服务器IP>:8080/api` | 开发期可使用 `http://localhost:8080/api`；后端未完成时前端用 mock |
| 协议 | HTTP/1.1 | 生产环境建议升级 HTTPS |
| 数据格式 | `application/json; charset=utf-8` | 请求体与响应体统一 JSON |
| 字符编码 | UTF-8 | |
| 时间格式 | `YYYY-MM-DD HH:mm:ss` | 如 `2026-06-30 14:02:11`；时区东八区 |
| 时区 | Asia/Shanghai (UTC+8) | |

### 1.2 鉴权方式

- 登录成功后后端返回 `token`，前端存入 `localStorage`。
- 后续所有业务请求在请求头携带：`Authorization: Bearer <token>`。
- token 过期返回 `401`，前端跳回登录页。
- 演示阶段若后端未做鉴权，可使用 mock 登录，token 固定为 `mock-token`。

### 1.3 请求规范

- GET 请求参数放在 Query String。
- POST/PUT/DELETE 请求参数放在 Body（JSON）。
- 所有接口 URL 以 `/api` 开头（即 BaseURL 已含 `/api`，下文接口路径不再重复写 `/api` 前缀，统一从 `/auth/login` 写起；若后端 BaseURL 不含 `/api`，请整体补回）。

---

## 2. 通用约定

### 2.1 统一响应结构

所有接口返回统一外层包装：

```json
{
  "code": 0,
  "message": "ok",
  "data": { /* 业务数据，可能是对象、数组或 null */ }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 业务状态码，`0` 表示成功，非 `0` 表示失败 |
| message | string | 状态描述，成功为 `ok`，失败为错误信息 |
| data | object/array/null | 业务数据；失败或无数据时为 `null` 或空数组 |

> HTTP 状态码与业务 code 配合使用：HTTP 层表达传输层结果（200/400/401/404/500），业务 code 表达业务语义。前端判断**优先看 code**。

### 2.2 分页参数与响应

列表类接口（告警日志、设备列表等）统一分页：

**请求参数（Query）**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| page | int | 否 | 1 | 页码，从 1 开始 |
| pageSize | int | 否 | 20 | 每页条数 |

**响应 data 结构**

```json
{
  "list": [ /* 记录数组 */ ],
  "total": 128,
  "page": 1,
  "pageSize": 20
}
```

### 2.3 错误码表

#### HTTP 状态码

| HTTP | 含义 | 触发场景 |
|---|---|---|
| 200 | 成功 | 请求处理完成（业务是否成功看 code） |
| 400 | 参数错误 | 缺字段、类型不符、值非法 |
| 401 | 未授权 | 未登录或 token 失效 |
| 403 | 无权限 | 角色无权操作 |
| 404 | 资源不存在 | 设备/告警不存在 |
| 409 | 状态冲突 | 重复处理告警等 |
| 500 | 服务器错误 | 后端异常 |

#### 业务 code

| code | 含义 | 关联接口 |
|---|---|---|
| 0 | 成功 | 全部 |
| 1001 | 参数缺失 | 全部 |
| 1002 | 参数类型错误 | 全部 |
| 1003 | 参数值非法 | 全部 |
| 2001 | 未登录/token 失效 | 全部 |
| 2002 | 无操作权限 | 设备控制/告警处理 |
| 4004 | 资源不存在或不属于当前鹦鹉 | 设备、鹦鹉档案及其体重/病历/账本/相片接口 |
| 4005 | 告警不存在 | alarm/{id}、alarm/handle |
| 5001 | 读取传感器失败 | smoke/latest |
| 5002 | 设备离线无法控制 | device/control |
| 5003 | 告警已处理，不可重复处理 | alarm/handle |
| 5004 | 阈值非法（低风险阈值≥高危阈值等） | settings/threshold |
| 5000 | 服务器内部错误 | 全部 |

---

## 3. 数据字典（枚举值）

> 以下枚举值与《主页面待定参数建议表-字段与状态字典》一致，全项目统一使用。

### 3.1 风险等级 riskLevel

| 编码 | smokeValue 区间 | 页面显示 | 颜色 | 行为 |
|---|---|---|---|---|
| `normal` | 0–100 ppm | 正常 | 绿色 | 安全主题 |
| `low` | 100–200 ppm | 低风险 | 黄绿色/黄色 | 轻度提示，不强制报警 |
| `medium` | 200–400 ppm | 中风险 | 橙色 | 计入今日告警 |
| `high` | >400 ppm | 高风险 | 红色 | 触发危险主题 + 设备联动 |

> 阈值为前端先用展示值，最终以后端/老师确认为准（见待定参数表）。

### 3.2 报警状态 alarmStatus

| 编码 | 页面显示 | 主题 | 说明 |
|---|---|---|---|
| `safe` | 安全 | 绿色山林 | 主卡片只显示状态 |
| `alarm` | 告警中/危险 | 红色岩浆 | 主卡片简化文字 |
| `offline` | 设备离线 | 灰蓝故障 | 可选状态，控制按钮禁用 |

### 3.3 设备状态 deviceStatus

| 编码 | 页面显示 | 颜色 | 说明 |
|---|---|---|---|
| `on` | 开启/运行中 | 绿或红，随主题 | 控制面板开关状态 |
| `off` | 关闭 | 灰色 | 控制面板开关状态 |

### 3.4 处理状态 handleStatus（告警记录）

| 编码 | 页面显示 | 颜色 |
|---|---|---|
| `pending` | 待处理 | 红色/橙色 |
| `processing` | 处理中 | 橙色 |
| `resolved` | 已处理/已恢复 | 绿色 |

### 3.5 设备在线状态 online

| 编码 | 说明 |
|---|---|
| `true` | 在线 |
| `false` | 离线（心跳中断） |

### 3.6 设备类型 deviceType（控制面板）

| 编码 | 说明 |
|---|---|
| `buzzer` | 蜂鸣器 |
| `alarm_light` | 报警灯 |
| `fan` | 排风扇 |

### 3.7 告警类型 alarmType

| 编码 | 说明 |
|---|---|
| `smoke_high` | 烟雾浓度超限 |
| `device_offline` | 设备离线 |

### 3.8 用户角色 userRole

| 编码 | 说明 |
|---|---|
| `admin` | 管理员 |
| `viewer` | 只读用户 |

---

## 4. 接口详情

### 4.1 鉴权模块

---

#### 4.1.1 登录 `POST /auth/login`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 账号密码登录，返回 token 与用户资料。 |
| 鉴权 | 否 |

**请求体**

```json
{
  "account": "admin",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| account | string | 是 | 账号（对应 `sys_user.username`） | `admin` |
| password | string | 是 | 密码（演示阶段明文传输） | `123456` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "smoke-token-1-2026-07-09T00:00:00-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": 1,
    "userRole": "admin",
    "username": "admin",
    "realName": "管理员",
    "phone": null,
    "email": null,
    "location": "重庆市沙坪坝区"
  }
}
```

| 字段 | 类型 | 说明 | 枚举 |
|---|---|---|---|
| token | string | 登录凭证，后续请求放 Header | - |
| userId | long | 当前用户数据库主键 | - |
| userRole | string | 用户角色 | `admin`/`viewer` |
| username | string | 用户名 | - |
| realName | string | 真实姓名 | - |
| phone | string \| null | 绑定手机号，未绑定为 `null` | - |
| email | string \| null | 绑定邮箱，未绑定为 `null` | - |
| location | string \| null | 用户位置信息，未填写为 `null` | - |

**错误码**：`1003` 参数校验失败、`2001` 账号或密码错误。

**备注**：前端校验非空后调用；成功后存 token 并通过 `GET /auth/me` 拉取完整资料。

---

#### 4.1.2 注册 `POST /auth/register`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 注册新账号，成功后直接返回登录凭证（等同于登录，进入系统）。 |
| 鉴权 | 否 |

**请求体**

```json
{
  "account": "newuser",
  "password": "123456",
  "phone": "13823070420"
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| account | string | 是 | 账号，3-64 位，全局唯一 | `newuser` |
| password | string | 是 | 密码，6-64 位 | `123456` |
| phone | string | 否 | 手机号，填了可用于短信登录 | `13823070420` |

**成功响应**：同 4.1.1 登录，`data` 含 `token`。

**错误码**：`1003` 参数校验失败（密码/账号长度不足）、`1201` 该账号已被注册。

---

#### 4.1.3 获取当前用户资料 `GET /auth/me`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 解析登录态，返回当前用户完整资料（手机/邮箱/角色等）。 |
| 鉴权 | 是，Header `Authorization: Bearer <token>` |

**成功响应**：同 4.1.1 登录的 `data` 结构（`token` 字段返回当前 token 原文）。

**错误码**：`2001` 未登录或 token 失效 / 凭证格式错误。

**备注**：设置页通过此接口展示真实用户名、绑定手机号/邮箱与角色，不再写死 mock 数据。

---

#### 4.1.4 更新当前用户资料 `PUT /auth/me`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 更新当前用户的登录账号、手机号、邮箱和位置信息。 |
| 鉴权 | 是，Header `Authorization: Bearer <token>` |

**请求体**

```json
{
  "username": "bird-owner",
  "phone": "13823070420",
  "email": "bird@example.com",
  "location": "重庆市沙坪坝区"
}
```

`username` 必填且长度为 3–64 位；手机号、邮箱和位置可为空，非空手机号必须为 11 位数字，邮箱必须符合格式，位置最长 255 位。用户名修改后，下次登录需使用新用户名，当前 token 仍然有效。

**成功响应**：同 `GET /auth/me`，返回更新后的完整用户资料与当前 token。

**错误码**：`1003` 参数校验失败、`1201` 用户名已被其他账号使用、`2001` 未登录或 token 无效。

---

#### 4.1.5 发送短信验证码 `POST /auth/sms-code`

| 项 | 值 |
|---|---|
| 优先级 | P2 |
| 描述 | 生成 6 位验证码并缓存，供 `sms-login` 校验。 |
| 鉴权 | 否 |

> 演示环境无真实短信网关：验证码打印到后端控制台日志（`[sms-code] phone=... code=...`）。生产环境应替换为 Redis + 真实短信网关并移除日志。

**请求体**

```json
{
  "phone": "13823070420"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号 |

**成功响应**

| 字段 | 类型 | 说明 |
|---|---|---|
| expiresIn | int | 验证码有效期（秒），当前 300s |

**错误码**：`1003` 手机号不能为空。

---

#### 4.1.6 手机验证码登录 `POST /auth/sms-login`

| 项 | 值 |
|---|---|
| 优先级 | P2 |
| 描述 | 校验短信验证码，校验通过后返回登录凭证。 |
| 鉴权 | 否 |

**请求体**

```json
{
  "phone": "13823070420",
  "code": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号 |
| code | string | 是 | 6 位验证码 |

**成功响应**：同 4.1.1 登录。

**错误码**：`1003` 参数校验失败、`2001` 验证码错误或已过期 / 该手机号未注册。

> 演示环境策略：手机号已注册（`sys_user.phone` 有值）则直接登录；未注册则拒绝（避免自动批量注册）。

---

#### 4.1.7 修改密码 `POST /auth/change-password`

| 项 | 值 |
|---|---|
| 优先级 | P2 |
| 描述 | 当前登录用户修改自己的密码：校验当前密码通过后写入新密码。 |
| 鉴权 | 是，Header `Authorization: Bearer <token>` |

**请求体**

```json
{
  "oldPassword": "123456",
  "newPassword": "888888"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| oldPassword | string | 是 | 当前密码，用于校验身份 |
| newPassword | string | 是 | 新密码，6-64 位 |

**成功响应**

```json
{
  "code": 0,
  "message": "OK",
  "data": "密码修改成功"
}
```

**错误码**：`1003` 参数校验失败（新密码长度不足）、`2001` 未登录或 token 失效 / 当前密码错误。

> 前端应在提交前做本地校验：三项均非空、新密码 ≥ 6 位、两次新密码输入一致；成功后提示并清空表单。

---

#### 4.1.8 注销账号 `DELETE /auth/account`

| 项 | 值 |
|---|---|
| 优先级 | P2 |
| 描述 | 注销当前登录账号，级联删除用户偏好、宠物档案及其关联记录（体重/病历/记账/照片），最后删除用户本身。 |
| 鉴权 | 是，Header `Authorization: Bearer <token>` |

**请求**：无请求体。

**成功响应**

```json
{
  "code": 0,
  "message": "OK",
  "data": "账号已注销"
}
```

**错误码**：`2001` 未登录或 token 失效 / 凭证格式错误。

> 该操作不可逆：用户的所有宠物数据与偏好将被永久删除。前端应在调用前弹出确认弹窗，二次确认后再发起请求；成功后清除本地登录态并跳回登录页。

---

### 4.2 系统模块

---

#### 4.2.1 获取系统状态 `GET /system/status`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 获取系统在线状态、当前时间、在线设备数。用于顶部状态栏。 |
| 鉴权 | 是 |

**请求参数**：无。

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "systemOnline": true,
    "currentTime": "2026-06-30 14:02:11",
    "onlineDeviceCount": 12,
    "totalDeviceCount": 12
  }
}
```

| 字段 | 类型 | 说明 | 枚举/范围 |
|---|---|---|---|
| systemOnline | bool | 系统是否在线 | `true`/`false` |
| currentTime | string | 系统当前时间 | `YYYY-MM-DD HH:mm:ss` |
| onlineDeviceCount | int | 在线设备数 | 0–totalDeviceCount |
| totalDeviceCount | int | 设备总数 | ≥0 |

**备注**：前端页面加载时拉取一次；时间可由前端时钟定时器本地更新，每 30s–60s 同步一次后端时间校准。

---

### 4.3 烟雾数据模块

---

#### 4.3.x 获取实时浓度+温度+湿度 `GET /smoke/realtime`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 实时浓度 + 温度 + 湿度，前端每 3 秒轮询一次，驱动环境指标卡片 |
| 鉴权 | 是 |
| 调用频率 | 前端每 3 秒轮询一次 |

**请求参数（Query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 否 | 设备编号，不传则主设备 | `SMK-001` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "deviceId": "SMK-001",
    "smokeValue": 86,
    "riskLevel": "low",
    "temperature": 25.3,
    "humidity": 49.8,
    "recordTime": "2026-07-08 14:02:11"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| deviceId | string | 设备编号 |
| smokeValue | float | 当前浓度（ppm） |
| riskLevel | string | 风险等级：`normal`/`low`/`medium`/`high` |
| temperature | float/null | 当前温度（℃），随 temperature_data 最新值返回；未接入时为 null |
| humidity | float/null | 当前湿度（%RH），随 humidity_data 最新值返回；未接入时为 null |
| recordTime | string | 数据更新时间 |

**备注**：temperature 与 humidity 分别来自 `temperature_data` 与 `humidity_data` 表最新一条；没有独立端点，随 smoke 聚合返回。

---

#### 4.3.1 获取最新烟雾浓度与状态 `GET /smoke/latest`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 获取指定设备最新烟雾浓度、风险等级、报警状态。首页"当前浓度卡片""风险等级卡片""报警状态卡片"共用此接口。 |
| 鉴权 | 是 |
| 调用频率 | 前端每 3 秒轮询一次 |

**请求参数（Query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 否 | 设备编号，不传则返回主设备 | `SMK-001` |

**请求示例**

```
GET /smoke/latest?deviceId=SMK-001
Authorization: Bearer <token>
```

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "deviceId": "SMK-001",
    "smokeValue": 86,
    "unit": "ppm",
    "riskLevel": "low",
    "riskScore": 86,
    "alarmStatus": "safe",
    "alarmType": null,
    "updatedAt": "2026-06-30 14:02:11"
  }
}
```

| 字段 | 类型 | 说明 | 枚举/范围 |
|---|---|---|---|
| deviceId | string | 设备编号 | `SMK-001` |
| smokeValue | int | 当前浓度，整数显示 | 0–999 ppm |
| unit | string | 浓度单位，全项目统一 | `ppm` |
| riskLevel | string | 风险等级 | `normal`/`low`/`medium`/`high` |
| riskScore | int | 风险分值 | 0–100 |
| alarmStatus | string | 报警状态 | `safe`/`alarm`/`offline` |
| alarmType | string/null | 告警类型，无告警为 null | `smoke_high`/`device_offline`/`null` |
| updatedAt | string | 数据更新时间 | `YYYY-MM-DD HH:mm:ss` |

**业务规则**

- 风险等级由 smokeValue 按阈值映射（见 3.1）；阈值最终以后端/老师确认为准。
- `alarmStatus=alarm` 时前端整体切换危险主题（红色岩浆背景）。
- `alarmStatus=offline` 时禁用设备控制按钮，显示"设备离线"。

**错误码**：`4004` 设备不存在、`5001` 读取传感器失败（前端降级为保留上一次数据 + 角标显示离线）。

---

#### 4.3.2 获取历史浓度趋势 `GET /smoke/history`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 获取历史烟雾浓度数据，用于 ECharts 折线图。支持首页时间范围切换与历史记录页查询。 |
| 鉴权 | 是 |

**请求参数（Query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 否 | 设备编号，不传则主设备 | `SMK-001` |
| range | string | 否 | 预设时间范围，二选一与 start/end | `24h` |
| start | string | 否 | 自定义起始时间 | `2026-06-29 00:00:00` |
| end | string | 否 | 自定义结束时间 | `2026-06-30 00:00:00` |

range 可选值：`realtime`（实时，最近 30 分钟）、`6h`、`12h`、`24h`（默认）、`7d`。

> `range` 与 `start/end` 二选一：首页用 `range` 快捷切换；历史记录页用 `start/end` 自定义。

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "deviceId": "SMK-001",
    "range": "24h",
    "interval": "hour",
    "threshold": {
      "warning": 200,
      "danger": 400
    },
    "records": [
      { "time": "2026-06-29 14:00:00", "value": 32 },
      { "time": "2026-06-29 15:00:00", "value": 41 }
    ]
  }
}
```

| 字段 | 类型 | 说明 | 枚举/范围 |
|---|---|---|---|
| deviceId | string | 设备编号 | - |
| range | string | 实际使用的时间范围 | 同请求 range |
| interval | string | 聚合粒度 | `second`/`minute`/`hour`/`day` |
| threshold.warning | int | 风险阈值（中风险下限） | ppm |
| threshold.danger | int | 高危阈值 | ppm |
| records[].time | string | 数据点时间 | `YYYY-MM-DD HH:mm:ss` |
| records[].value | int | 该时刻浓度 | 0–999 ppm |

**业务规则**

- 主页面默认 `range=24h`、`interval=hour`；7d 模式 `interval=day`；实时模式 `interval=minute` 且返回最近 60 个点。
- 折线图叠加 `threshold.warning` 与 `threshold.danger` 两条阈值线。
- 安全状态绿色折线，危险状态红色折线。

**错误码**：`4004` 设备不存在。

---

#### 4.3.3 模拟烟雾升高 `POST /smoke/simulate`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 触发危险场景，用于课堂演示。后端生成高浓度数据并触发告警。 |
| 鉴权 | 是 |

**请求体**

```json
{
  "deviceId": "SMK-001",
  "scenario": "smoke_high"
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 否 | 设备编号，不传则主设备 | `SMK-001` |
| scenario | string | 是 | 模拟场景 | `smoke_high` |

scenario 可选值：`smoke_high`（烟雾升高）、`device_offline`（设备离线，可选）。

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "deviceId": "SMK-001",
    "smokeValue": 520,
    "riskLevel": "high",
    "alarmStatus": "alarm",
    "alarmType": "smoke_high",
    "updatedAt": "2026-06-30 14:02:15"
  }
}
```

**业务规则**

- 调用后前端刷新 `/smoke/latest`、`/smoke/history`、`/alarm/logs`、`/alarm/stat/today`。
- 整体切换危险主题，新增一条告警记录。
- 设备联动：蜂鸣器/报警灯/排风扇自动开启（见 4.5.1）。

**错误码**：`4004` 设备不存在。

---

#### 4.3.4 恢复正常环境 `POST /smoke/restore`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 解除模拟危险场景，恢复低浓度并解除告警。 |
| 鉴权 | 是 |

**请求体**

```json
{
  "deviceId": "SMK-001",
  "scenario": "normal"
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 否 | 设备编号 | `SMK-001` |
| scenario | string | 是 | 固定 `normal` | `normal` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "deviceId": "SMK-001",
    "smokeValue": 35,
    "riskLevel": "normal",
    "alarmStatus": "safe",
    "alarmType": null,
    "updatedAt": "2026-06-30 14:02:20"
  }
}
```

**业务规则**

- 调用后前端整体切回安全主题，设备状态恢复。
- 后端将当前告警记录置为 `resolved`。

**错误码**：`4004` 设备不存在。

---

### 4.4 告警模块

---

#### 4.4.1 今日告警统计 `GET /alarm/stat/today`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 获取今日告警次数与较昨日变化，用于"今日告警卡片"。 |
| 鉴权 | 是 |

**请求参数**：无。

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "todayCount": 3,
    "yesterdayCount": 5,
    "changeRate": -40,
    "offlineCount": 1
  }
}
```

| 字段 | 类型 | 说明 | 枚举/范围 |
|---|---|---|---|
| todayCount | int | 今日告警次数 | ≥0 |
| yesterdayCount | int | 昨日告警次数 | ≥0 |
| changeRate | int | 较昨日变化百分比 | -100–∞ |
| offlineCount | int | 今日设备离线次数（单独统计） | ≥0 |

**业务规则**

- 统计口径：今日 00:00 至当前时间（自然日）。
- 统计对象：烟雾中风险（≥200 ppm）及以上告警。
- 设备离线不纳入 todayCount，单独计 offlineCount。
- 无昨日数据时 changeRate 可为 null，前端隐藏该字段。

---

#### 4.4.2 告警列表 `GET /alarm/logs`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 获取告警记录列表。首页底部表格只显示最近 5 条；告警日志页支持分页与筛选。 |
| 鉴权 | 是 |

**请求参数（Query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| page | int | 否 | 页码，默认 1 | `1` |
| pageSize | int | 否 | 每页条数，默认 20；首页传 5 | `5` |
| deviceId | string | 否 | 按设备筛选 | `SMK-001` |
| level | string | 否 | 按风险等级筛选 | `high` |
| status | string | 否 | 按处理状态筛选 | `pending` |
| start | string | 否 | 起始时间 | `2026-06-29 00:00:00` |
| end | string | 否 | 结束时间 | `2026-06-30 00:00:00` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "alarmId": "ALM-20260630-001",
        "alarmTime": "2026-06-30 13:45:22",
        "deviceId": "SMK-001",
        "alarmType": "smoke_high",
        "smokeValue": 520,
        "level": "high",
        "status": "pending",
        "handler": null,
        "remark": null
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 5
  }
}
```

| 字段 | 类型 | 说明 | 枚举/范围 |
|---|---|---|---|
| alarmId | string | 告警 ID | `ALM-YYYYMMDD-NNN` |
| alarmTime | string | 告警时间 | `YYYY-MM-DD HH:mm:ss` |
| deviceId | string | 设备编号 | - |
| alarmType | string | 告警类型 | `smoke_high`/`device_offline` |
| smokeValue | int | 触发时浓度 | 0–999 ppm |
| level | string | 风险等级 | `normal`/`low`/`medium`/`high` |
| status | string | 处理状态 | `pending`/`processing`/`resolved` |
| handler | string/null | 处理人，未处理为 null | - |
| remark | string/null | 处理备注 | - |

**业务规则**

- 按时间倒序返回。
- 首页表格字段：时间、设备编号、类型、浓度、等级、状态、操作（查看详情）。

**错误码**：无特殊。

---

#### 4.4.3 告警详情 `GET /alarm/{id}`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 获取单条告警详情，含处理记录。 |
| 鉴权 | 是 |

**路径参数**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| id | string | 是 | 告警 ID | `ALM-20260630-001` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "alarmId": "ALM-20260630-001",
    "alarmTime": "2026-06-30 13:45:22",
    "deviceId": "SMK-001",
    "alarmType": "smoke_high",
    "smokeValue": 520,
    "level": "high",
    "status": "pending",
    "handler": null,
    "remark": null,
    "timeline": [
      { "time": "2026-06-30 13:45:22", "event": "告警触发" },
      { "time": "2026-06-30 13:45:23", "event": "设备联动启动" }
    ],
    "chart": {
      "start": "2026-06-30 13:30:00",
      "end": "2026-06-30 14:00:00",
      "records": [
        { "time": "2026-06-30 13:30:00", "value": 40 },
        { "time": "2026-06-30 13:45:22", "value": 520 }
      ]
    }
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| alarmId ~ remark | - | 同告警列表单条 |
| timeline | array | 处理时间线 |
| timeline[].time | string | 事件时间 |
| timeline[].event | string | 事件描述 |
| chart | object | 告警时段浓度曲线片段 |
| chart.start/end | string | 曲线起止时间 |
| chart.records[].time/value | string/int | 数据点 |

**错误码**：`4005` 告警不存在。

---

#### 4.4.4 处理告警 `POST /alarm/handle`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 标记告警为已处理，填写处理人备注。答辩管理闭环关键接口。 |
| 鉴权 | 是 |

**请求体**

```json
{
  "alarmId": "ALM-20260630-001",
  "status": "resolved",
  "handler": "admin",
  "remark": "现场排查为误报，已恢复"
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| alarmId | string | 是 | 告警 ID | `ALM-20260630-001` |
| status | string | 是 | 目标处理状态 | `processing`/`resolved` |
| handler | string | 是 | 处理人 | `admin` |
| remark | string | 否 | 处理备注 | `现场排查为误报` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "alarmId": "ALM-20260630-001",
    "status": "resolved",
    "handler": "admin",
    "handledAt": "2026-06-30 14:00:00"
  }
}
```

**业务规则**

- 处理后前端表格状态变为对应状态。
- 已 `resolved` 的告警再次处理返回 `5003`。

**错误码**：`4005` 告警不存在、`5003` 告警已处理不可重复、`2002` 无权限。

---

### 4.5 设备模块

---

#### 4.5.1 设备控制 `POST /device/control`

| 项 | 值 |
|---|---|
| 优先级 | P0 |
| 描述 | 控制蜂鸣器/报警灯/排风扇开关。后端经 MQTT 下发或修改模拟状态。 |
| 鉴权 | 是 |

**请求体**

```json
{
  "deviceId": "SMK-001",
  "deviceType": "fan",
  "status": "on"
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 是 | 设备编号 | `SMK-001` |
| deviceType | string | 是 | 设备类型 | `buzzer`/`alarm_light`/`fan` |
| status | string | 是 | 目标状态 | `on`/`off` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "deviceId": "SMK-001",
    "deviceType": "fan",
    "status": "on",
    "executedAt": "2026-06-30 14:02:30"
  }
}
```

**业务规则**

- 前端点击开关 → 显示"执行中" → 成功后更新开关状态 → toast 提示成功。
- 失败时前端回滚开关状态并提示失败。
- 设备离线时返回 `5002`，控制按钮禁用。
- 危险状态触发时，蜂鸣器/报警灯/排风扇由后端自动联动开启（不依赖前端手动）。

**错误码**：`4004` 设备不存在、`5002` 设备离线无法控制、`2002` 无权限。

---

#### 4.5.2 设备列表 `GET /devices`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 获取全部设备列表，用于实时监控页、设备管理页。 |
| 鉴权 | 是 |

**请求参数（Query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| online | bool | 否 | 按在线状态筛选 | `true` |
| keyword | string | 否 | 按名称/位置搜索 | `仓库` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "deviceId": "SMK-001",
        "name": "1号仓库烟感",
        "location": "A区1号仓库",
        "online": true,
        "smokeValue": 86,
        "enabled": true
      }
    ],
    "total": 1
  }
}
```

| 字段 | 类型 | 说明 | 枚举/范围 |
|---|---|---|---|
| deviceId | string | 设备编号 | `SMK-001` |
| name | string | 设备名称 | - |
| location | string | 安装位置 | - |
| online | bool | 是否在线 | `true`/`false` |
| smokeValue | int | 最新浓度 | 0–999 ppm |
| enabled | bool | 是否启用 | `true`/`false` |

**业务规则**

- 单设备场景可简化，直接返回主设备。

---

#### 4.5.3 设备状态 `GET /device/status`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 获取设备在线状态与各受控设备开关状态。用于心跳检测与控制面板状态回显。 |
| 鉴权 | 是 |

**请求参数（Query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 否 | 设备编号，不传则主设备 | `SMK-001` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "deviceId": "SMK-001",
    "online": true,
    "lastHeartbeat": "2026-06-30 14:02:11",
    "controls": [
      { "deviceType": "buzzer", "status": "off" },
      { "deviceType": "alarm_light", "status": "off" },
      { "deviceType": "fan", "status": "off" }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| deviceId | string | 设备编号 |
| online | bool | 是否在线 |
| lastHeartbeat | string | 最近心跳时间 |
| controls[].deviceType | string | 受控设备类型 |
| controls[].status | string | 当前开关状态 |

**业务规则**

- `online=false` 时前端顶部/设备卡片显示离线，控制按钮禁用，显示"状态未知"。

**错误码**：`4004` 设备不存在。

---

#### 4.5.4 新增设备 `POST /devices`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 新增/绑定烟感设备。 |
| 鉴权 | 是 |

**请求体**

```json
{
  "deviceId": "SMK-002",
  "name": "2号仓库烟感",
  "location": "A区2号仓库",
  "enabled": true
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| deviceId | string | 是 | 设备编号 | `SMK-002` |
| name | string | 是 | 设备名称 | `2号仓库烟感` |
| location | string | 否 | 安装位置 | `A区2号仓库` |
| enabled | bool | 否 | 是否启用，默认 true | `true` |

**成功响应**：返回新增的设备对象（同 4.5.2 单条）。

**错误码**：`1003` 设备编号已存在。

---

#### 4.5.5 编辑设备 `PUT /devices/{deviceId}`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 编辑设备名称、位置、启用状态。 |
| 鉴权 | 是 |

**请求体**：`{ "name": "...", "location": "...", "enabled": true }`（字段均可选）。

**成功响应**：返回更新后的设备对象。

**错误码**：`4004` 设备不存在。

---

#### 4.5.6 删除/解绑设备 `DELETE /devices/{deviceId}`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 解绑设备。 |
| 鉴权 | 是 |

**成功响应**：`{ "code": 0, "message": "ok", "data": null }`

**错误码**：`4004` 设备不存在。

---

### 4.6 系统设置模块

---

#### 4.6.1 阈值配置 `GET /settings/threshold`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 获取当前风险阈值与高危阈值。 |
| 鉴权 | 是 |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "warningThreshold": 200,
    "dangerThreshold": 400,
    "unit": "ppm"
  }
}
```

| 字段 | 类型 | 说明 | 范围 |
|---|---|---|---|
| warningThreshold | int | 风险阈值（中风险下限） | >0 |
| dangerThreshold | int | 高危阈值 | >warningThreshold |
| unit | string | 单位 | `ppm` |

---

#### 4.6.2 保存阈值配置 `POST /settings/threshold`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 修改风险阈值与高危阈值。 |
| 鉴权 | 是 |

**请求体**

```json
{
  "warningThreshold": 200,
  "dangerThreshold": 400
}
```

**成功响应**：返回更新后的阈值对象（同 4.6.1）。

**业务规则**

- `dangerThreshold` 必须大于 `warningThreshold`，否则返回 `5004`。
- 保存后前端提示成功。

**错误码**：`5004` 阈值非法。

---

### 4.7 用户偏好模块

> 当前鉴权暂未接入，后端固定使用 `userId = 1` 读写用户偏好。后续接入登录后，将由后端从登录态中解析当前用户 ID。

---

#### 4.7.1 查询用户偏好 `GET /user/preferences`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 查询当前用户的页面偏好配置，用于设置页初始化。 |
| 鉴权 | 暂无，当前固定 `userId=1` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": 1,
    "language": "zh",
    "theme": "light",
    "fontFamily": "default",
    "fontSize": 16,
    "fontColor": "black",
    "notificationEnabled": true,
    "permissionEnabled": true,
    "avatarParrotId": null,
    "updatedAt": "2026-07-08T12:00:00"
  }
}
```

| 字段 | 类型 | 说明 | 默认值 |
|---|---|---|---|
| userId | long | 当前占位用户 ID | `1` |
| language | string | 语言：`zh` / `en` / `es` / `ja` | `zh` |
| theme | string | 主题：`light` / `dark` | `light` |
| fontFamily | string | 字体 | `default` |
| fontSize | int | 字号，单位 pt | `16` |
| fontColor | string | 字体颜色 | `black` |
| notificationEnabled | boolean | 通知开关 | `true` |
| permissionEnabled | boolean | 设备权限提示开关 | `true` |
| avatarParrotId | string/null | 设置页头像鹦鹉 ID | `null` |
| updatedAt | string/null | 最近一次偏好更新时间 | `null` |

---

#### 4.7.2 保存用户偏好 `PUT /user/preferences`

| 项 | 值 |
|---|---|
| 优先级 | P1 |
| 描述 | 保存当前用户的页面偏好配置。支持部分更新，未传字段保留原值；若数据库无记录，则读取时使用默认值。 |
| 鉴权 | 暂无，当前固定 `userId=1` |

**请求体**

```json
{
  "language": "zh",
  "theme": "dark",
  "fontFamily": "default",
  "fontSize": 18,
  "fontColor": "black",
  "notificationEnabled": true,
  "permissionEnabled": true,
  "avatarParrotId": "PET-xxxx"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| language | string | 否 | 语言：`zh` / `en` / `es` / `ja` |
| theme | string | 否 | 主题：`light` / `dark` |
| fontFamily | string | 否 | 字体，保存前去除首尾空格 |
| fontSize | int | 否 | 字号，范围 `12-28` |
| fontColor | string | 否 | 字体颜色，保存前去除首尾空格 |
| notificationEnabled | boolean | 否 | 通知开关 |
| permissionEnabled | boolean | 否 | 设备权限提示开关 |
| avatarParrotId | string | 否 | 设置页头像鹦鹉 ID；传空字符串可清空 |

**成功响应**：返回更新后的完整偏好对象，结构同 4.7.1。

**业务规则**

- 偏好数据保存到 `user_preference` 表。
- 当前固定写入 `user_id=1`。
- 每个偏好项按唯一键 `(user_id, pref_key)` 更新或新增。
- 布尔值保存为字符串 `"true"` / `"false"`，字号保存为字符串，例如 `"18"`。
- 手机号、邮箱、用户名等账号资料不走该接口，后续应由 `sys_user` 相关接口维护。

**错误码**：`1003` 参数非法，例如语言、主题或字号超出范围。

---

### 4.8 智能问答模块（加分项）

---

#### 4.8.1 智能问答 `POST /agent/chat`

| 项 | 值 |
|---|---|
| 优先级 | P2 |
| 描述 | 报警应急建议与知识库问答。可接 MaxKB/RAG，前端先留入口。 |
| 鉴权 | 是 |

**请求体**

```json
{
  "question": "烟雾浓度超标应该如何处理？",
  "alarmId": "ALM-20260630-001"
}
```

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| question | string | 是 | 用户问题 | `烟雾浓度超标应该如何处理？` |
| alarmId | string | 否 | 关联告警 ID，提供上下文 | `ALM-20260630-001` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "answer": "1. 立即确认现场是否有明火...",
    "citations": [
      { "title": "烟感应急预案", "source": "知识库", "url": "..." }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| answer | string | 回答内容 |
| citations[].title | string | 引用来源标题 |
| citations[].source | string | 来源 |
| citations[].url | string | 来源链接 |

**业务规则**

- 前端展示 loading，未接入智能体时返回固定提示文案。

---

### 4.8 视觉复核模块（加分项）

---

#### 4.8.1 视觉复核 `GET /vision/check`

| 项 | 值 |
|---|---|
| 优先级 | P2 |
| 描述 | 报警后查看摄像头截图与 AI 识别结果，支持人工确认。对接 SmartJavaAI 后再做。 |
| 鉴权 | 是 |

**请求参数（Query）**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|---|---|---|---|---|
| alarmId | string | 是 | 关联告警 ID | `ALM-20260630-001` |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "alarmId": "ALM-20260630-001",
    "imageUrl": "https://.../snapshot/20260630134522.jpg",
    "result": "smoke_detected",
    "confidence": 0.92,
    "confirmed": false
  }
}
```

| 字段 | 类型 | 说明 | 枚举/范围 |
|---|---|---|---|
| alarmId | string | 告警 ID | - |
| imageUrl | string | 摄像头截图 URL | - |
| result | string | AI 识别结果 | `smoke_detected`/`fire_detected`/`none` |
| confidence | float | 置信度 | 0–1 |
| confirmed | bool | 是否人工确认 | `true`/`false` |

**业务规则**

- 报警触发后自动调用，前端展示复核结果；可人工确认真实/误报。

---

### 4.9 鹦鹉照护数据模块

> 本模块已接入 MySQL。当前尚未实现登录鉴权，档案与账本的 `userId` 由后端固定为 `1`。日期使用 `YYYY-MM-DD`，日期时间使用 ISO 8601 格式 `YYYY-MM-DDTHH:mm:ss`。
>
> **相片存储策略（2026-07-08 更新）**：支持两种入库方式：
> - **fileUrl**：外部 URL，后端仅存元数据（与旧版本兼容）
> - **imageBase64**：前端 JPEG 压缩后直接传 base64，后端存入 `image_data` LONGTEXT 列
>
> 两者至少填一个。**截图（screenshot）上限 30 张/鹦鹉**，超出后由后端自动删除最旧的（行车记录仪模式）。

#### 4.9.1 接口总览

| 功能 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 档案列表 | GET | `/parrots` | 查询 `enabled=true` 的鹦鹉档案 |
| 档案详情 | GET | `/parrots/{petId}` | 按业务编号查询档案 |
| 新增档案 | POST | `/parrots` | 后端生成 `PET-UUID`；可同时写入初始体重 |
| 编辑档案 | PUT | `/parrots/{petId}` | 局部更新档案，不通过此接口修改体重 |
| 体重列表 | GET | `/parrots/{petId}/weights` | 按测量时间倒序 |
| 新增体重 | POST | `/parrots/{petId}/weights` | 写入体重并同步档案当前体重 |
| 修改体重 | PUT | `/parrots/{petId}/weights/{id}` | 修改后重新同步档案当前体重 |
| 病历列表 | GET | `/parrots/{petId}/medical-records` | 按记录日期倒序 |
| 新增病历 | POST | `/parrots/{petId}/medical-records` | 后端生成 `MED-UUID` |
| 修改病历 | PUT | `/parrots/{petId}/medical-records/{recordId}` | `recordId` 保持不变 |
| 账本列表 | GET | `/parrots/{petId}/ledger-records` | 按消费日期倒序 |
| 新增账本 | POST | `/parrots/{petId}/ledger-records` | 后端生成 `LED-UUID` |
| 修改账本 | PUT | `/parrots/{petId}/ledger-records/{ledgerId}` | `ledgerId` 保持不变 |
| 相片列表 | GET | `/parrots/{petId}/photos` | 仅返回 `photo` 和 `screenshot` |
| 插入相片 | POST | `/parrots/{petId}/photos` | 后端生成 `MEDIA-UUID`；支持 fileUrl 或 imageBase64 两种入库方式；截图满 30 张自动删最旧 |
| 删除相片 | DELETE | `/parrots/{petId}/photos/{mediaId}` | 物理删除相片元数据记录（含 image_data） |

#### 4.9.2 新增鹦鹉档案 `POST /parrots`

```json
{
  "name": "农药",
  "species": "太阳锥尾鹦鹉",
  "birthday": "2024-05-18",
  "sex": "unknown",
  "initialWeightGrams": 78.5,
  "cageId": null,
  "deviceId": "SMK-001",
  "featherColor": "橙黄色",
  "sterilized": false,
  "avatarUrl": null,
  "currentStatus": "standing",
  "remark": null
}
```

| 字段 | 类型 | 必填 | 规则 |
|---|---|---|---|
| name | string | 是 | 非空，最长 64 |
| species | string | 是 | 非空，最长 64 |
| birthday | date | 否 | 不得晚于今天 |
| sex | string | 否 | `male`/`female`/`unknown`，默认 `unknown` |
| initialWeightGrams | decimal | 否 | 大于 0；填写时同步新增首条体重记录 |
| cageId/deviceId | string | 否 | 最长 64 |
| featherColor | string | 否 | 最长 64 |
| sterilized | bool | 否 | 默认 `false` |
| avatarUrl/remark | string | 否 | 最长 500 |
| currentStatus | string | 否 | 最长 32 |

成功响应 `data` 为档案对象，主要字段包括 `petId`、`userId`、上述档案字段、`weightGrams`、`enabled`、`createdAt`、`updatedAt`。

#### 4.9.3 查询和编辑档案

- `GET /parrots`：`data` 为档案对象数组。
- `GET /parrots/{petId}`：`data` 为单个档案对象。
- `PUT /parrots/{petId}`：请求字段与新增档案基本一致，全部可选；使用 `weight` 接口修改体重。额外支持 `enabled`，不允许修改 `petId`、`userId` 和 `weightGrams`。

#### 4.9.4 体重记录

新增与修改使用相同请求体：

```json
{
  "weightGrams": 79.2,
  "measuredAt": "2026-07-06T14:30:00",
  "source": "manual",
  "remark": "晚饭前称重"
}
```

| 字段 | 类型 | 必填 | 规则 |
|---|---|---|---|
| weightGrams | decimal | 是 | 大于 0 |
| measuredAt | datetime | 是 | 不得晚于当前时间 |
| source | string | 否 | 默认 `manual`，最长 32 |
| remark | string | 否 | 最长 500 |

响应字段：`id`、`petId`、`weightGrams`、`measuredAt`、`source`、`remark`、`createdAt`。

#### 4.9.5 病历记录

新增与修改使用相同请求体：

```json
{
  "recordDate": "2026-07-06",
  "recordType": "symptom",
  "title": "食量下降",
  "content": "今日进食量明显下降，精神状态一般。",
  "hospitalName": null,
  "hospitalPhone": null,
  "attachments": ["https://example.com/medical/photo-1.jpg"]
}
```

`recordDate` 和 `content` 必填；`recordDate` 不得晚于今天。`recordType` 支持 `symptom`、`diagnosis`、`medication`、`recheck`、`other`，默认 `symptom`。响应额外包含后端生成且修改时保持不变的 `recordId`。

#### 4.9.6 账本记录

新增与修改使用相同请求体：

```json
{
  "expenseDate": "2026-07-06",
  "category": "主粮",
  "description": "补充鹦鹉主粮",
  "amount": 88.00,
  "currency": "CNY"
}
```

`expenseDate`、`description`、`amount` 必填；日期不得晚于今天，金额必须大于 0。`category` 默认“其他”，`currency` 默认 `CNY` 且必须为 3 位代码。响应额外包含 `ledgerId`、固定值 `userId=1`、`createdAt`、`updatedAt`。

#### 4.9.7 相片记录

插入相片元数据（支持 fileUrl 或 imageBase64 两种方式）：

```json
{
  "mediaType": "screenshot",
  "title": "监控截图",
  "fileUrl": null,
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQ...",
  "thumbnailUrl": null,
  "tags": "监控,站立",
  "cageId": null,
  "capturedAt": "2026-07-08T14:30:00"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| mediaType | string | 是 | `photo` 或 `screenshot`，默认 `photo` |
| title | string | 否 | 最长 128 |
| fileUrl | string | 否 | 外部资源 URL（与 imageBase64 至少填一个） |
| imageBase64 | string | 否 | JPEG base64（与 fileUrl 至少填一个），存 `image_data` LONGTEXT |
| thumbnailUrl | string | 否 | 缩略图 URL |
| tags | string | 否 | 逗号分隔标签 |
| cageId | string | 否 | 关联笼舍 |
| capturedAt | datetime | 是 | 拍摄时间，不得晚于当前时间 |

**业务规则**

- `fileUrl` 与 `imageBase64` 至少填一个，两者同时填时优先使用 `imageBase64`。
- **截图上限**：`screenshot` 类型单只鹦鹉最多保留 30 张；超出后后端自动删除最旧的（行车记录仪模式）。
- `photo` 类型无上限。
- 删除成功响应包含 `mediaId`、`petId`、`deletedAt`；接口删除数据库记录（含 `image_data`），不负责删除外部 URL 指向的文件。

**通用业务规则**

- 操作体重、病历、账本或相片前，后端会验证 `petId` 对应档案存在。
- 修改或删除子记录时会校验记录属于路径中的 `petId`，归属错误按 404 处理。
- 业务编号均由后端生成，前端不得自行生成或修改。

---

## 5. 接口与前端交互对照

> 取自《交互与接口约定》sheet，便于联调时定位。

| 场景 | 前端行为 | 后端行为 | 接口 | 成功 UI 变化 | 失败兜底 |
|---|---|---|---|---|---|
| 页面初始化 | 拉取最新数据/状态/历史/日志 | 查库返回 | `/smoke/latest` `/system/status` `/smoke/history` `/device/status` `/alarm/logs` | 渲染全部模块 | mock 数据 + 异常提示 |
| 定时刷新(3s) | 轮询最新浓度与状态 | 返回最新 | `/smoke/latest` | 更新卡片与图表最新点 | 保留上次数据 + 离线角标 |
| 模拟烟雾升高 | 按钮 loading | 生成高浓度+告警 | `/smoke/simulate` | 切危险主题 + 新增告警 | 本地 mock 一条危险数据 |
| 恢复正常 | 调用恢复 | 恢复低浓度+解除告警 | `/smoke/restore` | 切安全主题 + 设备恢复 | 本地 mock 正常数据 |
| 设备控制 | 发送控制命令 | MQTT 下发或改模拟态 | `/device/control` | 开关状态变化 + toast | 回滚开关 + 提示失败 |
| 查看告警详情 | 打开详情弹窗 | 返回详情与处理记录 | `/alarm/{id}` | 展示曲线片段与状态 | 展示基本表格数据 |
| 处理告警 | 弹备注框提交 | 更新告警状态 | `/alarm/handle` | 表格状态变已处理 | 提示失败保持原状态 |
| 查询历史 | 调历史接口重绘图表 | 按条件查库 | `/smoke/history` | 图表与明细表刷新 | 提示无数据 |
| 设备离线 | 顶部/卡片显示离线 | 心跳中断触发 | `/device/status` | 红色离线 + 按钮禁用 | 显示"状态未知" |
| 智能问答 | 提交问题 + loading | MaxKB/RAG 返回 | `/agent/chat` | 展示应急建议 | 提示暂未接入智能体 |
| 鹦鹉档案 | 页面加载、新增、编辑 | 查询或保存 `pet_profile` | `/parrots` | 刷新档案卡片 | 保留表单并提示失败 |
| 体重记录 | 查看曲线、新增、修改 | 保存 `pet_weight_record` 并同步档案当前体重 | `/parrots/{petId}/weights` | 刷新当前体重和曲线 | 保留旧数据并提示失败 |
| 病历记录 | 查看、新增、修改 | 查询或保存 `pet_medical_record` | `/parrots/{petId}/medical-records` | 刷新病历列表 | 保留表单并提示失败 |
| 饲养账本 | 查看、新增、修改 | 查询或保存 `pet_ledger_record` | `/parrots/{petId}/ledger-records` | 刷新账本和合计 | 保留表单并提示失败 |
| 相片记录 | 加载、插入、删除 | 操作 `pet_media_record`（fileUrl 或 imageBase64），截图上限 30 张 | `/parrots/{petId}/photos` | 刷新相册 | 回滚删除并提示失败 |

---

## 6. 待确认事项

> 取自《参数确认清单》，以下事项需后端/老师/全组确认后回填本文档。

| # | 待确认事项 | 建议选择 | 需谁确认 | 是否阻塞前端 |
|---|---|---|---|---|
| 1 | 烟雾浓度单位 | ppm | 全组/老师/后端 | 是 |
| 2 | 风险等级阈值 | 0-100/100-200/200-400/400+ | 老师/后端 | 半阻塞（前端先用 mock） |
| 3 | 主页面默认时间范围 | 近 24 小时 | 前端/全组 | 否 |
| 4 | 趋势图聚合粒度 | 主页面按小时，实时页按分钟 | 前端/后端 | 半阻塞 |
| 5 | 今日告警统计口径 | 今日 0 点起，中风险及以上 | 前端/后端 | 半阻塞 |
| 6 | 背景切换状态 | 安全/危险/离线三套主题 | 前端 | 否 |
| 7 | 控制面板设备 | 蜂鸣器/报警灯/排风扇 | 硬件/后端/前端 | 半阻塞 |
| 8 | 登录页是否做 | 做简单静态登录 | 全组 | 否 |
| 9 | 前端是否直连 MQTT | 不直连，走后端 API | 后端/前端 | 否 |
| 10 | 表格默认显示条数 | 最近 5 条 | 前端 | 否 |

---

## 7. 变更记录

| 版本 | 日期 | 变更内容 | 维护人 |
|---|---|---|---|
| v1.0 | 2026-06-30 | 初版，基于前端功能清单与待定参数建议表整理 17 个接口 | - |
| v1.2 | 2026-07-08 | 新增 `GET /smoke/realtime` P0 接口；相片接口支持 imageBase64 存库 + 截图 30 张上限；新增 "相片存储策略" 章节 |
| v1.1 | 2026-07-06 | 新增鹦鹉档案、体重、病历、账本和相片共 15 个后端接口 | - |
