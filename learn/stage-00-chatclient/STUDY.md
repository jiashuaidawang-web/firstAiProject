# 阶段 0 · 模型接入层（平台 v0.1）

**量化验收产物**：`MarketBriefAgent`  
一句话研究目标 → SSE 流式 + **结构化市场简报 JSON**。本阶段 **禁止 Tool**，禁止查真实行情。

仓库里已有 ChatClient / SSE / `.entity()` 骨架。本阶段任务是：**跑通、读懂、补成可讲的 v0.1**，不是再搭一个 Hello World。

---

## ★ 你必须能教给别人的 6 件事（代码可以 AI 写，这 6 件必须你讲）

1. **ChatModel vs ChatClient**  
   Model 是 Provider 适配（像 RestTemplate）；Client 是用户 API（像 RestClient）。Advisor、结构化输出、以后的 Tool 都挂在 Client 上。  
   讲不清这一层，后面全是调库。

2. **一次 `call()` 在网络上发生了什么**  
   组装 messages（system/user）→ HTTP `POST .../chat/completions` → 拿 assistant 文本 / usage。  
   LongCat 只是 OpenAI 兼容网关：`base-url=https://api.longcat.chat/openai`，Spring AI 再拼 `/v1/chat/completions`。

3. **System vs User**  
   System = 角色与约束（相对稳）；User = 本轮任务。对应 InstructGPT 的「指令跟随」，不是玄学。

4. **流式 vs 同步**  
   模型自回归逐 token 生成。`.stream().content()` 是 `Flux<String>` 增量，**不是** `Flux<ChatResponse>`。SSE 把增量推给浏览器。`.entity()` 不能边流边成完整对象。

5. **结构化输出为什么是契约**  
   `.entity(Xxx.class)` ≈ 生成 JSON Schema → 约束模型 → 反序列化成 Java。  
   下游量化 Agent 全部吃 JSON，不吃散文。`useProviderStructuredOutput()` 是把 schema 交给模型 API，比「Prompt 里求求你输出 JSON」可靠。

6. **多模型时为什么不能 `ChatClient.create(model)` 乱 new**  
   会丢掉 Observation / Customizer / 默认 Advisor。官方用 `ChatClientBuilderConfigurer`。本项目 LongCat 走 `spring.ai.openai.*`。

---

## 必读（只这些）

| 优先级 | 读什么 | 读到什么程度 |
|--------|--------|--------------|
| ★ 必读 | [ChatClient](https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html) 创建、call/stream、entity 三节 | 能默写 `prompt().user().call().content()` 和 `stream().content()` |
| ★ 必读 | 本仓库 `ChatController` / `StreamChatController` / `StructuredOutputController` / `MultiModelConfig` | 能指着代码讲上面 6 点 |
| 扫一眼 | [OpenAI Chat](https://docs.spring.io/spring-ai/reference/2.0/api/chat/openai-chat.html) 的 base-url / options | 知道兼容协议怎么换网关 |
| 扫一眼 | InstructGPT 摘要（Ouyang 2022） | 「为什么要有 system」有出处 |
| 扫一眼 | Holtzman ICLR 2020 摘要 | temperature 在干什么 |

不要读：RAG、Tool、LangGraph。

---

## ★ 必须看懂的代码（AI 写完你要对着讲）

| 文件 | 你要讲的逻辑 |
|------|----------------|
| `application.yml` | `spring.ai.openai.base-url` 为什么是 `.../openai` 而不是带 `/v1` |
| `MultiModelConfig` | 多个 ChatClient 怎么建；LongCat 为什么还是 `OpenAiChatModel` |
| `ChatController` | `call().content()` vs `chatResponse()`（usage 在哪） |
| `StreamChatController` | `Flux<String>` + `TEXT_EVENT_STREAM` |
| `StructuredOutputController` | `.entity()` 与 `validateSchema` |
| `RetryConfig` | Boot 4 用 `org.springframework.core.retry`，不是旧 `spring-retry` |
| `AIMetricsService` | 业务打点 vs ChatClient 自带 Observation |

---

## 利用 AI 写代码（一步一个提示词）

把每段复制到 Cursor Agent / Claude。**一次只跑一步。** 跑完用「必懂」检查自己能不能讲。

### 提示词 0 — 约束（每次对话开头贴一次）

```
你在仓库 firstAiProject 中改代码。JDK 21，Spring Boot 4.1，Spring AI 2.0。
主模型是 LongCat（spring.ai.openai.*，OpenAI 兼容）。
本阶段是阶段 0：禁止 Tool Calling、禁止 RAG、禁止接行情 API。
不要新开模块当 Demo。多模型必须保留 ChatClientBuilderConfigurer 的官方写法。
每个 PR 级改动后，在回复里列出「我（开发者）必须能讲清的 3 句话」，不要只贴代码。
```

### 提示词 1 — 先跑通现有接口

```
不要改代码。告诉我如何用 JDK 21 启动本项目，需要哪些环境变量。
列出验证这 3 个 URL 的 curl（chat / stream / structured）。
如果缺 LONGCAT_API_KEY 会怎样失败，从配置类讲清楚。
```

### 提示词 2 — 落地 MarketBriefAgent

```
在本仓库新增量化验收接口（不要新项目）：
- GET 或 POST /api/quant/brief?goal=
- 使用已有 openAiChatClient
- System：你是投研助理，不得编造具体成交价/未提供的数据；只给研究框架
- 输出 Java record：symbols（List<String>）、logic、uncertainties、nextActions
- 同步用 .entity()；另提供 SSE 版只流式输出 logic 文本（说明 entity 不能边流边解析）
- 注释标出 ChatClient 调用链，便于我讲解
完成后不要加 Tool。
```

### 提示词 3 — 补生产缺口（仍属阶段 0）

```
为 Chat / Brief 接口补：
1) 整体 read timeout（对 LLM HTTP）
2) 未配置的 DeepSeek/Qwen 不要让应用启动失败（optional bean 或配置开关）
3) 日志禁止打印 api-key
不要做鉴权全家桶，点到为止。列出超时发生在哪一层（Boot 的 HTTP client vs 业务）。
```

### 提示词 4 — 逼 AI 教你（代码写完立刻用）

```
不要继续加功能。针对 MarketBriefAgent 和 ChatClient 调用链，用苏格拉底方式问我 5 个问题，
每个问题先留空，等我回答后再给对错。重点必须覆盖：call vs stream、entity 原理、base-url 拼接。
```

---

## 阶段完成标准（没做到就还是 Demo）

- [ ] LongCat 真实对话成功（不是 mock）
- [ ] `/api/quant/brief` 返回合法 JSON record
- [ ] SSE 能在浏览器或 curl `-N` 看到增量
- [ ] 你能不看代码，在纸上画出 call / stream / entity 三条路径
- [ ] QUESTIONS.md 闭卷写完，再对 ANSWERS.md
