package com.edy.firstai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第一个 Spring AI 2.0 项目入口。
 *
 * <h2>你要从「Java 架构」迁移到「AI Agent 应用架构」时，先记住三层</h2>
 * <pre>
 *   Controllers / Services     —— 业务编排（和你写的微服务一样）
 *        ↓
 *   ChatClient + Advisors      —— Agent 能力面：Prompt、Tool、Memory、RAG、结构化输出
 *        ↓
 *   ChatModel (Provider SPI)   —— 对接 OpenAI / DeepSeek / 通义千问等具体模型
 * </pre>
 *
 * Spring AI 2.0 明确把 {@code ChatClient} 作为用户侧主 API，{@code ChatModel} 更偏底层构建块。
 * 发布说明：https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now
 *
 * 对照 LangChain：
 * <ul>
 *   <li>ChatModel ≈ LangChain ChatModel / BaseChatModel</li>
 *   <li>ChatClient ≈ LangChain LCEL Runnable 组合（更 Spring 化的 fluent API）</li>
 *   <li>Advisor ≈ LangChain Middleware / Callbacks + 可循环的拦截链</li>
 * </ul>
 * LangChain Concepts：https://python.langchain.com/docs/concepts/
 */
@SpringBootApplication
public class FirstAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FirstAiApplication.class, args);
    }
}
