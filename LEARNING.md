# 生产级 AI Agent 应用架构学习路线

面向 **10 年 Java 架构师转 AI Agent 应用架构**。目标不是「会调 Chat API」，而是能独立负责一条 **可上线、可分布式部署、可抗高并发、可观测、可计价、可降级** 的 Agent 产品线。

**毕业项目（唯一主战场）**：Quant Agent Platform ——「一个人 + 一支 24 小时 AI 量化投研团队」。  
协作关系见 [QUANT_ARCHITECTURE.md](./QUANT_ARCHITECTURE.md)。  
边界：工程化研究闭环 ≠ 天然赚钱。

本仓库是 **第 0 课（ChatClient 接入层）**。每一阶段产出的不是新 Demo 仓库，而是 **同一平台的一个可运行版本**，并绑定一个（组）量化 Agent 作为验收。

后续每一阶段都按「Agent 技术 → 量化验收 Agent → 生产门槛 → 文档/论文」组织。

---

## 0. 毕业标准（什么叫架构级、什么叫可上线）

你可以对外签字的系统，至少同时满足：

| 维度 | 架构级要求 | 不达标的典型形态 |
|------|------------|------------------|
| 产品闭环 | 用户任务可完成：对话 + 工具 + 知识 + 记忆 + 人工确认 | 只能聊天，不能办事 |
| 正确性 | 结构化输出、工具参数校验、检索命中可评估 | 靠「模型自己说对了」 |
| 可靠性 | 超时、重试、熔断、降级、幂等、死信 | 429/5xx 直接 500 给用户 |
| 并发 | 流式连接、排队、背压、租户隔离、虚拟线程/异步 | 一个请求占死一个 Tomcat 线程直到 LLM 返回 |
| 分布式 | 无状态应用节点 + 外置会话/锁/队列/缓存 | 会话存在 JVM 内存，扩容丢上下文 |
| 可观测 | Trace + Metrics + Log + 成本 + 质量，能按租户/模型/工具下钻 | 只有 `System.out` 和成功/失败计数 |
| 安全 | 提示注入防护、工具权限、密钥、审计、PII | API Key 进仓库、工具无 ACL |
| 成本/SLO | TTFT、端到端延迟、token 预算、单次任务成本上限 | 无限生成、无限重试、无限 RAG |
| 交付 | 配置中心、灰度、Prompt/模型版本、回滚 | 改 Prompt 就要发版且无法回溯 |

市面上 2025–2026 年招「Agent 应用架构 / AI 应用研发」时，以上是默认 implicit 要求，不是加分项。

---

## 1. 能力地图：现在做 Agent 产品必须会的

按 **用户请求穿越系统** 的路径看，而不是按框架模块看。

```
客户端 (SSE / WebSocket / 异步任务查询)
        │
API Gateway：鉴权、租户、限流、WAF
        │
BFF / Agent API：会话、幂等键、任务 ID
        │
编排层 (Orchestrator)
  ├─ Planner / Router          选模型、选工具、是否走 RAG
  ├─ Agent Loop                Think → Act → Observe（可多轮）
  ├─ Tool Gateway              权限、超时、沙箱、审计
  ├─ Knowledge (RAG)           检索、重排、引用、新鲜度
  ├─ Memory                    短期会话 + 长期画像（外置存储）
  └─ HITL                      高风险工具人工确认
        │
模型网关 (Model Gateway)
  ├─ Provider 适配 (OpenAI 兼容 / 原生)
  ├─ 路由 / 降级 / 配额
  └─ 缓存（语义缓存，可选）
        │
基础设施
  Redis / MQ / 向量库 / 对象存储 / 可观测后端
```

### 1.1 必须具备（没有就不能叫 Agent 应用）

