package com.chinasoft.smokesensor.common;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * 负责把 Controller 和 Service 抛出的异常统一转换成 ApiResult，
 * 保证前端拿到稳定的 { code, message, data } 响应结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：按异常自带的业务 code 和 HTTP 状态返回。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult> handleBusiness(BusinessException ex) {
        // 业务异常由用户操作触发（密码错误、用户不存在等），warn 级即可，不打堆栈。
        log.warn("业务异常 code={} message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResult.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * @Valid 请求体校验失败：HTTP 400 / 业务 code 1003。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求参数校验失败: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(1003, message));
    }

    /**
     * @Validated 路径参数或查询参数校验失败：HTTP 400 / 业务 code 1003。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(1003, ex.getMessage()));
    }

    /**
     * 非法参数异常：HTTP 400 / 业务 code 1003。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(1003, ex.getMessage()));
    }

    /**
     * 数据完整性冲突，例如唯一键重复：HTTP 500 / 业务 code 5000。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult> handleDataIntegrity(DataIntegrityViolationException ex) {
        // 数据唯一键冲突属于数据层异常，打 error 级；前端只收固定提示，细节留日志。
        log.error("数据完整性冲突", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(5000, "数据冲突：存在重复或关联数据"));
    }

    /**
     * 兜底异常处理：HTTP 500 / 业务 code 5000。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult> handleOther(Exception ex) {
        // 兜底异常 = 意料之外，必须打 error 级并保留完整堆栈；前端只收固定提示，不泄露内部细节。
        log.error("未捕获异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(5000, "服务器内部错误，请稍后重试"));
    }
}
