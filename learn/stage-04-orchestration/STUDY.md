# 阶段 4 · 编排 / Supervisor（平台 v0.5）

**产物**：`QuantSupervisor` 雏形 + Technical / Industry / Sentiment（可先 stub 成三个 ChatClient + 不同 system）。  
Supervisor **只拆任务、选人、汇总**，自己不写完整研报。长任务 `taskId` + 异步。禁止 Execution。

## ★ 必懂

1. **不是所有流程都该 Agent。** 强一致、规则稳定 → 工作流/状态机；探索性调研 → LLM 循环。量化里「风控规则」不该让模型即兴发挥。
2. **Supervisor 是路由器不是天才。** 子 Agent 有契约（JSON）。汇总是拼装+冲突检测，不是再幻觉一篇长文。
3. **图 + 可恢复状态。** 节点失败从 checkpoint 续，不从头烧 token。对照 LangGraph checkpointer，Java 里就是状态机 + 存储。
4. **HITL：** 高风险边 interrupt，状态停在 `WAIT_HUMAN`。绕过必须失败。
5. **API 不跑长循环。** 202 + Worker；查询当前节点。

## 必读

LangGraph 的 interrupt/checkpointer 概念页（对照用）；本仓库 QUANT_ARCHITECTURE 组织图。

## AI 提示词

约束：`Supervisor 不得调用行情工具自己研报。禁止 Execution。长任务必须 taskId。`

1. `实现 Supervisor：输入研究目标，按规则或一次结构化 plan 选出 2~3 个子 Agent 并行（线程/虚拟线程），汇总成 ResearchBundle JSON。子 Agent 先可用固定 stub。`
2. `把该流程改成任务表：PENDING/RUNNING/DONE/FAILED。POST 创建返回 taskId，GET 查状态。`
3. `加一个假 HITL 节点：当情绪分数极端时 WAIT_HUMAN。不点批准不得进入「形成策略候选」。`
4. `考我：为什么 Supervisor 自己写研报是架构腐败？`

## 完成标准

能画出组织图；任务可查询；HITL 能卡住；QUESTIONS。