1. **模型接入与协议兼容**：OpenAI 兼容网关、多模型、超时与重试（本仓库已覆盖雏形）。
2. **流式交互**：SSE/WebSocket、断线续传策略、首字延迟（TTFT）。
3. **结构化输出**：JSON Schema / `.entity()`，作为下游系统的契约，而不是自由文本。
4. **Tool Calling**：模型调你的业务 API；参数校验、超时、副作用控制。
5. **MCP 或等价工具总线**：工具可被多个 Agent/IDE/运行时复用（市场标配）。
6. **会话记忆**：多轮上下文窗口管理、摘要、外置存储。
7. **RAG**：文档接入、切片、嵌入、检索、引用、更新管道。
8. **Agent 循环**：ReAct / Plan-Execute；有最大步数、最大 token、最大墙钟时间。
9. **Guardrail**：输入输出过滤、提示注入、工具白名单。
10. **评测**：Golden set、回归、LLM-as-Judge 只作辅助，关键路径要有确定性断言。

### 1.2 上线还必须具备（没有就不敢分布式放量）

11. **租户与配额**：按租户 QPS、token、并发流式连接数隔离。
12. **幂等与任务模型**：长 Agent 不能同步 HTTP 一把梭；要有 `taskId`、状态机、可查询、可取消。
13. **消息队列与执行器**：慢工具/长推理进 Worker；API 节点保持瘦。
14. **分布式会话与锁**：Redis 存 memory；同一会话并发写入要串行或 CRDT/合并策略。
15. **可观测三支柱 + GenAI 语义**：prompt/completion tokens、模型名、tool 名、cache hit、检索命中。
16. **成本治理**：单请求预算、日预算、缓存、小模型分流、截断策略。
17. **发布体系**：Prompt 版本、模型别名、A/B、回滚，与代码发布解耦。
18. **安全合规**：密钥托管、审计日志、数据驻留、脱敏。

对照框架（学概念，不换栈）：

| 能力 | Spring AI 2.0 | LangChain / 生态 |
|------|---------------|------------------|
| 对话 | `ChatClient` | Chat Model + LCEL |
| 拦截/循环 | Advisor 链（含 ToolCallingAdvisor） | Middleware / Agent Executor |
| 工具 | `@Tool` / `ToolCallback` | Tools / ToolNode |
| 工具互操作 | MCP Client/Server | MCP |
| 记忆 | Chat Memory Advisors | Memory / Checkpointer |
| RAG | Advisor + VectorStore | Retriever / RAG pipeline |
| 图编排 | Spring 工作流 / 自研状态机；关注 Spring AI + 工作流结合 | LangGraph |
| 观测 | Micrometer Observation | LangSmith / OTel |

官方入口：

- Spring AI ChatClient：https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html
- Spring AI 2.0 GA：https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now
- LangChain Concepts：https://python.langchain.com/docs/concepts/
- MCP：https://modelcontextprotocol.io
- OpenTelemetry GenAI：https://opentelemetry.io/docs/specs/semconv/gen-ai/
- OWASP LLM Top 10：https://owasp.org/www-project-top-10-for-large-language-model-applications/

---

## 2. 生产参考架构（分布式 / 高并发 / 可观测）

你已有的微服务经验直接复用。Agent 只是把「下游 RPC」换成「模型 + 工具 + 检索」，但 **状态更长、尾延迟更高、失败模式更脏**。

```
                    ┌─────────────┐
  用户 ──SSE/WS──►  │  API 节点   │  ×N  (无状态, JDK 21 虚拟线程)
                    │  BFF/Chat   │
                    └──────┬──────┘
           短请求同步 │        │ 长任务投递
                     │        ▼
                     │  ┌─────────────┐     ┌──────────────┐
                     │  │  MQ (任务)  │────►│ Agent Worker │ ×N
                     │  └─────────────┘     └──────┬───────┘
                     │                             │
         ┌───────────┴──────────┬────────────┬─────┴──────┐
         ▼                      ▼            ▼            ▼
   Redis 会话/配额         向量库/检索引擎   模型网关     业务系统
   分布式锁/限流            对象存储文档     Provider     工具 ACL
         │                      │            │            │
         └──────────────────────┴────────────┴────────────┘
                                ▼
                     OTel Collector → Trace/Metrics/Log
                     成本账本 / 评测任务 / 审计仓
```

