# 阶段 2 · 记忆与会话（平台 v0.3）

**产物**：`ResearchMemoryAgent` —— `researchId` 下假设/结论可恢复，重启不丢。

## ★ 必懂

1. **多轮不是 ChatClient 自动记住。** 默认每次请求无状态。记忆 = 你把历史 Message 再塞进 Prompt，或用 Memory Advisor + 外置 Store。
2. **JVM 内存当会话 = 不能扩容。** 必须 Redis/DB。同一 `researchId` 并发写入要串行（锁或单队列）。
3. **窗口有限。** 无限拼接 K 线会爆上下文和钱。策略：最近 N 轮 + 摘要 + 结构化「假设表」（不要把摘要当事实源而不标注）。
4. **流式未完成的 assistant 消息** 断线后如何落库：要么不落半句，要么标 `partial`。
5. **长期记忆写入要审核。** 用户/网页内容进长期记忆 = 投毒面。

## 必读

Spring AI Chat Memory 一节；LangChain Checkpointer 只对照概念「状态在图外」。

## AI 提示词（逐步）

约束：`阶段 2。禁止上 RAG 向量库、禁止 Execution。记忆必须外置。`

1. `为 researchId 设计 Redis 结构：messages 列表 + hypothesis JSON。Advisor 或显式把历史拼进 prompt。重启应用后同一 id 仍能续聊。提供 GET 查看记忆。同一 id 用锁串行。`
2. `当 token 估算超过阈值，触发一次摘要写入 summary 字段，后续只带 summary+最近 6 轮。摘要必须标注 generated_at。禁止把原始日线全塞进历史。`
3. `考我：扩容两个实例时如何避免交叉写乱序？摘要和事实如何区分？`

## 完成标准

重启后续聊；超长会话有摘要；QUESTIONS 闭卷。
