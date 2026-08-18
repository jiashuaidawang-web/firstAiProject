# 阶段 6 · 标准答案

1. 建议：researchId、taskId、model、provider http status、prompt/completion tokens、toolName+latency、ttft、error class。缺 correlating id、缺 tool、缺 token，基本查不下去。

2. 隐私、合规、密钥、策略细节外泄；日志系统权限通常弱于生产数据权限。

3. 适合：解释是否可读、摘要是否跑题。不适合：Sharpe、回撤、IC——必须代码算。Judge 有位置偏差、自我偏好，要校准。

4. 协作式取消：不再发下一轮 LLM；写终止原因；任务 FAILED 或 BUDGET_EXCEEDED；已产生的部分结果保留可审计。

5. 无法区分代码 bug vs 文案变化。Prompt 和模型别名必须进 job 快照。

6. 按模型单价×token + 回测机时，按 researchId/功能聚合。没有账单字段等于没有架构。