### 2.1 并发模型（Java 架构师必做的选择）

LLM 调用是 **高延迟 IO（秒级）**，不是你熟悉的 20ms RPC。

| 场景 | 推荐 | 原因 |
|------|------|------|
| 同步编排、工具是阻塞 JDBC/RPC | JDK 21 **虚拟线程** | 代码保持命令式，扛得住大量阻塞等待 |
| SSE 流式输出 | WebFlux `Flux` 或 MVC + Reactor | 与 Spring AI `.stream()` 对齐 |
| 长 Agent（多步工具，分钟级） | HTTP 202 + Worker + 查询/推送 | 不要占用网关超时预算 |
| 突发流量 | 令牌桶 + 队列削峰 + 拒绝有损 | 保护 Provider 配额和自身线程 |

原则：**API 层不跑长循环**。Agent loop 进 Worker；API 只负责建任务、鉴权、推流订阅。

### 2.2 分布式部署要点

- **应用无状态**：会话、记忆、幂等键、任务状态全部外置。
- **粘滞会话不是架构**：SSE 可用 Redis Pub/Sub 或 Gateway 把事件推回任意节点。
- **同一 conversationId 写入串行**：Redis 锁或单分区队列，避免多 Worker 同时 append memory。
- **工具副作用幂等**：`Idempotency-Key` 贯穿网关 → Agent → 业务 API。
- **配置与密钥**：配置中心 + Secret Manager；模型名用别名（`chat.primary`），不要把 `LongCat-2.0` 写死在代码里。
- **多活**：向量库、Redis、MQ 的一致性模型要先定；RAG 允许最终一致，扣款/下单不允许。

### 2.3 SLO 建议（上线前先写进设计，而不是上线后拍脑袋）

| 指标 | 建议起点（按业务再调） |
|------|------------------------|
| TTFT（流式首 token） | P95 < 2s（取决于 Provider） |
| 简单问答端到端 | P95 < 8s |
| 带 1–3 次工具的任务 | P95 < 30s，超时有明确状态 |
| 错误率（5xx + 业务失败） | < 1%，可重试错误单独记账 |
| 流式连接并发 | 按节点 FD/内存压测得出，设硬顶 |
| 单次任务成本 | 硬预算（token + 检索 + 工具）超限中止 |

---

## 3. 分阶段学习路线

每阶段结束都有 **生产验收**。没过验收不要进入下一阶段堆功能。

时间是「有 Java 架构基础、每周可投入主路径」的估计，按你节奏压缩或拉长。

### 阶段 0 — 模型接入层（本仓库，已完成骨架）

**能力**：ChatClient、System/User、流式 SSE、结构化输出、多模型路由、HTTP 重试、基础指标。

**你要补到生产级的缺口**（本仓库还不是上线形态）：

- [ ] API Key 只走环境变量 / Secret，轮换你曾泄露过的 Key
- [ ] 网关鉴权、租户 ID 贯穿 MDC 与 Observation
- [ ] 同步接口超时（connect/read/整体 deadline）
- [ ] 流式断开、取消与 Provider 侧取消对齐
- [ ] DeepSeek / Qwen 未配置时不要在启动期硬失败（可选模型）
- [ ] 压测：同步 QPS、SSE 并发连接、错误注入

**量化验收 Agent（平台 v0.1）**：**MarketBriefAgent**  
输入一句话研究目标，流式输出 + **结构化市场简报 JSON**（标的、逻辑、不确定点、下一步建议）。无工具、不查真实行情——先把接入层、契约、SSE、观测打成可运维骨架。

**文档**：Spring AI Getting Started、ChatClient、OpenAI Chat（兼容协议接入 LongCat）。

---

### 阶段 1 — Tool Calling 与 Advisor 链（Agent 真正开始）

**为什么是架构课**：没有工具，LLM 只是文本生成器。有工具，它成为 **不可信的分布式调用方**。你要当它是「会胡乱调 API 的客户端」。

