package com.edy.firstai.config;

import com.edy.firstai.multimodel.ModelRouter;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

/**
 * 多模型装配 —— Java 架构师最该掌握的「Provider 可插拔」模式。
 *
 * <h2>为什么不能简单 builder.build(model)？</h2>
 * Spring AI 2.0 官方明确：直接 {@code ChatClient.create(chatModel)} /
 * {@code ChatClient.builder(chatModel)} 会绕过自动配置的
 * {@link ChatClientBuilderConfigurer}，从而丢失：
 * <ul>
 *   <li>Observability（Micrometer Observation）</li>
 *   <li>ChatClientBuilderCustomizer</li>
 *   <li>ToolCallingAdvisor 等默认装配</li>
 * </ul>
 * 正确做法见文档「ChatClients for Different Model Types」：
 * https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html#chatclients-for-different-model-types
 *
 * <h2>多模型 + enabled=false 的坑</h2>
 * 多模型并存时我们把 {@code spring.ai.chat.client.enabled=false} 关掉默认
 * {@link ChatClient.Builder} 自动装配，以避免 {@link ChatModel} 注入歧义。
 * 但 {@code enabled=false} 会把整个 {@code ChatClientAutoConfiguration} 都关掉，
 * 导致 {@link ChatClientBuilderConfigurer} 也没了。所以这里<b>自行定义</b>一个
 * {@link ChatClientBuilderConfigurer} bean，既保留可观测性，又不依赖自动装配。
 *
 * <h2>三种接入方式对照</h2>
 * <ol>
 *   <li>官方 Starter 自动配置：OpenAI / DeepSeek（本类注入具体类型）</li>
 *   <li>OpenAI 兼容协议：通义千问 DashScope Compatible Mode（本类手写 OpenAiChatModel）</li>
 *   <li>Spring AI Alibaba 等扩展 BOM：需要时再引入（本项目先不绑死）</li>
 * </ol>
 *
 * <h2>与 LangChain 的对应</h2>
 * LangChain 里 init_chat_model / 多 Runnable 路由 ≈ 这里的多个 ChatClient + ModelRouter。
 * https://python.langchain.com/docs/how_to/chat_models_universal_init/
 */
@Configuration
public class MultiModelConfig {

    /**
     * 多模型并存时 {@code spring.ai.chat.client.enabled=false} 会连
     * {@link ChatClientBuilderConfigurer} 一起关掉，导致启动失败。
     * 这里自行定义一个，等价于自动装配的空实现（无自定义 Customizer 时直接透传 builder），
     * 从而保留 Observation / ToolCallingAdvisor 等默认装配。
     */
    @Bean
    public ChatClientBuilderConfigurer chatClientBuilderConfigurer() {
        return new ChatClientBuilderConfigurer();
    }

    /**
     * 默认（Primary）ChatClient：对接 spring.ai.openai.*（本项目默认指向 LongCat）。
     * <p>
     * LongCat 完全兼容 OpenAI Chat Completions，因此复用 OpenAiChatModel 即可，
     * 只需改 base-url / api-key / model。这是 AI 应用架构里最常见的「协议兼容接入」模式。
     */
    @Bean
    @Primary
    public ChatClient openAiChatClient(
            OpenAiChatModel openAiChatModel,
            ChatClientBuilderConfigurer configurer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
            ObjectProvider<AdvisorObservationConvention> advisorObservationConvention) {
        return buildChatClient(
                openAiChatModel,
                configurer,
                observationRegistry,
                chatClientObservationConvention,
                advisorObservationConvention);
    }

    /**
     * DeepSeek：注入具体类型 {@link DeepSeekChatModel}，避免 ChatModel 接口歧义。
     * <p>
     * <b>阶段 0 可选 Provider</b>：DeepSeek 是否能成为 bean，取决于
     * {@code DeepSeekChatAutoConfiguration} 的 {@code @ConditionalOnProperty(spring.ai.model.chat=deepseek)}。
     * 这里用 {@link ObjectProvider}，model 不存在时返回 {@code null}（不创建 client），
     * 应用照常启动——「未配置的 Provider 不让启动失败」。
     */
    @Bean
    public ChatClient deepSeekChatClient(
            ObjectProvider<DeepSeekChatModel> deepSeekChatModel,
            ChatClientBuilderConfigurer configurer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
            ObjectProvider<AdvisorObservationConvention> advisorObservationConvention) {

        DeepSeekChatModel model = deepSeekChatModel.getIfAvailable();
        if (model == null) {
            return null; // DeepSeek 未配置/未激活：不创建该 client，路由器里也不会出现
        }
        return buildChatClient(
                model,
                configurer,
                observationRegistry,
                chatClientObservationConvention,
                advisorObservationConvention);
    }

