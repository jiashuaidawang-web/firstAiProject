package com.edy.firstai.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * SSE 流式对话。
 *
 * <h2>为什么 Agent / 对话产品几乎都要流式？</h2>
 * LLM 生成是自回归（autoregressive）的：逐 token 预测下一个 token。
 * 流式把「已生成部分」尽早推给前端，降低首字延迟（TTFT），改善体验。
 *
 * <h2>Spring AI 流式 API</h2>
 * <pre>
 *   chatClient.prompt().user(...).stream().content()     → Flux&lt;String&gt;  文本增量
 *   chatClient.prompt().user(...).stream().chatResponse() → Flux&lt;ChatResponse&gt; 带元数据
 * </pre>
 * 文档：https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html#streaming-responses
 *
 * <h2>与 Web 的对接</h2>
 * {@code produces = TEXT_EVENT_STREAM} 时，Spring MVC 把 Flux 写成 SSE。
 * 前端可用 EventSource / fetch ReadableStream 消费。
 *
 * 注意：结构化输出 {@code entity()} 不支持边流边解析完整对象；
 * 需先 {@code collectList().join()} 再交给 BeanOutputConverter（官方文档同页说明）。
 */
@RestController
@RequestMapping("/api/stream")
public class StreamChatController {

    private static final Logger log = LoggerFactory.getLogger(StreamChatController.class);

    private final ChatClient chatClient;

    public StreamChatController(@Qualifier("openAiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 文本增量 SSE。
     * <p>
     * 纠正常见误区：{@code .stream().content()} 返回的是 {@link Flux}&lt;{@link String}&gt;，
     * 不是 {@code Flux&lt;ChatResponse&gt;}。要 ChatResponse 请用 {@code .stream().chatResponse()}。
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(chunk -> log.debug("SSE chunk: {}", chunk))
                .doOnError(e -> log.error("AI streaming failed", e))
                .onErrorResume(e -> Flux.just("[ERROR] AI 服务异常: " + e.getMessage()));
    }

    /**
     * 带元数据的流式输出（适合需要看 finishReason / 中间状态的场景）。
     */
    @GetMapping(value = "/chat-response", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChatResponse(@RequestParam String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .chatResponse();
    }

    /**
     * 演示 Reactor 背压与限速。
     * <p>
     * LLM Provider 侧通常按 token 推送，客户端消费过慢时，Reactor 缓冲可防止下游被冲垮。
     * 真实生产更常见的是：前端渲染节流 + 服务端不主动 delay（避免人为拉高延迟）。
     * <p>
     * 背压概念：Reactive Streams Spec — https://www.reactive-streams.org/
     */
    @GetMapping(value = "/chat-controlled", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatControlled(@RequestParam String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .delayElements(Duration.ofMillis(30))
                .onBackpressureBuffer(256);
    }
}
