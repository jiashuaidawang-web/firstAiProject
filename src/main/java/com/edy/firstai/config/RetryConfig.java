package com.edy.firstai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.time.Duration;

/**
 * 业务层重试 —— 使用 Spring Framework 7 内置 {@link RetryTemplate}。
 *
 * <h2>Boot 4 重要变化（架构师必读）</h2>
 * 独立项目 {@code spring-retry} 已归档，能力并入 {@code org.springframework.core.retry}。
 * 迁移指南：https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
 * 博文：https://spring.io/blog/2025/09/09/core-spring-resilience-features/
 *
 * <h2>两层重试不要混为一谈</h2>
 * <ol>
 *   <li>{@code spring.ai.retry.*}：ChatModel HTTP 客户端层（Spring AI 内置）</li>
 *   <li>本类 RetryTemplate：业务编排层（可按异常类型、可加补偿/降级）</li>
 * </ol>
 *
 * 经验法则：指数退避 + jitter，避免惊群；只对可恢复错误重试。
 * AWS：https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate aiRetryTemplate() {
        RetryPolicy policy = RetryPolicy.builder()
                .maxRetries(2) // 额外重试次数（不含首次）；共最多 3 次尝试
                .delay(Duration.ofSeconds(1))
                .multiplier(2.0)
                .maxDelay(Duration.ofSeconds(10))
                .jitter(Duration.ofMillis(200))
                .build();
        return new RetryTemplate(policy);
    }
}
