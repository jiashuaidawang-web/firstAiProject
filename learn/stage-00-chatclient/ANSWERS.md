# 阶段 0 · 标准答案（写完 QUESTIONS 再看）

## A

1. `ChatModel` ≈ 低层 Provider 客户端（RestTemplate / WebClient 那种），负责把 Prompt 打到具体 HTTP API。`ChatClient` ≈ 高层 fluent API（RestClient），负责拼 system/user、Advisor、结构化输出、以后的 tools。Spring AI 2.0 把日常开发面收口到 Client，Model 当 SPI。只抱 Model 写，等于绕过拦截器和观测。

2. Controller 取字符串 → `chatClient.prompt().system?(...).user(goal)` 建成 Prompt（若干 Message）→ Advisor 链（阶段 0 几乎透传）→ ChatModel 序列化成 OpenAI 风格 `messages` + `model` + `temperature` → POST `{base-url}/v1/chat/completions` → 响应 choices[0].message.content 和 usage → Client 解成 String 或 ChatResponse。

3. Spring AI OpenAI starter 会把 completions path（默认 `/v1/chat/completions`）拼到 base-url 后面。LongCat 文档的接入端点是 `https://api.longcat.chat/openai`，因此 base-url 停在 `/openai`。若你再手写 `/v1/...`，容易变成错误路径 404。这是「兼容网关 + 客户端约定」问题，不是模型魔法。

## B

4. `Flux<String>`：每个元素是文本增量。`Flux<ChatResponse>` 要用 `.stream().chatResponse()`。类型写错要么编不过，要么你以为有 metadata 其实只有碎片字符串。

5. 流式到达的是 token 碎片，完整 JSON 在结束前不合法，BeanOutputConverter 无法稳定反序列化。做法：同步路径 `.entity()`；或把 stream 先 `collect` 成完整字符串再 convert（官方文档同一节）。UX 上可以 SSE 推「思考过程」，契约字段等结束后再给一条完整 JSON 事件。

6. Prompt 里求 JSON = 软约束，模型仍可能输出 markdown 代码块。Provider 原生结构化 = 把 schema 交给 API（如 json_schema），失败率低得多。`validateSchema()` 再在本地校验失败则重试。原生支持因模型而异，所以默认不一定打开。

## C

7. 丢掉自动配置的 Observation、`ChatClientBuilderCustomizer`、默认 ToolCallingAdvisor 装配。`MultiModelConfig` 用 `ChatClient.builder(model, observation..., toolAdvisorBuilder)` + `ChatClientBuilderConfigurer.configure(builder)`，与官方「ChatClients for Different Model Types」一致。LongCat 仍用 `OpenAiChatModel` 是因为协议兼容。

8. 只要展示文本：`content()`。要计费、finishReason、多 generation：`chatResponse()` 读 metadata。流式 TTFT 要从第一个 chunk 的时间戳自己打点。

9. Boot 4 / Framework 7 把 retry 收进 `org.springframework.core.retry`；BOM 不再管 `spring-retry`。本仓库 `RetryConfig` 使用 `RetryPolicy.builder()` + `RetryTemplate`。`maxRetries` 指额外重试次数，不含首次。

## D

10. 阶段 0 要先把「契约 + 流式 + 网关」学纯。没有 Tool 时模型给出的价格是幻觉，不能当数据。产品处理：System 明确禁止编造数字；JSON 里若出现具体行情字段，校验丢弃或标 `unverified`；真正价格留给阶段 1 的 MarketData 工具。

## E

11. 自检要点（口试过关标准）：提到自回归、首 token、SSE `data:` 行、连接断开后服务端应停止向已取消的订阅推（理想情况）。没提到「模型不会一次性想好整段再发」的，回去看 StreamChatController 注释。