**必须掌握**：

1. Spring AI 2.0 把 **tool-loop 提升为 Advisor**（`ToolCallingAdvisor` 自动注册）。
2. 工具粒度：只暴露 **业务能力**（下单、查库存），不暴露万能 SQL。
3. 每个工具：JSON Schema 入参、超时、重试策略（只对读接口）、权限、审计。
4. 循环熔断：`maxToolCalls`、墙钟超时、重复调用检测。
5. 高风险工具：**Human-in-the-loop**（确认票），不能模型说写就写。

**生产验收**：

- 给定「查 X 再汇总」任务，Agent 能稳定调工具并给出带引用的结论
- 工具失败时有降级文案，不会死循环
- Trace 里能看到：模型调用 span + 每个 tool span
- 越权 tool 名或越权租户数据被拒绝并审计

**文档 / 论文**：

- Spring AI Tool Calling（各 Chat 章节，如 OpenAI / DeepSeek）
- MCP 规范：https://modelcontextprotocol.io
- Yao et al., *ReAct*, ICLR 2023
- Schick et al., *Toolformer*, 2023

**量化验收 Agent（平台 v0.2）**：**MarketDataAgent** + **DataQualityAgent**  
工具拉行情/资金流；质量 Agent 检查缺失、重复、时间错位、源冲突。Research 不得使用未打质量标记的数据。Trace 必须能看到每个 tool span。

**市场对应岗位技能**：Function Calling、MCP Server/Client、Agent Loop。

---

### 阶段 2 — 记忆、会话、上下文窗口治理

**为什么**：分布式下「多轮对话」= 外部状态机。窗口满了还硬塞，成本和胡话一起涨。

**必须掌握**：

1. 短期记忆：最近 N 轮，外置 Redis / DB。
2. 窗口策略：滑动窗口、摘要（summarization memory）、关键事实抽取。
3. 长期记忆：用户偏好、已办事项；写入要审核（防投毒）。
4. 会话并发：同一 `conversationId` 的 append 串行化。
5. 与流式结合：未完成的 assistant 消息如何落库。

**生产验收**：

- 扩容/重启后会话仍在
- 超长会话费用可控（有摘要，不无限拼接）
- 用户「忘记刚才说的」可复现、可定位是截断还是丢消息

**量化验收 Agent（平台 v0.3）**：**ResearchMemoryAgent**  
同一 `researchId` 下：假设、中间结论、已跑实验可恢复；扩容/重启不丢。窗口满了要摘要，禁止无限拼接 K 线进 Prompt。

**文档**：Spring AI Chat Memory；LangChain Memory / LangGraph Checkpointer。

---

### 阶段 3 — RAG（企业 Agent 的默认知识路径）

**为什么**：企业数据不能全塞进 Prompt。RAG 是 **数据产品 + 检索系统**，不是 `vector.similaritySearch` 一行。

**必须掌握**：

1. 数据管道：接入、清洗、切片（chunk）、元数据（权限、时间、来源）。
2. 嵌入模型与向量库：更新、删除、版本、租户隔离（必须过滤 ACL）。
3. 检索：混合检索（关键词 + 向量）、重排（Rerank）、查询改写。
4. 生成：带 citation；拒答（检索分数不够就说不知道）。
5. 评估：Recall@K、引用忠实度、幻觉率；没有评测的 RAG 不能上生产。

**生产验收**：

- 权限：用户 A 检索不到用户 B 的文档
- 文档更新后，问答在 SLA 内反映新内容
- 能解释「答案来自哪几段」
- 有离线评测集，改切片策略能看指标而不是体感

**文档 / 论文**：

- Spring AI RAG / VectorStore / Advisors
- Lewis et al., *Retrieval-Augmented Generation*, NeurIPS 2020
- Gao et al., *RAG 综述 / 进阶实践*（检索失败是主因，不是模型不够大）

