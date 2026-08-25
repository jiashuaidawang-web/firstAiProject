# A-Quant Agent OS
# 60 天生产级 AI 量化 / Agent 架构师学习与项目作战总纲

> **目标：用 60 天完成一个真实的 A 股 AI 量化项目，同时系统补齐中国 AI 应用 / Agent 工程师岗位最核心的能力。**
>
> 这不是“学完几个框架，然后做一个聊天 Demo”。
>
> 最终目标是：
>
> **项目能力 + Agent 架构能力 + AI 应用工程能力 + 量化工程能力，一起沉淀成可以展示、可以答辩、可以继续迭代的完整作品集。**

---

# 一、60 天后的最终成品

## 1. 最终系统

项目名称暂定：

```text
A-Quant Agent OS
```

一句话定义：

> **一个面向 A 股研究场景的生产级 AI Quant Agent 平台。它能够基于真实市场数据、金融资料和量化工具完成研究任务，通过 RAG、Tool Calling、MCP、Single Agent / Multi-Agent、Memory 和 Eval 形成完整的 AI 投研工作流。**

最终系统不是简单的：

```text
用户
  ↓
大模型
  ↓
回答一段股票分析
```

而是：

```text
                         ┌─────────────────────┐
                         │     用户 / API       │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   AI Application    │
                         │   Spring Boot API   │
                         └──────────┬──────────┘
                                    │
                                    ▼
                   ┌─────────────────────────────┐
                   │        Agent Runtime         │
                   │                              │
                   │ Task / State / Plan / Trace  │
                   └──────────────┬──────────────┘
                                  │
             ┌────────────────────┼────────────────────┐
             │                    │                    │
             ▼                    ▼                    ▼
      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
      │ LLM Gateway │      │     RAG     │      │ Tool Calling│
      │ 多模型路由   │      │ 金融知识检索 │      │ 真实能力调用 │
      └─────────────┘      └─────────────┘      └──────┬──────┘
                                                       │
                                              ┌────────┴────────┐
                                              │       MCP        │
                                              │ 远程能力连接层    │
                                              └────────┬────────┘
                                                       │
                    ┌──────────────────────────────────┼─────────────────────────────────┐
                    │                                  │                                 │
                    ▼                                  ▼                                 ▼
             ┌─────────────┐                    ┌─────────────┐                   ┌─────────────┐
             │ 市场数据系统 │                    │ Quant Engine│                   │ 金融资料系统 │
             │ 行情/财务/日历│                    │ 因子/策略/回测│                  │ 公告/研报/RAG │
             └──────┬──────┘                    └──────┬──────┘                   └──────┬──────┘
                    │                                  │                                 │
                    └──────────────────────┬───────────┴────────────┬──────────────────┘
                                           │                        │
                                           ▼                        ▼
                                  ┌────────────────┐       ┌────────────────┐
                                  │   Data Layer   │       │ Evaluation     │
                                  │ ClickHouse     │       │ RAG / Tool /   │
                                  │ MySQL / Redis  │       │ Agent / Report │
                                  └────────────────┘       └────────────────┘
```

---

## 2. 最终用户可以提出什么问题

例如：

> “分析某只 A 股在当前研究时间点是否值得进一步关注，结合近一年价格趋势、技术指标、基本面、相关新闻和历史策略表现给出研究报告。”

系统不能直接靠 LLM 胡说。

正确流程应该是：

```text
用户问题
    ↓
Supervisor / Router
    ↓
任务理解
    ↓
选择需要的能力
    ├── 市场数据 Tool
    ├── 财务数据 Tool
    ├── 金融 RAG
    ├── 指标计算 Tool
    ├── 回测 Tool
    └── Risk Check Tool
    ↓
真实数据返回
    ↓
专业 Agent 分析
    ├── Market Agent
    ├── Research Agent
    ├── Quant Agent
    ├── Risk Agent
    └── Report Agent
    ↓
证据汇总
    ↓
Risk Check
    ↓
最终结构化研究报告
```

最终报告至少包含：

```text
1. 研究问题
2. 数据时间范围
3. 市场表现
4. 技术指标
5. 基本面数据
6. RAG 检索证据
7. 量化策略/回测结果
8. 风险提示
9. 数据来源
10. Agent Trace
11. Eval 结果
```

---

## 3. 最终 Quant Engine

量化部分不是为了做一个“AI 猜涨跌”。

