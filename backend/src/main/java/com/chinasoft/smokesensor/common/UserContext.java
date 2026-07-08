package com.chinasoft.smokesensor.common;

/**
 * 当前登录用户上下文。
 *
 * <p>由 {@link com.chinasoft.smokesensor.config.AuthInterceptor} 在每个请求开始时
 * 从 token 解析出用户 ID 并写入；请求结束后清理，避免线程复用导致用户串号。
 *
 * <p>业务层通过 {@link #getCurrentUserId()} 或 {@link #requireUserId()} 取当前用户 ID，
 * 不再依赖写死的默认用户。
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 返回当前登录用户 ID；未登录或 token 无效时返回 null。
     */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 返回当前登录用户 ID；未登录时直接抛 401。
     *
     * <p>用于业务层中“必须登录才能操作”的接口，避免每个调用点重复判空。
     */
    public static Long requireUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 请求结束时清理，防止 ThreadLocal 在线程池中残留。
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