**量化验收 Agent（平台 v0.4）**：**NewsAgent** + **FundamentalAgent**（可含 Industry 知识库）  
财报/公告/研报 RAG，答案必须带 citation；分数不够要拒答。ACL：租户/权限过滤在检索阶段强制。不可信网页/公告与系统指令隔离（防间接注入）。

---

### 阶段 4 — 编排：图、多 Agent、工作流、HITL

**为什么**：单次 ReAct 适合短任务。订单、审批、排障是 **工作流**。架构师要会判断：这是 LLM 循环，还是 BPM/状态机，还是二者混合。

**必须掌握**：

1. 何时 **不要** 用 Agent：规则稳定、要强一致的，用工作流 + 局部 LLM。
2. 图编排：节点（LLM / Tool / HITL / 子 Agent）、边、状态、可恢复。
3. 多 Agent：按职责拆（检索、编码、评论），要有 supervisor 与共享状态协议。
4. 补偿与回滚：工具已执行一半如何 saga。
5. 人机协同：确认、编辑、接管、审计。

**生产验收**：

- 任意一步失败可从 checkpoint 恢复，不从头烧 token
- 高风险节点必须人工，绕过会失败
- 每个节点有超时与死信
- 状态可查询：当前节点、已调用工具、待确认项

**对照**：LangGraph 的 checkpointer / interrupt；你在 Java 侧应对齐「可恢复状态机」而不是新造一轮聊天。

**论文**：Wu et al., *AutoGen*；Hong et al., *MetaGPT*（学组织方式，不要学「无边界多 Agent 聊天」上生产）。

**量化验收 Agent（平台 v0.5）**：**Quant Supervisor（雏形）** + Research 小队（Technical / Industry / Sentiment）  
Supervisor 只拆任务、选 Agent、汇总，自己不写研报。长研究任务走 `taskId` + Worker。高风险动作 HITL。仍 **禁止** 调用 Execution。

---

### 阶段 5 — 可靠性与流量治理（生产底座）

**必须掌握**（全部是你熟悉的分布式问题，套在 LLM 上）：

1. **两层超时**：对 Provider 的 socket 超时 + 对用户的 API 超时 + Worker 墙钟超时。
2. **两层重试**：`spring.ai.retry.*`（HTTP 暂态）与业务重试；写工具禁止盲目重试。
3. **熔断与降级**：主模型 → 备用模型 → 规则/缓存 → 友好失败。
4. **限流**：租户令牌桶、Provider 配额、全局并发闸。
5. **舱壁**：检索、模型、工具线程池/信号量隔离，避免慢检索拖死对话。
6. **缓存**：完全相同请求的响应缓存；语义缓存要极谨慎（易串答）。
7. **背压**：队列深度告警；拒绝要返回 `Retry-After`。

Spring Framework 7 / Boot 4：优先用 `org.springframework.core.retry` 与弹性注解，而不是已归档的 `spring-retry`。

**生产验收**：

- 故障演练：Provider 500、429、超时、半开流式
- 降级路径有指标，能自动切回
- 无雪崩：下游慢时自己先限流

**量化验收 Agent（平台 v0.6）**：**StrategyAgent** + **BacktestAgent**  
策略产出可回测规格（结构化）；回测是长任务：排队、可查询、可取消、可重试（仅引擎暂态失败）。Python Quant Engine 只通过 Tool Gateway 调用。回测失败有死信，不把 API 线程占满。

---

### 阶段 6 — 可观测、评测、成本（没有这三项等于没有架构）

**可观测（每个请求必带的维度）**：

- `tenant`、`conversationId`、`taskId`、`userId`
- `model`、`provider`、`promptTokens`、`completionTokens`、`cachedTokens`
- `toolName`、`toolLatency`、`toolSuccess`
- `ragCollection`、`retrievedDocs`、`rerankScore`
- `ttft`、`totalLatency`、`retryCount`、`fallback`

实现上：Micrometer Observation + OTel Trace；日志禁止把完整 Prompt/PII 明文打到公共日志（分级：debug 采样、脱敏）。

**评测**：

