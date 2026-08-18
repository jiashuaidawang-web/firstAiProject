# 阶段 8 · 标准答案

1. Monitor 写事件 → 总线 → Supervisor 开新 research task → 投研环 → 候选方案 **停**。改仓必须新的 Risk+HITL。禁止 Monitor 直接调 Execution。

2. Prompt 在配置中心/内容表，按 tenant 百分比读版本。回滚切回旧 version id，不必回滚 jar。模型别名同理。

3. 缺：未报/部成/撤单、幂等、对账、部分成交。模拟期就把状态机做对。

4. LLM 等待是 IO 阻塞/虚拟线程，CPU 可能很低但连接和队列已经满。应按 `sse.connections`、`queue.depth`、`inflight.llm`。

5. 提纲自检：协议兼容 → 契约 JSON → 工具三步循环 → 外置记忆 → RAG 权限 → Supervisor 组织 → 回测作业 → 观测成本 → 独立风控 → 纸交易闸门。能按这个顺序讲完，阶段 0–8 才算学成。
