# 阶段 5 · 可靠性与长任务回测（平台 v0.6）

**产物**：`StrategyAgent`（结构化策略规格）+ `BacktestAgent`（Worker 调 Python 引擎）。

## ★ 必懂

1. **两层超时、两层重试** 不要拧成一股。Provider HTTP ≠ 用户 API deadline ≠ Worker 墙钟。写操作不盲目重试。
2. **回测是批量作业** 不是聊天。HTTP 202、队列、可取消、死信。占着 Tomcat/SSE 线程跑 10 分钟回测 = 架构错误。
3. **Python 引擎是不可信外部进程。** 只经 Tool Gateway：参数白名单、超时杀掉、结果 schema 校验。
4. **舱壁：** 模型调用慢不能拖死回测队列，反之亦然。
5. **降级：** 主模型 429 → 备用 → 明确失败。对用户的语义要定义（排队 / 失败），不要随机。

## 必读

本仓库 RetryConfig；AWS backoff+jitter 短文；Spring `core.retry`。

## AI 提示词

约束：`回测必须异步。禁止在 Controller 里 ProcessBuilder 同步等到结束。禁止真下单。`

1. `Strategy record：name, entry, exit, universe。Agent 只产出该 JSON。`
2. `quant-engine/ 放一个 fake python：读 stdin JSON，睡 2 秒，stdout 指标 JSON（sharpe, mdd）。Java Worker 消费队列调用它。`
3. `支持 cancel。超时杀进程。失败进死信。指标：queue.depth, job.duration。`
4. `考我：为什么回测重试要幂等？同一 strategyId 跑两次应如何去重？`

## 完成标准

同步接口秒回 taskId；可查询/取消；QUESTIONS。
