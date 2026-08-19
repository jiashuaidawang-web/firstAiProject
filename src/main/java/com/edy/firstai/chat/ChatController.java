package com.edy.firstai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ChatClient 同步对话示例。
 *
 * <h2>原理：一次 Chat Completion-完成 请求在做什么？</h2>
 * <ol>
 *   <li>组装 Prompt = SystemMessage? + UserMessage(+ History)</li>
 *   <li>经 Advisor-顾问 链（Spring AI 2.0 把 tool-loop / structured-output-retry 都提升为可组合 Advisor）</li>
 *   <li>ChatModel 调用 Provider HTTP API（/v1/chat/completions）</li>
 *   <li>返回 assistant content（以及 usage / finishReason 等元数据）</li>
 * </ol>
 *
 * 官方 ChatClient 文档：
 * https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html
 *
 * 对照论文 / 概念：
 * <ul>
 *   <li>System / User / Assistant-辅助 角色来自 InstructGPT-指示 / ChatML 实践：
 *       Ouyang et al., "Training language models to follow instructions with human feedback" (InstructGPT), NeurIPS 2022</li>
 *   <li>temperature 控制采样随机性：Holtzman et al., "The Curious Case of Neural Text Degeneration", ICLR 2020</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient chatClient;

    /**
     * 注入名为 openAiChatClient 的 Bean（见 {@link com.edy.firstai.config.MultiModelConfig}）。
     * 多模型场景下不要依赖自动配置的单例 Builder，以免 ChatModel 歧义。
     */
    public ChatController(@Qualifier("openAiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 最简同步对话：user → call → content。
     *
     * <pre>
     * GET /api/ai/chat?prompt=用一句话解释什么是ChatClient
     * </pre>
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String prompt) {
        // prompt()：开启 fluent API
        // user(...)：写入 UserMessage
        // call()：阻塞调用（适合短请求 / 服务端编排）
        // content()：只要文本；若要 token 用量用 chatResponse()
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 带 System Prompt 的对话。
     * <p>
     * System 消息用于设定「角色 / 约束 / 输出风格」，通常比塞进 user 里更稳定。
     * 这对应 Agent 设计里的 Policy / Persona 层。
     */
    @GetMapping("/chat-with-system")
    public String chatWithSystem(@RequestParam String prompt) {
        return chatClient.prompt()
                .system("""
                        你是一名资深 Java 技术顾问，擅长 Spring Framework、分布式架构与 AI Agent 应用落地。
                        回答要简洁、可落地，必要时给出接口设计或伪代码。
                        """)
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 返回完整 {@link ChatResponse}：包含 generations、token usage、finish reason。
     * 生产环境计费、限流、质量评估都会用到这些元数据。
     */
    @GetMapping("/chat-response")
    public Map<String, Object> chatResponse(@RequestParam String prompt) {
        ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        return Map.of(
                "content", response.getResult().getOutput().getText(),
                "metadata", response.getMetadata()
        );
    }
}