而是建立真实、可验证的量化基础：

```text
Market Data
    ↓
Data Quality
    ↓
Feature / Factor
    ↓
Strategy
    ↓
Signal
    ↓
Portfolio
    ↓
Execution Simulator
    ↓
Backtest
    ↓
Performance Metrics
    ↓
Risk Analysis
```

至少包含：

- A 股基础行情数据
- 历史 K 线
- 财务数据
- 交易日历
- 数据质量校验
- MA
- RSI
- MACD
- Momentum
- Volatility
- 多个基础策略
- Strategy 抽象
- Backtest Engine
- Transaction Cost
- Slippage
- Look-ahead Bias Guard
- Out-of-Sample 验证
- Sharpe Ratio
- Sortino Ratio
- Maximum Drawdown
- Equity Curve
- Strategy Comparison

核心原则：

> **AI 可以决定“要研究什么、调用什么工具、如何组织分析”；Quant Engine 负责真实计算。**

也就是说：

```text
AI ≠ Quant Engine

AI
负责：
- 任务理解
- 工具选择
- 策略选择
- 结果解释
- 报告生成

Quant Engine
负责：
- 指标计算
- 策略运行
- 回测
- 收益计算
- 风险计算
```

---

# 二、最终不是一个项目，而是一个完整作品集

60 天完成后，你得到的应该不是一个 Git 仓库，而是一个完整的能力证明体系。

---

## 作品集 1：生产级 AI Application Backend

你本身是 Java 架构背景。

因此最有价值的路线不是：

```text
放弃 Java
→ 从零变成 Python Agent 开发者
```

而是：

```text
Java 架构师
+
AI Application Engineering
+
Agent Architecture
=
企业级 AI Agent 工程师 / 架构师
```

你最终应该能展示：

```text
Java 21
Spring Boot
Spring AI
Clean Architecture
DDD / Domain Model
Modular Monolith
REST API
Async Pipeline
Redis
MQ
ClickHouse
MySQL
Docker
Observability
```

---

## 作品集 2：A 股量化数据与回测系统

这部分证明你不是只有 AI Demo。

你有：

```text
Market Data Provider
        ↓
Normalization
        ↓
Data Quality
        ↓
ClickHouse
        ↓
Feature
        ↓
Strategy
        ↓
Backtest
        ↓
Metrics
```

这个项目可以单独作为：

> **A 股 Quant Research Platform**

---

## 作品集 3：金融 RAG 系统

展示：

```text
Document Ingestion
    ↓
Chunking
    ↓
Embedding
    ↓
Vector Search
    +
Keyword Search
    ↓
Hybrid Retrieval
    ↓
RRF
    ↓
Reranker
    ↓
Citation
    ↓
RAG Evaluation
```

最终能力：

- 文档解析
- Chunk 策略
- Metadata
- Hybrid Retrieval
- Reranker
- Citation
- Grounding
- Faithfulness
- Retrieval Eval
- Regression Eval

---

## 作品集 4：Agent Runtime

这部分是你未来“Agent 架构师”最重要的资产。

你会真正理解：

```text
Agent State
Task
Plan
Action
Observation
Tool Call
Checkpoint
Retry
Failure Recovery
Human-in-the-loop
Trace
```

最终不是只会：

```python
while True:
    ask_llm()
```

而是知道：

> **一个 Agent 系统本质上是一个带状态、带外部能力、带失败恢复、带可观测性的执行系统。**

---

## 作品集 5：Tool Calling + MCP

最终：

```text
Agent
  ↓
Tool Contract
  ↓
Validation
  ↓
Permission
  ↓
Execution
  ↓
Audit
  ↓
Structured Result
```

同时：

```text
Agent Runtime
      ↓
MCP Client
      ↓
MCP Server
      ↓
Market / Quant Capability
```

你需要真正理解：

> MCP 是能力连接协议。

它不是 Agent。

它也不是 RAG。

它解决的是：

```text
如何让模型 / Agent
以标准协议
发现并调用外部能力。
```

---

## 作品集 6：Multi-Agent System

最终包含：

```text
                 Supervisor
                      │
       ┌──────────────┼──────────────┐
       │              │              │
       ▼              ▼              ▼
 Market Agent    Research Agent   Quant Agent
       │              │              │
       └──────────────┼──────────────┘
                      │
                      ▼
                  Risk Agent
                      │
                      ▼
                 Report Agent
```

