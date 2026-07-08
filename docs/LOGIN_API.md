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

## 发送短信验证码

`POST /api/auth/sms-code`

请求体：

```json
{
  "phone": "13823070420"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "验证码已发送",
  "data": {
    "expiresIn": 60
  }
}
```

## 手机验证码登录

`POST /api/auth/sms-login`

请求体：

```json
{
  "phone": "13823070420",
  "code": "123456"
}
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "JWT_OR_SESSION_TOKEN",
    "user": {
      "userId": "U-230701-042",
      "username": "Wenderella",
      "phone": "13823070420"
    }
  }
}
```

## 账号密码登录

`POST /api/auth/login`

请求体：

```json
{
  "account": "wenderella",
  "password": "123456"
}
```

成功响应同验证码登录。

## 前端存储

登录成功后前端会写入：

```js
localStorage.setItem('parrotAuthToken', token)
localStorage.setItem('parrotAuthUser', JSON.stringify(user))
```

退出登录时前端会清除以上两个字段并回到登录页。

登录成功后的所有 `request()` 请求会自动携带：

```http
Authorization: Bearer JWT_OR_SESSION_TOKEN
```
