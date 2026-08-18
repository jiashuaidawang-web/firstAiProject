# 阶段 1 · 标准答案

## ★ 核心

1. 至少两轮（常常更多）HTTP 打向 LongCat：第一轮模型返回 `tool_calls`；你的 JVM 执行 `@Tool` 方法（这不是 HTTP 到 LongCat）；第二轮把 tool 结果作为消息发给 LongCat，模型生成最终自然语言/JSON。**模型从不执行 Java。** 所谓 Agent，是「模型选动作 + 你的运行时执行动作」的闭环。

2. 1.x / 各模型实现里，tool loop 经常散落在 ChatModel 内部。2.0 把它做成 Client Advisor，和结构化重试、评估循环同一套可组合链，并能接入 Observation。没有 Advisor 时你要么自己 while，要么用低层 ChatModel 手动 `hasToolCalls`。

3. 模型思考（该不该用工具）→ 调用工具 → 观察结果 → 再思考。没有上限：模型可反复调同一工具、或工具一直失败仍重试，token 与线程都被掏空。所以 maxToolCalls、墙钟、重复调用检测是产品特性不是优化。

4. 数据质量是 **可单测的不变量**。再问 LLM 会引入新幻觉，且无法审计「凭什么 FAIL」。LLM 可以解释 FAIL 原因，但判定必须是规则。

## 契约与安全

5. Spring AI 从方法签名 / record 生成 JSON Schema 发给模型。模型仍可能乱填。**校验是你的责任**，在 Java 入口拦。schema 是给模型看的说明书，不是防火墙。

6. 工具内短超时（2s）+ 只读可有限重试；ChatModel HTTP 另有 `spring.ai.retry`。两层不要叠成风暴。写工具有副作用，重试会产生双花，必须幂等键，本阶段不做写。

7. MCP 是让工具跨运行时（IDE、别的 Agent）暴露的协议。本阶段会了「模型如何调你的 Java」就够。USB 标准不等于你第一天要开工厂。

## 本阶段代码

8. 看 Observation / HTTP 日志：两次（或以上）chat completions；中间夹着你的 `getDailyBar` 日志。只有一次 completions 且答案里却有行情，多半是模型瞎编、工具没挂上。

9. **Java if**：`quality!=PASS` 则不调用后续生成或返回 422。只靠 Prompt 说「请不要编」不够。门在业务层。

10. 超时、限流、鉴权（symbol 白名单）、审计、指标、舱壁、错误码。和调支付 RPC 同一套，只是调用方从你的代码变成了模型。
