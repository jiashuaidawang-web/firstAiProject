package com.edy.firstai.exception;

/**
 * 限流异常：对应 HTTP 429 / Provider rate limit。
 * <p>
 * 生产实践：结合 Retry-After、令牌桶、队列削峰；对用户返回可重试提示。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
