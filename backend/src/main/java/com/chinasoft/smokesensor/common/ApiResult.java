package com.chinasoft.smokesensor.common;

/**
 * 统一接口响应包装。
 *
 * 前端所有接口都按 { code, message, data } 结构读取：
 * code=0 表示成功，非 0 表示失败；失败时 data 通常为 null。
 */
public record ApiResult(int code, String message, Object data) {

    /**
     * 成功响应。
     */
    public static ApiResult ok(Object data) {
        return new ApiResult(0, "ok", data);
    }

    /**
     * 失败响应。
     */
    public static ApiResult error(int code, String message) {
        return new ApiResult(code, message, null);
    }
}
