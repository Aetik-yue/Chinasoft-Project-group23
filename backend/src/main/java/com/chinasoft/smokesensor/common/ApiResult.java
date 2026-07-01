package com.chinasoft.smokesensor.common;

/**
 * 统一响应包装，对应 API 文档 2.1：{ code, message, data }。
 * code=0 表示成功，非 0 表示失败；失败时 data 为 null。
 */
public record ApiResult(int code, String message, Object data) {

    public static ApiResult ok(Object data) {
        return new ApiResult(0, "ok", data);
    }

    public static ApiResult error(int code, String message) {
        return new ApiResult(code, message, null);
    }
}
