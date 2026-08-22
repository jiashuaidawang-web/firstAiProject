package com.edy.firstai.quant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 量化研究简报 —— 阶段 0 验收接口（不接行情 API、禁 Tool Calling、禁 RAG）。
 *
 * <p>注入 {@code openAiChatClient}（{@code @Primary}，见 MultiModelConfig），
 * 底层走 {@code spring.ai.openai.*} → LongCat。
 */
@RestController
@RequestMapping("/api/quant")
public class QuantController {

    private final ChatClient openAiChatClient;

    public QuantController(@Qualifier("openAiChatClient") ChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
    }

    /**
     * 量化简报输出契约（DTO）。
     * <p>用 record：不可变 + 清晰 schema，天然适合做 LLM 结构化输出的反序列化目标。
     */
    public record QuantBrief(
            List<String> symbols,
            String logic,
            List<String> uncertainties,
            List<String> nextActions) {
    }

    /**
     * 同步版：{@code .entity()} 结构化输出。
     * <pre>
     * GET/POST /api/quant/brief?goal=我想做沪深300的动量策略
     * </pre>
     */
    @GetMapping("/brief")
    public QuantBrief brief(@RequestParam String goal) {
        // ===================== ChatClient 调用链（讲解锚点）=====================
        return openAiChatClient
                .prompt()                                            // ① 开启 fluent Builder（Prompt 容器）
                .system("""
                        你是投研助理。不得编造具体成交价或未提供的数据；
                        只给研究框架（标的池、逻辑、不确定性、下一步），不给出投资建议。
                        """)                                             // ② SystemMessage：角色 + 硬约束（Policy 层）
                .user(goal)                                          // ③ UserMessage：本轮用户输入（goal）
                .call()                                              // ④ 阻塞调用 ChatModel → Provider HTTP /v1/chat/completions
                .entity(QuantBrief.class);                           // ⑤ BeanOutputConverter：完整响应文本 → JSON → Java record
        // ======================================================================
        // ⑤ 内部：根据 QuantBrief 类型生成 JSON Schema → 注入格式指令 → 等完整响应 → 反序列化
        // 因此 entity() 必须拿到【完整】响应，不能边流边解析（见 briefStream 说明）。
    }

    /**
     * SSE 版：只流式输出 {@code logic} 文本。
     *
     * <h2>为什么不是 {@code .stream().entity(...)}？</h2>
     * {@link ChatClient.ChatClientRequestSpec#entity(Class)} 是同步、终结式操作。
     * 它依赖 BeanOutputConverter：先把 LLM 返回的完整文本当 JSON 反序列化成 QuantBrief。
     * 流式（{@code .stream()}）吐的是增量 chunk，永远拼不成完整 JSON，
     * 所以「边流边 entity」在语义上就不成立——{@code .stream().entity()} 不存在。
     *
     * <p>正确做法：先同步 {@code .entity()} 拿到完整 QuantBrief，再把 {@code logic} 字段拆成 Flux 流式推送。
     * 这里按中文句末标点切分，让前端看到「逐句出现」的流式效果；
     * 本质是「先结构化、再流式推送字段」，流的是【已完整解析后的文本】，不是 LLM 原始增量。
     *
     * <pre>
     * GET /api/quant/brief-stream?goal=我想做沪深300的动量策略
     * </pre>
     */
    @GetMapping(value = "/brief-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> briefStream(@RequestParam String goal) {
        // 必须先拿到完整结构化对象——entity() 不能边流边解析。
        QuantBrief brief = openAiChatClient
                .prompt()
                .system("""
                        你是投研助理。不得编造具体成交价或未提供的数据；
                        只给研究框架（标的池、逻辑、不确定性、下一步），不给出投资建议。
                        """)
                .user(goal)
                .call()
                .entity(QuantBrief.class);

        // 把已解析的 logic 字段，按句末标点切成 chunk 作为 SSE 推出。
        return Flux.fromArray(brief.logic().split("(?<=[。；!?\n])"))
                .filter(chunk -> !chunk.isBlank())
                .concatWith(Flux.just("[DONE]"));
    }
}