关键不是 Agent 越多越厉害。

而是必须回答：

```text
为什么拆？
拆完有什么收益？
上下文是否隔离？
工具是否不同？
职责是否不同？
是否真的提升了成功率？
成本是否增加？
是否应该退回 Single Agent？
```

---

## 作品集 7：Memory System

包括：

```text
Short-term Memory
    ↓
当前任务上下文

Long-term Memory
    ↓
用户研究偏好
关注股票
历史研究结论

Memory Retrieval
    ↓
当前任务检索相关记忆

Memory Policy
    ↓
什么值得写
什么不值得写
什么时候过期
```

必须理解：

> Memory 不是把所有聊天记录塞进向量数据库。

---

## 作品集 8：Evaluation Platform

这是中国市场很多 AI 项目都缺失的一部分，也是你和普通 AI Demo 开发者拉开差距的关键。

最终建立：

```text
Eval Dataset
     │
     ├── RAG Eval
     ├── Tool Eval
     ├── Agent Eval
     ├── Routing Eval
     ├── Report Eval
     └── Regression Eval
```

指标包括：

```text
Retrieval Recall
Citation Correctness
Tool Accuracy
Task Success Rate
Routing Accuracy
Latency
Token Usage
Cost
Faithfulness
Regression Rate
```

核心思想：

> **没有 Eval，就不知道你的 Agent 改动到底是变好了还是变坏了。**

---

# 三、先确定你的技术路线

你的路线不应该是纯 Python Agent 路线。

更适合你的是：

```text
                 ┌───────────────────────┐
                 │   AI Agent Architect  │
                 └───────────┬───────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
 Java Backend         AI Application         Quant Domain
         │                   │                   │
 Spring Boot          RAG                     Market Data
 DDD                   Agent Runtime           Factor
 Redis                 Tool Calling            Strategy
 MQ                    MCP                     Backtest
 ClickHouse            Memory                  Risk
 Docker                Eval                    Research
 Observability         Multi-Agent             Validation
```

推荐主技术栈：

## 后端

```text
Java 21
Spring Boot
Spring AI
Spring Web
Spring Data
Redis
RabbitMQ
MySQL
ClickHouse
```

## AI

```text
LLM Gateway
OpenAI-compatible API
Spring AI
RAG
Embedding
Hybrid Retrieval
Reranker
Tool Calling
MCP
Agent Runtime
Memory
Evaluation
```

## Quant

```text
Java Domain
Python Research / Data Tools（必要时）
Market Data
Feature
Factor
Strategy
Backtest
Metrics
Risk
```

## Infrastructure

```text
Docker
Docker Compose
Prometheus / Metrics 思路
Structured Logging
Trace
Health Check
```

---

## 为什么不是一上来上 LangGraph / Python？

不是说它们不好。

而是你的优势已经是：

```text
Java
Spring
企业后端
系统架构
中间件
数据库
Docker
```

因此正确升级路线是：

```text
已有 Java 企业架构能力
        ↓
理解 AI Application Architecture
        ↓
理解 Agent Runtime
        ↓
理解 RAG / MCP / Tool Calling
        ↓
理解 Eval
        ↓
理解 Multi-Agent
        ↓
形成 AI + Backend + Domain 的复合能力
```

你可以学习 LangChain / LangGraph 的设计思想和官方文档，但不要为了“学框架”而把项目主架构全部切换掉。

你的最终核心能力应该是：

> **我可以不用任何特定框架，也能解释一个生产级 Agent Runtime 应该有哪些模块和边界。**

---

# 四、整个 60 天分为 9 个阶段

---

## Phase 0：Day 01 - Day 03

# 项目定义与架构基础

目标：

```text
确定项目
确定边界
确定领域模型
确定模块边界
```

完成：

- PRD
- MVP
- Non-goals
- Domain Model
- DDD Boundary
- Modular Monolith
- ADR

产物：

```text
A-Quant Agent OS Architecture v1
```

---

## Phase 1：Day 04 - Day 10

# A 股数据底座

完成：

```text
Provider SPI
    ↓
Data Normalization
    ↓
Data Quality
    ↓
ClickHouse
    ↓
Query API
    ↓
Async Pipeline
    ↓
Observability
```

目标：

> 建立一个未来所有 Quant、RAG、Agent 都可以复用的数据底座。

---

## Phase 2：Day 11 - Day 17

