# 阶段 5 · 标准答案

1. API 只负责入队。阻塞点只能在 Worker。Controller 同步 wait Python = 阶段失败。

2. 指数叠加：一次用户请求触发 N×M 次 Provider 调用，429 雪崩、账单爆炸。要分层：HTTP 暂态短重试；业务按错误类型；有总 deadline。

3. 四次引擎跑、写四份结果、费用×4。需要 idempotency key 或「同一 strategy 版本只允许一个 RUNNING」。

4. 投研可排队（研究员能等）；交易链路应失败或降级到规则。不要 silently 换模型导致回测不可复现。模型别名要进 job 快照。

5. 至少：LLM 调用、检索、回测进程。一种慢不能占满整个线程池/连接池。

6. cancel 标志 → Worker 中断 → destroy 子进程 → 状态 CANCELED。只改 DB 状态而进程还在跑，不算取消。
