package com.edy.firstai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结构化输出（Structured Output）：把 LLM 自由文本约束成 Java 对象。
 *
 * <h2>原理（两层）</h2>
 * <ol>
 *   <li><b>Prompt 约束</b>：BeanOutputConverter 根据 Java 类型生成 JSON Schema，
 *       把格式说明追加进 prompt，再反序列化。</li>
 *   <li><b>Provider 原生结构化输出</b>（更可靠）：把 schema 传给模型 API
 *       （如 OpenAI response_format.json_schema），减少「说了要 JSON 却返回 Markdown」。</li>
 * </ol>
 *
 * Spring AI 文档：
 * https://docs.spring.io/spring-ai/reference/2.0/api/structured-output-converter.html
 * ChatClient.entity()：
 * https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html#returning-an-entity
 *
 * LangChain 对照：with_structured_output / PydanticOutputParser
 * https://python.langchain.com/docs/how_to/structured_output/
 *
 * 相关能力演进：
 * OpenAI Structured Outputs（2024）— 模型侧 JSON Schema 约束，显著降低解析失败率。
 */
@RestController
@RequestMapping("/api/ai")
public class StructuredOutputController {

    private final ChatClient chatClient;

    public StructuredOutputController(@Qualifier("longCatChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 目标类型用 record：不可变 + 清晰 schema，非常适合作为 LLM 输出契约（DTO）。
     */
    public record Person(String name, int age, String email) {
    }

    /**
     * 基础用法：.entity(Person.class)
     * <p>
     * 内部 ≈ BeanOutputConverter：生成 schema → 注入格式指令 → JSON → Person。
     */
    @GetMapping("/structured")
    public Person structured(@RequestParam(defaultValue = "虚构一位资深Java架构师的个人信息") String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(Person.class);
    }

    /**
     * 生产推荐：Provider 原生结构化输出 + schema 校验重试。
     * <ul>
     *   <li>{@code useProviderStructuredOutput()}：走模型 API 的 schema 约束</li>
     *   <li>{@code validateSchema()}：校验失败则带错误信息重试（StructuredOutputValidationAdvisor）</li>
     * </ul>
     * 注意：validateSchema 与 streaming 不兼容（官方说明）。
     */
    @GetMapping("/structured-strict")
    public Person structuredStrict(
            @RequestParam(defaultValue = "虚构一位资深Java架构师的个人信息") String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(Person.class, spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema());
    }
}