# Quant Engine

完成：

```text
Factor
Feature
Strategy
Signal
Portfolio
Backtest
Metrics
Bias Guard
OOS Validation
```

这是整个项目的真实计算核心。

Agent 未来调用的必须是真实 Quant Tool。

---

## Phase 3：Day 18 - Day 24

# LLM Gateway + Financial RAG

完成：

```text
Multi-Model Gateway
Failover
Retry
Circuit Breaker

Document Ingestion
Chunking
Embedding
Hybrid Retrieval
Reranker
Citation
RAG Eval
```

---

## Phase 4：Day 25 - Day 31

# Tool Calling + MCP

完成：

```text
Tool Contract
Tool Guardrail
Tool Calling
MCP Server
MCP Client
Tool Discovery
Progressive Disclosure
```

目标：

> 让 Agent 真正拥有调用 Quant、Data、RAG 能力的手和脚。

---

## Phase 5：Day 32 - Day 38

# Single Agent Runtime

完成：

```text
Agent State
ReAct
Planner
Executor
Failure Recovery
Human-in-the-loop
Trace
Single Agent Eval
```

目标：

> 真正理解 Agent 是如何运行的。

---

## Phase 6：Day 39 - Day 45

# Multi-Agent

完成：

```text
Supervisor
Router
Market Agent
Research Agent
Quant Agent
Risk Agent
Report Agent
Multi-Agent Evaluation
```

关键目标：

> 用数据证明 Multi-Agent 是否真的比 Single Agent 更好。

---

## Phase 7：Day 46 - Day 52

# Memory + Evaluation

完成：

```text
Short-term Memory
Long-term Memory
Memory Retrieval

Eval Dataset
Rule-based Eval
LLM-as-Judge
Regression Eval
```

这是从“能跑”走向“可持续优化”的阶段。

---

## Phase 8：Day 53 - Day 60

# Productionization + Final Validation

完成：

```text
Security
Rate Limit
Resource Control
Observability
Docker
Failure Testing
Historical Validation
Architecture Defense
Final Release
```

最终：

```text
A-Quant Agent OS v1.0.0
```

---

# 五、每天固定工作模式

这是整个 60 天最重要的执行规则之一。

每天不是：

```text
看教程
→ 看教程
→ 看教程
→ 感觉懂了
```

而是：

```text
需求
  ↓
理解
  ↓
设计
  ↓
AI 协作开发
  ↓
自己 Review
  ↓
测试
  ↓
Agent 验收
  ↓
Git Commit
  ↓
复盘
```

---

## Step 1：先读当天任务

每天只做当天的事情。

例如：

```text
Day 21
Hybrid Retrieval
```

先明确：

```text
今天为什么需要 Hybrid Retrieval？
Vector Search 的问题是什么？
BM25 的优势是什么？
RRF 为什么可以融合？
Reranker 放在哪里？
如何验证效果提升？
```

---

## Step 2：先理解 🔴

所有标记为：

```text
🔴 必须理解
```

的内容，必须自己能够解释。

要求不是背概念。

而是能回答：

> 为什么？

例如：

```text
为什么 Strategy 和 Backtest Engine 要分开？
```

你应该能回答：

```text
Strategy
负责产生 Signal

Backtest Engine
负责模拟执行

如果耦合：
Strategy 复用困难
测试困难
不同策略会重复实现回测逻辑
Agent 也无法统一调用策略能力
```

---

## Step 3：让 Claude / Cursor 先设计

每天不要一上来：

```text
“帮我实现”
```

而是：

```text
阅读当前项目。

不要立即修改代码。

先输出：
1. 当前代码现状
2. 当前模块边界
3. 今天需求的设计方案
4. 会影响哪些模块
5. 修改哪些文件
6. 是否存在风险
7. 测试计划
```

这个步骤非常重要。

你需要训练的是：

> **先设计，再让 AI 写。**

---

## Step 4：AI 开发

可以让 Claude / Cursor 写：

```text
DTO
Entity
Repository
Adapter
Configuration
Controller
测试样板
Docker
Compose
SQL
```

但是 AI 写完以后，你必须检查：

```text
这个代码为什么放在这个模块？
依赖方向对吗？
Domain 被污染了吗？
有没有 Demo Shortcut？
异常怎么办？
失败怎么办？
能测试吗？
以后 Agent 怎么调用？
以后 Eval 怎么测？
```

