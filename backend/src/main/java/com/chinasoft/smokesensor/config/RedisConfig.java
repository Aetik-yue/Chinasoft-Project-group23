package com.chinasoft.smokesensor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置类。
 *
 * <p>作用：配置 RedisTemplate，让存入 Redis 的 Java 对象以 JSON 格式存储（而非默认的 JdkSerialization），
 * 便于调试查看缓存内容，同时支持 LocalDateTime 等常用 Java 时间类型的序列化/反序列化。
 *
 * <p>使用方式：在其他 Service 中直接注入 {@code RedisTemplate<String, Object>} 即可。
 */
@Configuration
public class RedisConfig {

    /**
     * 统一 ObjectMapper：注册 JavaTimeModule 以支持 LocalDateTime 序列化。
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 8 时间模块，使 LocalDateTime 等类型能被 Jackson 正确序列/反序列化
        mapper.registerModule(new JavaTimeModule());
        // 禁止将日期序列化为时间戳，始终输出为 "2026-07-06T14:02:11" 格式的字符串
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * RedisTemplate Bean。
     *
     * <p>Key 使用 String 序列化（人眼可读），Value 使用 GenericJackson2JsonRedisSerializer（JSON 格式）。
     * 序列化后的 JSON 中会包含 "@class" 字段，用于反序列化时还原对象类型。
     *
     * @param factory  Redis 连接工厂（由 Spring Boot 自动配置提供）
     * @param objectMapper 上面定义的 ObjectMapper
     * @return 配置好的 RedisTemplate，可直接注入使用
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper redisObjectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 使用 GenericJackson2JsonRedisSerializer：将对象序列化为 JSON，保留类型信息
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // 设置 Key 序列化器：String（如 "smoke:latest:SMK-001"）
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        // 设置 Value 序列化器：JSON（如 {"deviceId":"SMK-001","smokeValue":86,...}）
        template.setValueSerializer(jsonSerializer);
        // 设置 Hash 结构 Key/Value 序列化器（预留，后续如需 HASH 结构可直接用）
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashValueSerializer(jsonSerializer);

        // 初始化所有已配置的序列化器
        template.afterPropertiesSet();
        return template;
    }
}