# 登录接口对接说明

前端统一通过 Vite 代理访问 `/api`，后端本地服务默认应监听 `http://localhost:8080`。

## 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

`code = 0` 表示成功；非 0 时前端会把 `message` 显示为登录错误。

## 账号密码登录

`POST /api/auth/login`

请求体：

```json
{
  "account": "admin",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 账号（对应 `sys_user.username`） |
| password | string | 是 | 密码（演示阶段明文） |

**成功响应**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "smoke-token-1-2026-07-09T00:00:00-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userRole": "admin",
    "username": "admin",
    "realName": "管理员",
    "phone": null,
    "email": null
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| token | string | 登录凭证，后续请求放 Header |
| userRole | string | 用户角色 `admin` / `viewer` |
| username | string | 用户名 |
| realName | string | 真实姓名（未填则与 username 一致） |
| phone | string \| null | 绑定手机号，未绑定为 `null` |
| email | string \| null | 绑定邮箱，未绑定为 `null` |

**错误码**：`1003` 参数校验失败、`2001` 账号或密码错误。

---

## 注册

`POST /api/auth/register`

请求体：

```json
{
  "account": "newuser",
  "password": "123456",
  "phone": "13823070420"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| account | string | 是 | 账号，3-64 位，全局唯一 |
| password | string | 是 | 密码，6-64 位 |
| phone | string | 否 | 手机号，填了可用于短信登录 |

**成功响应**：同登录接口，`data` 含 `token`，注册成功后直接进入系统。

**错误码**：`1003` 参数校验失败、`1201` 该账号已被注册。

---

## 发送短信验证码

`POST /api/auth/sms-code`

> 演示环境无真实短信网关：验证码会打印到后端控制台日志（`[sms-code] phone=... code=...`），生产环境应替换为 Redis + 真实短信网关并移除日志。

请求体：

```json
{
  "phone": "13823070420"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 手机号 |

**成功响应**：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "expiresIn": 300
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| expiresIn | int | 验证码有效期（秒），当前 300s |

---

## 手机验证码登录

`POST /api/auth/sms-login`

请求体：

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

**成功响应**：同账号密码登录，`data` 含 `token`。

**错误码**：`1003` 参数校验失败、`2001` 验证码错误或已过期 / 该手机号未注册。

> 演示环境策略：手机号已注册则直接登录；未注册则拒绝（避免自动批量注册）。

---

## 获取当前用户资料

`GET /api/auth/me`

| 项 | 值 |
|---|---|
| 鉴权 | 是，Header `Authorization: Bearer <token>` |
| 说明 | 解析 token 中的用户 ID，返回完整资料（手机/邮箱/角色等），用于设置页展示真实用户信息 |

**成功响应**：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "smoke-token-1-...",
    "userRole": "admin",
    "username": "admin",
    "realName": "管理员",
    "phone": null,
    "email": null
  }
}
```

**错误码**：`2001` 未登录或 token 失效 / 凭证格式错误。

---

## 前端存储与会话

登录 / 注册成功后前端会写入：

```js
localStorage.setItem('parrotAuthToken', token)
```

> `parrotAuthUser` 已不再使用，登录后通过 `GET /auth/me` 拉取完整资料。

前端设置页展示**真实**用户名、绑定手机号/邮箱与角色（未绑定显示「未绑定」），不再写死 mock 数据。

退出登录时前端清除 token、回到登录页。

所有业务 `request()` 请求会随 Header 自动携带：

```http
Authorization: Bearer <token>
```

`Authorization` 缺失或 token 过期时后端返回 HTTP `401`，业务码 `2001`。

---

## token 格式（演示阶段）

```
smoke-token-{userId}-{expiresAt}-{uuid}
```

例如 `smoke-token-1-2026-07-09T00:00:00-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`。`/auth/me` 通过拆分第 3 段解析用户 ID。后续接入 JWT 后此格式会替换，前端无需关心。