---

## Step 5：Code Review

每天至少一次严格 Review。

Review 不应该问：

```text
“代码有没有问题？”
```

而应该问：

```text
你现在是 Staff Engineer。

请严格 Review 当前代码。

检查：
1. 模块边界
2. Domain 设计
3. SOLID
4. 可测试性
5. 错误处理
6. 超时
7. Retry
8. 幂等
9. 数据时间边界
10. Agent 安全
11. 可观测性
12. 是否存在 Demo 级 Shortcut

输出：
P0
P1
P2

如果存在 P0：
不允许进入下一天。
```

---

## Step 6：运行测试

至少：

```text
Compile
Unit Test
Integration Test
```

重要阶段增加：

```text
E2E Test
RAG Eval
Agent Eval
Regression Eval
```

---

## Step 7：Agent 验收

每天最后使用验收 Prompt。

验收不能接受：

```text
“代码已经写完”
```

必须有证据：

```text
代码
测试
日志
接口结果
Trace
Metrics
Eval Report
```

验收结果：

```text
A：优秀，可以继续
B：通过
C：基本通过，需要记录问题
D：不建议继续
F：失败
```

---

## Step 8：Git Commit

每天形成明确版本。

例如：

```text
feat: add hybrid financial retrieval
```

或者：

```text
feat: add agent state model
```

60 天以后你的 Git History 本身就是学习和工程过程证明。

---

# 六、三个重要级别

每天所有知识和工作分成三个级别。

---

# 🔴 第一类：必须理解

这是未来你面试、做架构、做设计必须能自己讲清楚的。

包括：

## Architecture

- Domain First
- DDD Boundary
- Clean Architecture
- Modular Monolith
- Dependency Direction

## Quant

- Look-ahead Bias
- Data Leakage
- Strategy vs Backtest
- Transaction Cost
- Slippage
- Out-of-Sample

## RAG

- Chunking
- Metadata
- Hybrid Retrieval
- Reranker
- Citation
- RAG Eval

## Agent

- Agent State
- Tool Calling
- ReAct
- Planner / Executor
- Failure Recovery
- Human-in-the-loop
- Multi-Agent Boundary

## MCP

- Host
- Client
- Server
- Tool
- Resource
- Capability Boundary

## Eval

- Dataset
- Rule-based Eval
- LLM Judge
- Regression Eval

你需要达到：

> **不看代码，能在白板上设计出来。**

---

# 🟡 第二类：应该理解

这些不一定需要从零手写，但必须知道：

```text
它是什么？
解决什么问题？
什么时候用？
有什么代价？
替代方案是什么？
```

包括：

- ClickHouse 表设计
- Redis Memory
- MQ Retry
- Circuit Breaker
- RRF
- Embedding
- Reranker
- Token Cost
- Rate Limit
- OpenTelemetry
- Docker Compose
- Health Check

---

# 🟢 第三类：可以让 Agent 完成

这些大量工作完全可以让 Claude / Cursor 帮你：

```text
DTO
Mapper
Repository
SQL 初稿
Dockerfile
Compose
OpenAPI
配置类
单元测试样板
Mock 数据
文档初稿
重复性 Adapter
基础 CRUD
```

但是有一个铁律：

> **AI 可以替你写代码，但不能替你拥有架构判断能力。**

---

# 七、整个项目的核心学习方法

你的学习方式不是：

```text
先学完 Agent
再做 Agent 项目
```

而是：

```text
学一个
    ↓
项目中使用一个
    ↓
遇到真实问题
    ↓
理解为什么需要这个技术
    ↓
再深入学习
```

例如：

```text
Day 25
为什么需要 Tool Contract？

不是因为教程说要学。

而是因为：
Agent 现在要调用 Market Data。

如果没有 Tool Contract：
- 参数不统一
- 输出不统一
- 权限无法控制
- Eval 无法统一
- Agent 难以切换 Tool

因此你才真正理解 Tool。
```

这就是：

```text
Project Driven Learning
+
Agent Assisted Development
+
Architecture Review
+
Evaluation Driven Improvement
```

---

# 八、最终项目的真实能力闭环

60 天结束后，完整闭环应该是：

