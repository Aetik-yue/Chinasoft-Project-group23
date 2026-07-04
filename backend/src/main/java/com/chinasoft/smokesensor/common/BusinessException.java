package com.chinasoft.smokesensor.common;

import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 *
 * Service 层遇到业务错误时抛出该异常，
 * 再由 {@link GlobalExceptionHandler} 统一转换为 ApiResult 响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /**
     * 资源不存在：HTTP 404 / 业务 code 4004。
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(4004, message, HttpStatus.NOT_FOUND);
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