    /**
     * 通义千问：通过 OpenAI Compatible Mode 创建独立 {@link OpenAiChatModel}。
     * <p>
     * 这是架构上极重要的能力：大量国产 / 开源推理网关都兼容 OpenAI 协议，
     * 你可以用同一套 ChatClient 代码切换 Provider，而不改业务层。
     * <p>
     * <b>阶段 0 可选 Provider</b>：由 {@code app.ai.qwen.enabled} 开关控制，默认 false。
     * 未显式开启时，Qwen 的 model / client bean 都不创建，应用启动只依赖 LongCat。
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.qwen.enabled", havingValue = "true")
    public ChatModel qwenChatModel(
            @Value("${app.ai.qwen.api-key}") String apiKey,
            @Value("${app.ai.qwen.base-url}") String baseUrl,
            @Value("${app.ai.qwen.model}") String model,
            @Value("${app.ai.qwen.temperature}") Double temperature) {

        // Spring AI 2.0：底层改用官方 openai-java SDK，旧 OpenAiApi 已移除。
        // 兼容端点通过 OpenAiChatOptions.baseUrl/apiKey 配置即可。
        // 文档：https://docs.spring.io/spring-ai/reference/2.0/api/chat/openai-chat.html
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }

    /**
     * 先创建longCat的baseModel,然后在buildLongCat的Client
     * **/
    @Bean
    public ChatModel longCatChatMode(){
        return OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
                .baseUrl("https://api.longcat.chat/openai")
                .apiKey("ak_2go1Wb4e66uV3cz7Ek7FS9qL4F58c")
                .model("LongCat-2.0")
                .temperature(0.1d)
                .build()
        ).build();
    }

    @Bean
    public ChatClient longCatChatClient(
            ChatModel longCatChatMode,
            ChatClientBuilderConfigurer configurer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
            ObjectProvider<AdvisorObservationConvention> advisorObservationConvention) {
        return buildChatClient(
                longCatChatMode,
                configurer,
                observationRegistry,
                chatClientObservationConvention,
                advisorObservationConvention);
    }

    @Bean
    public ChatClient qwenChatClient(
            @Qualifier("qwenChatModel") ObjectProvider<ChatModel> qwenChatModel,
            ChatClientBuilderConfigurer configurer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
            ObjectProvider<AdvisorObservationConvention> advisorObservationConvention) {

        // qwenChatModel bean 仅在 app.ai.qwen.enabled=true 时存在；未开启则返回 null
        ChatModel model = qwenChatModel.getIfAvailable();
        if (model == null) {
            return null;
        }
        return buildChatClient(
                model,
                configurer,
                observationRegistry,
                chatClientObservationConvention,
                advisorObservationConvention);
    }

    /**
     * 路由器：只把「实际存在」的 client 注册进去。
     * <p>
     * 用 {@link ObjectProvider} + {@code @Qualifier} 接收 DeepSeek / Qwen —— 它们可能为 null
     * （未配置 / 未开启），{@link ObjectProvider#getIfAvailable()} 此时返回 null。
     * 注意必须按 bean 名限定：若只写 {@code ObjectProvider<ChatClient>}，Spring 会取
     * {@code @Primary} 的 openAiChatClient，而非我们要的 deepSeek/qwen client。
     * {@link Map#of} 不允许 null value，所以这里用 {@link HashMap} 手工 put 非空项。
     */
    @Bean
    public ModelRouter modelRouter(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("deepSeekChatClient") ObjectProvider<ChatClient> deepSeekChatClient,
            @Qualifier("qwenChatClient") ObjectProvider<ChatClient> qwenChatClient) {
        Map<String, ChatClient> routes = new HashMap<>();
        routes.put("longcat", openAiChatClient);
        routes.put("openai", openAiChatClient); // 别名，兼容旧调用
        deepSeekChatClient.ifAvailable(c -> routes.put("deepseek", c));
        qwenChatClient.ifAvailable(c -> routes.put("qwen", c));
        return new ModelRouter(routes);
    }

    /**
     * 官方推荐的「保留可观测性」构建方式。
     * <p>
     * 阶段 0 明确禁止 Tool Calling，因此使用 4 参数 {@link ChatClient#builder} 重载
     * （ObservationRegistry + 两个 ObservationConvention），<b>不</b>传入第 5 参数
     * {@code ToolCallingAdvisor.Builder}。
     * <p>
     * 三对照：
     * <ul>
     *   <li>1 参数 builder：连 Observation 都丢掉 —— 不可取</li>
     *   <li>4 参数 builder：保留 Observation，无 Tool Calling —— 阶段 0 正确选择</li>
     *   <li>5 参数 builder：额外装配 ToolCallingAdvisor —— 阶段 0 禁止</li>
     * </ul>
     */
    private ChatClient buildChatClient(
            ChatModel chatModel,
            ChatClientBuilderConfigurer configurer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
            ObjectProvider<AdvisorObservationConvention> advisorObservationConvention) {

        ChatClient.Builder builder = ChatClient.builder(
                chatModel,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                chatClientObservationConvention.getIfUnique(),
                advisorObservationConvention.getIfUnique());

        return configurer.configure(builder).build();
    }
}
