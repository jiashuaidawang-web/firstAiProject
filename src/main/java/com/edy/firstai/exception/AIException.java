package com.edy.firstai.exception;

/**
 * AI 下游失败统一业务异常。
 * <p>
 * 建议在架构上区分：
 * <ul>
 *   <li>Transient（可重试）：429 / 5xx / 超时</li>
 *   <li>Non-transient（不可重试）：401 / 内容违规 / 参数错误</li>
 * </ul>
 * Spring AI 自身也有 TransientAiException / NonTransientAiException 概念（见各 Provider retry 文档）。
 */
public class AIException extends RuntimeException {

    public AIException(String message) {
        super(message);
    }

    public AIException(String message, Throwable cause) {
        super(message, cause);
    }
}
