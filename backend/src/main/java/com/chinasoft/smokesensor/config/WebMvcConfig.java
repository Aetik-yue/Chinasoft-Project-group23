package com.chinasoft.smokesensor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册全局登录拦截器。
 *
 * <p>拦截所有 /api/** 请求；登录、短信验证码、设备数据上报等无用户态的接口放行。
 * WebSocket 端点 /ws/alarm 不在 /api/** 下，不受影响。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/sms-code",
                        "/api/auth/sms-login",
                        "/api/auth/register",
                        "/api/sensor/upload"
                );
    }
}
