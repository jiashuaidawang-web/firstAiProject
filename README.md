

**我建议你的实际使用方式是：每天只打开当天的 Day-XX 文件，
严格执行“理解 → AI 开发 → Review → 验收 → Git Commit”这个闭环。
**这样你不是被 AI 带着复制代码，而是在真实项目中逐步建立 Agent 架构能力



# First AI Project · Spring AI 2.0 ChatClient

面向 **10 年 Java 架构师** 的 AI Agent 应用开发入门工程：用你熟悉的 Spring Boot 分层，把 LLM 接到生产级可观测、可路由、可重试的服务骨架上。

## 你要建立的心智模型

```
业务层 (Controller / Service)
        ↓
ChatClient + Advisors     ← 你日常该用的 API（类比 RestClient）
        ↓
ChatModel (Provider SPI)  ← OpenAI / DeepSeek / 通义千问…（类比 RestTemplate 适配器）
```

Spring AI 2.0 把 **ChatClient** 作为用户侧主 API，**ChatModel** 更偏底层构建块。  
发布说明：[Spring AI 2.0.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)

| Spring AI | 你熟悉的 Spring | LangChain |
|-----------|-----------------|-----------|
| `ChatModel` | `RestTemplate` / 低层客户端 | `BaseChatModel` |
| `ChatClient` | `RestClient` fluent API | LCEL Runnable 组合 |
| `Advisor` | Filter / Interceptor（可循环） | Middleware / Callbacks |
| `BeanOutputConverter` / `.entity()` | HttpMessageConverter | `with_structured_output` |

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | **21** | Spring Boot 4 / Spring AI 2.0 基线 |
| Spring Boot | 4.1.0 | 与 Spring AI 2.0 深度集成 |
| Spring AI | 2.0.0 GA | 2026-06-12 发布 |
| Maven | 3.9+ | |

官方入门：[Getting Started](https://docs.spring.io/spring-ai/reference/2.0/getting-started.html)

## 快速启动

```bash
# 1) LongCat（OpenAI 兼容）—— 主模型
export LONGCAT_API_KEY=ak-xxx
# 可选覆盖：
# export LONGCAT_BASE_URL=https://api.longcat.chat/openai
# export LONGCAT_CHAT_MODEL=LongCat-2.0

# 其他模型（可选）
export DEEPSEEK_API_KEY=sk-xxx
export DASHSCOPE_API_KEY=sk-xxx

# 2) 使用 JDK 21
export JAVA_HOME=/Library/Myself/Env/JDK/jdk21/Contents/Home   # 按本机路径调整

# 3) 启动
mvn spring-boot:run
```

浏览器打开：http://localhost:8080/ （SSE 演示页）

## API 一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/chat?prompt=` | 同步对话 |
| GET | `/api/ai/chat-with-system?prompt=` | 带 System Prompt |
| GET | `/api/ai/chat-response?prompt=` | 含 usage 元数据 |
| GET | `/api/ai/structured?prompt=` | 结构化输出 → Java record |
| GET | `/api/ai/structured-strict?prompt=` | 原生 schema + 校验重试 |
| GET | `/api/stream/chat?prompt=` | SSE 文本流 |
| GET | `/api/multi/models` | 可用模型 |
| GET | `/api/multi/chat?model=longcat&prompt=` | 指定模型（longcat / deepseek / qwen） |
| GET | `/api/multi/smart?prompt=` | 启发式智能路由 |
| GET | `/api/multi/compare?prompt=` | 多模型对比 |

## 大纲对应关系 & 对你原稿的修正

你的学习大纲已落到本仓库代码中，并按 **官方 API** 修正了几处常见坑：

1. **流式返回类型**：`.stream().content()` → `Flux<String>`，不是 `Flux<ChatResponse>`（要用 `.stream().chatResponse()`）。  
   文档：[Streaming Responses](https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html#streaming-responses)

2. **多模型不要** `builder.build(model)`：会丢掉 Observation / Customizer / ToolCallingAdvisor。  
   正确做法：`ChatClientBuilderConfigurer` + 具体 `*ChatModel` 类型注入。  
   文档：[Multiple Chat Models](https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html#working-with-multiple-chat-models)

3. **通义千问**：本项目用 DashScope **OpenAI Compatible Mode**（同一套 `OpenAiChatModel`），便于先掌握「协议兼容」这一架构能力；需要 Alibaba 原生特性时再引入 Spring AI Alibaba。

4. **两层重试**：`spring.ai.retry.*`（HTTP 层）+ Spring Framework 7 内置 `org.springframework.core.retry.RetryTemplate`（业务层）。Boot 4 已移除对独立 `spring-retry` 的 BOM 管理。

## 推荐阅读（论文 / 文档）

### Spring AI
- [ChatClient API](https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html)
- [Structured Output Converter](https://docs.spring.io/spring-ai/reference/2.0/api/structured-output-converter.html)
- [OpenAI Chat](https://docs.spring.io/spring-ai/reference/2.0/api/chat/openai-chat.html)
- [DeepSeek Chat](https://docs.spring.io/spring-ai/reference/2.0/api/chat/deepseek-chat.html)

### LangChain（对照概念，不必换栈）
- [Concepts](https://python.langchain.com/docs/concepts/)
- [Structured output](https://python.langchain.com/docs/how_to/structured_output/)
- [Routing](https://python.langchain.com/docs/how_to/routing/)

### 论文 / 规范（帮你把「调参」变成「有理论支撑的设计」）
- Ouyang et al., *InstructGPT*, NeurIPS 2022 — System/User 指令跟随
- Holtzman et al., *Neural Text Degeneration*, ICLR 2020 — temperature / nucleus sampling
- Yao et al., *ReAct*, ICLR 2023 — 推理 + 工具调用（下一阶段 Tool/Agent）
- Zheng et al., *Judging LLM-as-a-Judge*, NeurIPS 2023 — 多模型评测
- OpenTelemetry [GenAI Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/gen-ai/)

## 学习路线

本 README 只覆盖 **第 0 课（ChatClient 接入层）** 的启动与 API。

完整的 **架构级、可上线** 学习路线与量化毕业项目：

- **[learn/README.md](./learn/README.md)** — 每阶段：提示词、必懂、验收题（答案分开）
- **[QUANT_ARCHITECTURE.md](./QUANT_ARCHITECTURE.md)** — 量化多 Agent 如何配合（投研环 / 交易环）

## 项目结构

```
src/main/java/com/edy/firstai/
  FirstAiApplication.java
  chat/                 # 同步 / 流式 / 结构化输出
  multimodel/           # 路由与对比
  config/               # 多模型装配 + Retry
  exception/            # 统一错误契约
  metrics/              # Micrometer 业务打点
```
