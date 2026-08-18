# 阶段 8 · 平台化与监控闭环（平台 v1.0）

**产物**：`MonitoringAgents` + `ExecutionAgent` **仅纸交易**，且必须经 Risk Gate + HITL。  
投研环与交易环隔离。监控异常回 Supervisor。

## ★ 必懂

1. **毕业标准是闭环不是 Agent 数量。** 失效检测 → 事件 → 再研究。没有监控的执行是裸奔。
2. **配置与代码解耦：** 模型别名、Prompt 版本、功能开关。灰度 5% → 全量 → 可回滚。
3. **容量指标：** SSE 连接、队列深度、回测机时。HPA 跟这些，不跟 CPU 玄学。
4. **纸交易仍当真实账户：** 幂等、状态机、审计。只是 broker 是 simulator。
5. **密钥与网络：** 即使模拟，Execution 模块也要边界清晰，为以后拆进程做准备。

## 必读

LEARNING.md 阶段 8；QUANT_ARCHITECTURE 监控回边。

## AI 提示词

约束：`Execution 只能 PaperBroker。没有 Risk PASS + 人工批准禁止调用 broker。`

1. `PaperBroker：内存订单簿。ExecutionAgent 只接收 GateTicket。`  
2. `Monitor：若模拟持仓回撤超阈值，发 ResearchReopenEvent。`  
3. `把模型名改成别名 chat.primary，配置可切换。`  
4. `写一份 20 行的发布清单：评测→灰度→回滚。`  
5. `考我：监控发现失效后为什么不要自动实盘改仓？`

## 完成标准

纸交易链路可演示；监控能触发再研究；QUESTIONS；能独立把架构讲给别人听 15 分钟。
