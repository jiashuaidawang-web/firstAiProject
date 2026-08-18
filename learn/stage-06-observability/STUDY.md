# 阶段 6 · 可观测 / 评测 / 成本（平台 v0.7）

**产物**：`BacktestAnalystAgent`（指标断言 + 有限解释）+ `FactorResearchAgent` 最小回归集 + 按 `researchId` 成本账。

## ★ 必懂

1. **不能定位就不能上线。** Trace 维度见 LEARNING.md 阶段 6 列表。5 分钟内分清：模型 / 工具 / 检索 / 网关。
2. **Prompt 默认不是 debug 日志。** PII、完整行情、密钥脱敏。采样。
3. **评测分两类：** 确定性断言（Sharpe 计算、IC 符号）和开放生成（LLM-as-Judge 要校准，Zheng 2023）。关键路径靠断言。
4. **成本是一等公民。** token 预算打断 loop；账按租户/研究任务。
5. **Prompt/模型版本化。** 改了词就能回滚，才能谈回归。

## 必读

OTel GenAI semconv；Zheng et al. LLM-as-Judge 摘要（知道边界即可）。

## AI 提示词

1. `为 chat/tool/backtest 打 Observation：researchId, model, promptTokens, toolName。`  
2. `Analyst：Java 校验指标区间；LLM 只解释，不允许改数字。`  
3. `eval/ 放 5 条黄金：给定 fixture 回测输出，断言 Analyst 标签。`  
4. `超 token 预算中止并写 BILLING_EXCEEDED。`  
5. `考我：为什么 LLM 解释不能覆盖 Sharpe 数值？`

## 完成标准

能用一次 trace 讲清故障；有评测命令；QUESTIONS。
