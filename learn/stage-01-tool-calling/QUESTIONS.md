# 阶段 1 · 验收题

闭卷写到 `MY_ANSWERS.md`。不要打开 ANSWERS.md。

## ★ 核心

1. 画出一次带工具的对话：哪些 HTTP 是打向 LongCat 的，哪些是你 JVM 里的方法调用？模型执行了 Java 吗？
2. `ToolCallingAdvisor` 解决了「谁来写 while(hasToolCalls)」这个问题。阶段 0 没有它时循环发生在哪？2.0 为什么要收到 Client 侧？
3. 用 5 句话讲 ReAct。生产上如果没有 maxToolCalls 会发生什么？
4. 为什么质量检查必须是确定性规则，而不是再问模型「你觉得这根 K 线靠谱吗」？

## 契约与安全

5. 工具入参 schema 从哪来？用户输入 `symbol=drop table` 时责任在模型还是在你的校验？
6. 读工具超时：重试是 Advisor/HTTP 层还是工具里？写工具（本阶段没有）为什么默认不重试？
7. MCP 是什么？本阶段为什么不实现它也能叫会了 Tool Calling？

## 本阶段代码

8. 日志里如何证明「第二次模型调用」发生了？如果只有一次 HTTP 到 LongCat，说明什么？
9. `quality=FAIL` 后 MarketBrief/研报为什么必须停？停在哪一层（Prompt 还是 Java if）？
10. 口试 3 分钟：把工具当成不可信 RPC 客户端，你会加哪些和普通微服务一样的治理？
