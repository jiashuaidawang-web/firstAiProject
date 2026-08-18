# 阶段 1 · Tool Calling 与 Advisor 链（平台 v0.2）

**量化验收产物**：`MarketDataAgent` + `DataQualityAgent`  
工具取数 → 质量门打标 → 结构化行情。脏数据不得进入「研报」。

**前提**：阶段 0 已对 LongCat 跑通 `call` / `stream` / `entity`。

---

## ★ 你必须能教给别人的 7 件事

1. **模型不跑你的 Java。** 它只输出 tool call（名字 + JSON 参数）。`ToolCallingAdvisor` 在 **你的 JVM** 里 invoke，再把结果当 tool 消息发回模型。第二轮模型才生成最终答案。看不到「提出调用 → 执行 → 再请求模型」三步，等于没学会 Agent。

2. **Advisor 链** 是 Spring AI 2.0 的拦截器 + **可循环**。Tool 循环从「每个 ChatModel 自己写 loop」提升到 Client 侧一等公民。类比 Filter，但允许 re-enter。

3. **ReAct**：Reason + Act。论文要你记住的是循环，不是公式。生产必须给循环加 **maxToolCalls / 墙钟超时**，否则模型会空转烧钱。

4. **工具是 API 产品，不是万能函数。** 暴露 `getDailyBar(symbol, date)`，禁止 `httpGet(url)` / `executeSql`。入参 record = JSON Schema。

5. **读可重试，写不可盲目重试。** 本阶段只有读。失败返回结构化错误给模型，比扔 HTML 堆栈有用。

6. **质量门是业务规则，不是再来一个 LLM 胡判。** `assertBarQuality` 用确定性检查（空值、成交量、日期）。Research 看到 `FAIL` 必须停。

7. **MCP = 工具的 USB 协议。** 这阶段知道就行，先不实现 Server。你的 `@Tool` 以后可以被 MCP 包一层。

---

## 必读

| ★ | [ChatClient](https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html) Tool / Advisor |
| ★ | 你所用模型文档的 Tool Calling 节（OpenAI 兼容同样适用） |
| 扫 | ReAct（Yao, ICLR 2023）摘要 + 那张 Think-Act-Observe 图 |
| 扫 | MCP 首页一段话：https://modelcontextprotocol.io |
| 不读 | 完整 MCP spec、LangGraph |

---

## ★ 必须看懂的逻辑（AI 写完对着讲）

- ChatClient `.tools(...)` 之后，请求里多了 tools 定义（给模型看的 schema）
- 响应 `finish_reason=tool_calls` 时 Advisor 如何执行 Java 方法
- 方法返回值如何变成下一条 message
- `maxToolCalls` 打到上限时你的产品语义（报错 vs 降级）
- 审计字段：toolName、args、latency、success、symbol

---

## 利用 AI（一步一个提示词）

每次开头仍贴阶段 0 的「约束」+ 下面这句：

```
当前阶段 1。只允许只读行情工具 + 质量检查工具。禁止 Execution、禁止 RAG、禁止 Memory Redis。
Agent 循环必须有 maxToolCalls。工具失败不得死循环。
```

### 提示词 1 — 最小闭环（写死数据）

```
在 com.edy.firstai.quant.data 包新增：
- record DailyBar(symbol, tradeDate, open, high, low, close, volume)
- @Tool getDailyBar(symbol, tradeDate) 先返回写死的 600519 一根 K 线
- MarketDataAgent 用 openAiChatClient.prompt().tools(...).user(...).call()
- GET /api/quant/market?symbol=
日志必须能看出：tool call 请求、Java 方法进入、第二次模型调用。
在类注释里用中文画这三步。不要接真实 HTTP 行情。
```

### 提示词 2 — 真数据 + 质量工具

```
把 getDailyBar 改成从 classpath CSV 或你指定的简单 HTTP 读（超时 2s）。
新增 @Tool assertBarQuality(symbol, tradeDate)：检查缺字段、volume<=0、日期格式。
返回 QualityReport{status: PASS|FAIL, reasons:[]}。
MarketDataAgent 最终 JSON 必须带 quality。FAIL 时不得编造 close。
入参校验：symbol 必须是 6 位数字。非法直接拒绝并审计，不要让模型绕过。
```

### 提示词 3 — 熔断与观测

```
限制 maxToolCalls=5。注入：工具抛 TimeoutException、返回空 bar。
用户应收到降级文案，进程不得空转。
Micrometer counter: quant.tool.calls{tool,status}。
不要引入新框架。
```

### 提示词 4 — 考你

```
基于当前 diff，问我：模型有没有执行 Java？Advisor 在第几跳介入？
质量检查为什么不该再叫一次 LLM？等我口答，再纠正。
```

---

## 完成标准

- [ ] 日志里能指出三步循环
- [ ] `/api/quant/market` 带 `quality`
- [ ] FAIL / 超时 / 超 maxToolCalls 都有产品行为
- [ ] 闭卷 QUESTIONS.md
