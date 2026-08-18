# 量化 Multi-Agent 总体协作架构

终极产品不是「一个什么都会的超级 Agent」，而是：

**一个人 + 一支 24 小时工作的 AI 量化投研团队。**

边界（写在门上）：

> 它把研究、分析、回测、决策流程工程化、自动化、可迭代化。  
> **不等于天然能赚钱。** Alpha 来自你的假设、数据和风控，不来自 Agent 数量。

本仓库的 Agent 学习以这套系统为 **唯一主战场 / 毕业项目**。协作细节如下；阶段如何验收见 [LEARNING.md](./LEARNING.md)。

---

## 1. 组织，不是聊天群

```
                    ┌──────────────────────────┐
                    │  Quant Supervisor         │
                    │  总控 / Planner（不自己研报）│
                    └────────────┬─────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        ↓                        ↓                        ↓
  Data Agents              Research Agents           Risk Agents
  数据工程                  投研分析                  独立风控（否决权）
        │                        │                        │
        └──────────────┬─────────┴──────────┬─────────────┘
                       ↓                    ↓
                Strategy Agents      Backtest / Optimizer
                       │                    │
                       └─────────┬──────────┘
                                 ↓
                          Portfolio Agent
                                 ↓
                          Risk Gate（硬闸）
                                 ↓
                          Execution Agent
                                 ↓
                          Monitoring Agents
                                 │
                                 └── 异常 / 新数据 ──► Supervisor
```

Supervisor 只做四件事：**收任务、拆任务、选 Agent、汇总/决定是否继续调查**。  
它不查行情、不写策略、不调券商。

---

## 2. 所有 Agent 共享的四条总线

没有总线，所谓 Multi-Agent 只是一堆 Prompt 互相转发。

```
 Agent A / B / C
        │
        ├─ Quant Knowledge (RAG)   研报、财报、策略文档、复盘
        ├─ Market Data             行情/基本面事实源（经质量门）
        ├─ Memory                  会话、假设日志、实验记录
        └─ Tool / API Layer        唯一允许碰外部世界的地方
                    │
         ClickHouse / Redis / 对象存储 / Python Quant Engine
```

原则：

- **事实走工具与数据库**，不走模型幻觉。
- **知识走 RAG**，带来源。
- **过程走 Memory**，假设与实验可恢复。
- **副作用走 Tool Gateway**（权限、超时、审计、幂等）。

---

## 3. 两条必须分开的环

### 3.1 投研环（可自动多轮，允许失败）

```
发现机会 → 形成假设 → 写策略 → 回测 → 发现问题
    → 优化 → 再回测 → 风险检查 → 候选方案（给人看）
```

适合 Agent 循环。必须有：**最大实验次数、最大费用、最大墙钟时间**。

### 3.2 交易环（默认不自动，硬闸）

```
候选方案 → 人确认 → Portfolio → Risk Gate → Execution → Monitoring
```

**禁止**：`LLM → 直接下单`。  
Execution 只接受通过 Risk Gate 的、带幂等键的指令。

---

## 4. 各小队职责与协作关系

| 小队 | 成员（按需出现，不要第一天全造） | 产出契约 | 谁可以否决它 |
|------|----------------------------------|----------|--------------|
| Data | Market Data、Data Quality、ETL | 带质量标记的可信数据 | Quality 不通过则 Research 不得使用 |
| Research | Fundamental、Technical、Regime、Industry、News、Sentiment | 结构化研报（JSON） | Supervisor 可要求补证据 |
| Factor | Factor Research | IC/IR/衰减/相关 | 不稳定因子不得进模型 |
| Strategy | Momentum / MR / Trend / Factor / StatArb / ML | 策略规格（可回测） | Risk 可拒绝上组合 |
| Backtest | Backtest Runner、Backtest Analyst | 指标 + 归因解释 | 数据质量/前视偏差检查失败则作废 |
| Optimizer | Strategy Optimizer | 新参数/新过滤（仍要再回测） | 过拟合检测失败则丢弃 |
| Risk | Risk Manager | 通过 / 拒绝 + 原因 | **独立于 Strategy**，可一票否决 |
| Portfolio | 仓位/配置 | 目标持仓 | Risk Gate |
| Execution | 下单/撤单/拆单/滑点 | 订单状态 | **仅 Risk Gate 之后** |
| Monitor | PnL、持仓、策略失效、数据、系统 | 告警事件 | 可触发 Supervisor 重开投研环 |

协作的「重量」来自 **否决权与契约**，不来自 Agent 个数。

典型一次提问：

```
你：「今天 A 股新能源有没有值得短线继续研究的标的？」

Supervisor
  ├── Market Data Agent      行情/资金流（工具）
  ├── Data Quality Agent     缺数/冲突则打回
  ├── Industry Agent         景气与轮动
  ├── Fundamental Agent      估值与财报要点（RAG+工具）
  ├── Technical Agent        结构/量价
  ├── News / Sentiment       事件与情绪（不可信内容隔离）
  ├── Strategy Agent         仅产出「候选假设」，不是下单
  └── Risk Agent             集中度/流动性/制度风险
        ↓
汇总研报（引用数据与文档）→ 人决定是否进回测
```

---

## 5. 这套架构是不是生产级？能达到什么效果？

**是生产级组织方式的雏形**，不是把运维细节补完后的最终生产系统。

已经具备生产系统该有的骨架：

```
数据 → 研究 → 策略 → 回测 → 风控 → 组合 → 执行 → 监控
  ↑                                              ↓
  └────────────── 反馈 / 再研究 ─────────────────┘
```

真正放量还要补齐（与 [LEARNING.md](./LEARNING.md) 后半段一一对应）：权限与审计、任务状态机、MQ、故障恢复、可观测、模型降级、HITL、交易安全隔离、数据质量体系、评测回归。

**效果（直白）：**

| 以前 | 有这套系统之后 |
|------|----------------|
| 你手工找数、看新闻、写策略、跑回测、改参、再跑 | 你下研究目标，团队跑完给你带证据的候选方案 |
| 机械劳动占满时间 | 你把时间留在假设、约束和签字 |

更进一步：投研环可以自己「发现问题 → 改策略 → 再验证」，但仍停在 **候选方案**，除非你显式打开交易环并过 Risk Gate。

---

## 6. 阶段怎么「产出项目」才不算 Demo

不要 10 个独立玩具仓库。

要 **一个 Quant Agent Platform**，每个学习阶段发布一个 **可运行版本**（v0.1、v0.2…），并且必须同时满足：

1. 这个版本里 **新出现的 Agent 能独立完成自己的契约**（JSON/指标/否决，而不是一段散文）。
2. 带上该阶段对应的 **生产门槛**（超时、审计、外置状态、评测……见学习路线）。
3. 旧 Agent 还在，且接口兼容——你在打磨同一家「公司」，不是每学期换一家。

这样你才能判断：学到的是 Demo，还是能并进生产架构的一块骨头。