- 离线：Golden dataset，每次改 Prompt/切片/模型跑回归
- 在线：抽样人工 + 关键业务硬指标（下单成功率、检索无结果率）
- LLM-as-Judge 只用于开放生成，且要校准（Zheng et al., NeurIPS 2023）

**成本**：

- 成本作为 **一等指标** 进账单：按租户、按功能
- 预算拦截：超 token 中止 Agent loop
- 路由：简单意图走小模型，复杂走大模型（本仓库 `ModelRouter` 的生产版）

**生产验收**：

- 一次故障能在 Trace 里 5 分钟内定位：是模型、检索、工具还是网关
- 能回答 CFO：这个功能上周花了多少钱、单位任务成本趋势
- 改 Prompt 有版本号，可回滚，评测分数可对比

**量化验收 Agent（平台 v0.7）**：**BacktestAnalystAgent** + **FactorResearchAgent** + 评测集  
回测结果自动归因（确定性指标断言 + 有限的 LLM 解释）。因子 IC/IR/衰减进回归集。Optimizer 若出现：必须有过拟合/重复实验预算，禁止无上限搜参。成本按 `researchId` 可账单。

---

### 阶段 7 — 安全与合规（Agent 比普通 API 危险）

**必须掌握**：

1. **提示注入**：不可信内容（邮件、网页、检索文档）与系统指令隔离；工具前二次校验。
2. **工具 ACL**：模型没有「用户的全部权限」；向下传递用户身份与最小权限。
3. **输出**：防泄露密钥、内部 URL、他人数据；结构化出口再校验。
4. **供应链**：MCP 第三方工具等同第三方依赖，要审核、签名、网络隔离。
5. **数据**：训练/日志退出、驻留区域、保留周期。

**生产验收**：红队用例（恶意文档诱使调用转账工具）必须失败；有审计。

参考：OWASP LLM Top 10；NVIDIA/学术界关于 indirect prompt injection 的工作。

**量化验收 Agent（平台 v0.8）**：**RiskAgent（独立否决权）** + **PortfolioAgent**  
Strategy 不能自己放行。Risk 输出通过/拒绝+原因。红队：恶意公告诱使「直接下单」必须失败。Execution 代码可以存在，但 **未过 Risk Gate + HITL 不得连通券商**。

---

### 阶段 8 — 交付、灰度、平台化（架构师本职）

到这里你才是「应用架构」而不是「会写 Demo 的人」：

1. 环境：dev / staging / prod，模型别名分环境。
2. 发布：Prompt 配置化；模型切换不发版；功能开关。
3. 容量：按 SSE 连接数、Worker 积压、向量 QPS 扩缩容。
4. 多租户 SaaS：数据隔离、配额、专有知识库。
5. 平台化：内部 Model Gateway + Tool Registry + Eval 平台，避免每个业务线自己接一套 Provider。

**生产验收**：完整走一遍「改 Prompt → 评测 → 灰度 5% → 全量 → 回滚」；K8s 上滚动发布不丢会话。

**量化验收 Agent（平台 v1.0）**：**MonitoringAgents** + **ExecutionAgent（仅模拟/纸交易，经 Risk Gate）**  
监控发现失效 → 事件回 Supervisor → 重开投研环。交易环与投研环隔离。密钥托管、审计、HPA 按队列深度。此时才叫 Quant Agent Platform 毕业版，而不是 Agent Demo 合集。

---

## 4. 阶段 = 同一平台的版本，不是一堆 Demo

判断「学到的是 Demo 还是生产级」的方法只有一个：**每个阶段交付可运行的平台版本，并带生产门槛。**  
10 个独立玩具仓库无法验收，因为标准每次都在漂。

