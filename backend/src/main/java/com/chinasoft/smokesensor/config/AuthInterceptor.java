package com.chinasoft.smokesensor.config;

import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 全局登录拦截器：从 Authorization 请求头解析 token，取出当前用户 ID 存入 {@link UserContext}。
 *
 * <p>当前为“宽松”模式：有 token 且有效则写入上下文，没有或无效则留空（不拦截请求）。
 * 这样既能让已登录请求拿到用户 ID，又不会在改造期间打断未登录的现有接口。
 * 后续业务层接入用户 ID 后，可在此处切换为“强制鉴权”（无效则返回 401）。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            Long userId = authService.resolveUserIdFromToken(token);
            if (userId != null) {
                UserContext.setCurrentUserId(userId);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        // 无论请求是否异常，都要清理 ThreadLocal，防止线程复用导致用户串号。
        UserContext.clear();
    }
}