```text
                    用户提出研究任务
                            │
                            ▼
                    Task Understanding
                            │
                            ▼
                     Supervisor Router
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
          ▼                 ▼                 ▼
     Market Agent      Research Agent      Quant Agent
          │                 │                 │
          ▼                 ▼                 ▼
      Market Tool       Financial RAG    Backtest Tool
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
                            ▼
                        Risk Agent
                            │
                            ▼
                       Report Agent
                            │
                            ▼
                 Structured Research Report
                            │
                            ▼
                    Citation + Trace + Eval
```

这条链路同时证明：

```text
Backend Engineering     ✓
LLM Integration         ✓
RAG                     ✓
Tool Calling            ✓
MCP                     ✓
Single Agent            ✓
Multi-Agent             ✓
Memory                  ✓
Evaluation              ✓
Quant Engineering       ✓
Data Engineering        ✓
Production Engineering  ✓
```

---

# 九、60 天之后，你真正应该得到什么

不是：

```text
我学过 LangChain
我学过 MCP
我学过 RAG
我做过 Agent Demo
```

而是：

> **我设计并实现了一个生产级 AI Quant Agent 平台。**

你应该能够完整讲出：

### 1. 为什么系统采用 Modular Monolith？

### 2. 为什么 Quant Domain 不依赖 LLM？

### 3. 为什么 RAG 要 Hybrid Retrieval？

### 4. 为什么 Tool 需要 Contract 和 Guardrail？

### 5. MCP 和 Tool Calling 的关系是什么？

### 6. Agent 为什么需要 State？

### 7. Planner 和 Executor 为什么分离？

### 8. 为什么 Multi-Agent 不一定比 Single Agent 更好？

### 9. Memory 为什么不能存所有内容？

### 10. Eval 为什么是生产 Agent 的核心？

### 11. 如何防止 Agent 改动导致能力 Regression？

### 12. Quant Backtest 如何避免 Look-ahead Bias？

### 13. AI 如何调用真实 Quant Engine 而不是自己编造数据？

### 14. 系统如何处理模型故障、Tool 故障和 MCP 故障？

### 15. 如何从最终报告反查 Agent 的所有执行步骤？

如果这些问题你都可以结合：

```text
代码
架构图
ADR
测试
Eval
Trace
真实历史数据验证
```

讲清楚，那么这个 60 天项目就不是学习玩具。

---

# 十、最终验收标准

## 工程

- [ ] 核心模块可以编译
- [ ] Domain 有单元测试
- [ ] 关键链路有集成测试
- [ ] Docker Compose 可启动
- [ ] Secret 不硬编码
- [ ] 核心调用有日志和 Trace

## 数据

- [ ] Provider 可替换
- [ ] 数据有 Quality Check
- [ ] 数据有来源和时间字段
- [ ] 防止未来数据进入回测

## Quant

- [ ] Strategy 和 Backtest 解耦
- [ ] 有交易成本
- [ ] 有 Slippage
- [ ] 有 Bias Guard
- [ ] 有 OOS Validation
- [ ] 有 Metrics

## RAG

- [ ] 有 Document Pipeline
- [ ] 有 Hybrid Retrieval
- [ ] 有 Citation
- [ ] 有 RAG Eval

## Agent

- [ ] 有 Tool Calling
- [ ] 有 Tool Guardrail
- [ ] 有 MCP
- [ ] 有 Agent State
- [ ] 有 Failure Recovery
- [ ] 有 Trace
- [ ] 有 Single Agent Eval
- [ ] 有 Multi-Agent Eval

## Memory

- [ ] 有 Short-term Memory
- [ ] 有 Long-term Memory
- [ ] 有 Memory Retrieval
- [ ] 有写入策略

## Evaluation

- [ ] 有 Eval Dataset
- [ ] 有 Rule-based Eval
- [ ] 有 LLM-as-Judge
- [ ] 有 Regression Eval

## 最终 Demo

用户输入：

> “研究某只 A 股在指定时间点是否值得进一步关注。”

系统完成：

```text
任务理解
    ↓
路由
    ↓
查询真实行情
    ↓
查询真实财务数据
    ↓
RAG 检索金融资料
    ↓
计算技术指标
    ↓
运行历史策略
    ↓
风险检查
    ↓
生成研究报告
    ↓
显示引用
    ↓
显示数据时间
    ↓
显示 Agent Trace
    ↓
运行 Eval
```

---

# 十一、最重要的最终原则

这 60 天，你不是在追求：

```text
每天写多少代码
用了多少 AI 框架
用了多少 Agent
```

真正追求的是：