| 版本 | Agent 技术阶段 | 必须交出的量化 Agent | 这一版若只有聊天、没有契约，就算失败 |
|------|----------------|----------------------|--------------------------------------|
| v0.1 | 0 接入层 | MarketBriefAgent | 无结构化简报、无 SSE、无基础指标 |
| v0.2 | 1 Tool | MarketData + DataQuality | 无工具审计/超时，或 Research 可用脏数据 |
| v0.3 | 2 Memory | ResearchMemoryAgent | 重启丢假设、无 researchId |
| v0.4 | 3 RAG | News + Fundamental | 无引用、无 ACL、不能拒答 |
| v0.5 | 4 编排 | Supervisor 雏形 + Technical/Industry/Sentiment | Supervisor 自己写研报，或同步卡死长任务 |
| v0.6 | 5 可靠性 | Strategy + Backtest（Worker） | 回测占满 API 线程，或不可取消 |
| v0.7 | 6 观测/评测/成本 | BacktestAnalyst + Factor + 评测 CI | 只看感觉、无回归、无成本账 |
| v0.8 | 7 安全 | Risk（否决权）+ Portfolio | 策略自评自过，或 LLM 能碰到下单接口 |
| v1.0 | 8 平台化 | Monitoring 闭环 + Execution（纸交易+闸） | 无灰度/审计/队列 HPA |

压测报告、故障演练、SLO 面板、评测分数：缺一样，该版本就还是 Demo。

与常见「Prompt→RAG→Tool→Multi-Agent」课表的对应：顺序必须是 **先工具与数据质量，再记忆与 RAG，再 Supervisor，最后才 Execution**。倒过来先堆多智能体，量化上会先出「会聊天的错误下单」。

---

## 5. 架构决策清单（面试/设计评审会问的）

能用 3 分钟讲清每一条，才算这个阶段过关：

1. 为什么主路径用 ChatClient 而不是直接 ChatModel？
2. 为什么 LongCat 走 OpenAI 兼容协议？切换 Provider 要改哪一层？
3. 流式用 SSE 还是 WebSocket？断线怎么办？
4. Agent loop 放 API 进程还是 Worker？如何取消？
5. 工具失败重试还是交给模型再想？依据是读/写？
6. 记忆存在哪？多副本如何不打乱对话顺序？
7. RAG 的 ACL 在过滤阶段还是生成阶段强制？
8. 同一用户刷接口如何保证幂等与配额？
9. 模型 429 时对用户的产品语义是排队、降级还是失败？
10. Prompt 和模型版本如何回滚而不回滚整个应用？
11. 完整 Prompt 能否进日志？谁能看 Trace 里的内容？
12. 单次任务成本上限触发后，状态如何对用户可见？

---

## 6. 阅读与标准（按优先级）

**先读（干活用）**

1. Spring AI 2.0 ChatClient、Tool Calling、Structured Output、Advisors
2. Spring Boot 4 迁移（虚拟线程、原生 retry、starter 更名）
3. OpenTelemetry GenAI Semantic Conventions
4. MCP 规范
5. OWASP LLM Top 10

**再读（把经验变成判断力）**

6. InstructGPT（Ouyang et al., 2022）— 指令跟随从哪来
7. ReAct（Yao et al., 2023）— Agent 循环
8. RAG（Lewis et al., 2020）
9. Neural Text Degeneration（Holtzman et al., 2020）— temperature / nucleus
10. LLM-as-a-Judge（Zheng et al., 2023）— 评测边界
11. AWS *Exponential Backoff and Jitter* — 你已经在用，要知道为什么

**LangChain / LangGraph 文档**：用来对照概念（Memory、Tool、Graph、HITL），实现仍落在 Spring AI + 你熟悉的 Java 基础设施上。

---

## 7. 与本仓库文档的关系

- `README.md`：第 0 课怎么跑起来。
- `QUANT_ARCHITECTURE.md`：量化团队怎么配合、两条环、否决权。
- `LEARNING.md`（本文）：Agent 技术阶段如何变成平台 v0.1–v1.0。

学习顺序：**数据工具与质量 → 记忆/RAG → Supervisor 与投研小队 → 回测长任务 → 独立风控 → 监控闭环。**  
最后才接 Execution。倒过来堆多智能体，是 Demo 很多、生产事故也很多的原因。
