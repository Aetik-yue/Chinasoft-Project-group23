package com.chinasoft.smokesensor.common;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。把异常统一转成 API 文档约定的 { code, message, data } 结构。
 * 错误码对应 API 文档 2.3：1003 参数非法、4004 资源不存在、5000 服务器内部错误。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常自带的 code/HTTP 状态返回。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResult.error(ex.getCode(), ex.getMessage()));
    }

    /** @Valid 请求体校验失败：400 / 1003。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(1003, message));
    }

    /** @Validated 路径/Query 校验失败：400 / 1003。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(1003, ex.getMessage()));
    }

    /** 非法参数（兜底）：400 / 1003。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(1003, ex.getMessage()));
    }

    /** 数据完整性冲突（如唯一键重复）：500 / 5000。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(5000, "数据冲突: " + ex.getMostSpecificCause().getMessage()));
    }

    /** 兜底：500 / 5000。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(5000, "服务器内部错误: " + ex.getMessage()));
    }
}