```text
第一阶段：
我能让系统跑起来

第二阶段：
我知道为什么这样设计

第三阶段：
我能发现 AI 写错的地方

第四阶段：
我能设计一个 Agent 系统

第五阶段：
我能证明它是否有效

最终：
我能把一个真实 Domain
和 AI Agent Architecture
结合成生产级系统
```

---

# 最终一句话定位

60 天后，你的目标定位不是：

> “一个学过一点 AI 的 Java 开发。”

而应该是：

> **具备 Java 企业级后端基础，能够围绕真实业务 Domain 设计 RAG、Tool Calling、MCP、Agent Runtime、Multi-Agent、Memory 和 Evaluation，并能够将 AI 系统接入真实数据与量化计算链路的 AI 应用 / Agent 工程师。**

---

# 与逐日作战手册的关系

本文件是：

```text
战略总纲
```

之前的 60 个 Day 文件是：

```text
战术执行手册
```

正确使用方式：

```text
先看本文件
    ↓
理解最终项目全貌
    ↓
进入 Day 01
    ↓
每天执行
    ↓
完成一个 Commit
    ↓
通过每日验收
    ↓
进入下一天
    ↓
Day 60 Final Release
```

最终形成：

```text
战略总纲
+
60 天逐日任务
+
Claude / Cursor 开发 Prompt
+
Code Review Prompt
+
每日验收 Prompt
+
真实代码仓库
+
Architecture Docs
+
ADR
+
Eval Dataset
+
Backtest Report
+
Final Demo
```

# A-Quant Agent OS v1.0.0

**目标不是做一个 Demo。**

**目标是完成你的第一个真正可以代表“AI Application / Agent Architect 能力”的生产级项目。**




好的补充


最终你会获得什么能力？
我设计并实现了一套 A 股 AI 量化 Agent 平台。
Java Enterprise Architecture
+
LLM Gateway
+
RAG
+
Hybrid Retrieval
+
Reranker
+
Tool Calling
+
MCP
+
Single Agent
+
Multi-Agent
+
Agent State
+
Memory
+
Human-in-the-loop
+
Evaluation
+
Observability
+
Quant Research
+
Backtest
+
Data Engineering
+
Production Engineering





九、你必须真正理解的核心思想来源
Agent 状态机

来源：

LangGraph 的 State / Node / Edge。
LangGraph Graph API 官方文档
https://docs.langchain.com/oss/python/langgraph/graph-api?utm_source=chatgpt.com


Durable Agent
来源：
Checkpoint / Persistence / Resume。
LangGraph Persistence 官方文档
https://docs.langchain.com/oss/python/langgraph/persistence?utm_source=chatgpt.com


Human-in-the-loop
来源：
Interrupt → Persist → Review → Resume。
LangGraph Interrupts 官方文档
https://docs.langchain.com/oss/python/langgraph/interrupts?utm_source=chatgpt.com



Multi-Agent
来源：
Supervisor / Subagents / Handoff / Router / Skills。
LangGraph Multi-Agent 官方文档
https://langchain-ai.github.io/langgraph/tutorials/multi_agent/multi-agent-collaboration/?utm_source=chatgpt.com


Tool Calling
来源：
Schema → Model → Tool Call → Execute → Result → Model。
Spring AI Tool Calling 官方文档
https://docs.spring.io/spring-ai/reference/api/tools.html?utm_source=chatgpt.com


Java Agent Runtime
来源：
Spring AI Advisor / Recursive Tool Calling。
Spring AI 2.0 已经把工具执行循环做成 Advisor 链中的一等能力，这很适合你从 Java 架构角度研究 Agent Runtime，而不是只会调用一个 SDK。
Spring AI 官方网站
https://spring.io/ai/?utm_source=chatgpt.com


MCP
核心思想：
Tool Provider 和 Agent Host 解耦。
MCP 规范在 2026 年仍持续演进，近期规范重点已经包括无状态核心、可缓存列表结果、路由和授权强化，因此你的 MCP 学习不能只停留在“本地 stdio Server Demo”。
Model Context Protocol 规范更新说明
https://blog.modelcontextprotocol.io/posts/2026-07-28/?utm_source=chatgpt.com


Eval
来源：
Dataset → Experiment → Evaluator → Regression。
LangSmith Evaluation 官方文档
https://docs.langchain.com/langsmith/evaluation?utm_source=chatgpt.com