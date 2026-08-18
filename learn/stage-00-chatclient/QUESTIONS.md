# 阶段 0 · 验收题（先自己写在 MY_ANSWERS.md）

规则：闭卷。能讲给一个没碰过 Spring AI 的 Java 同事听，才算会。  
**不要打开 ANSWERS.md。**

## A. 心智模型（★ 核心）

1. 用 Spring 里的类比，说明 `ChatModel` 和 `ChatClient` 各是什么。为什么 2.0 文档把 Client 当用户主 API？
2. 用户打了一句「分析新能源」，从 Controller 到 LongCat HTTP，中间有哪些对象（Prompt、Message、options）？
3. 为什么 `base-url` 填 `https://api.longcat.chat/openai` 而不是 `.../openai/v1/chat/completions`？填错会怎样？

## B. 流式与结构化（★ 核心）

4. `.stream().content()` 的返回类型是什么？若写成 `Flux<ChatResponse>` 错在哪？
5. 为什么结构化 `.entity()` 不能直接用在 SSE 流上？生产上要完整 JSON 该怎么做？
6. `useProviderStructuredOutput()` 和「把 JSON 格式写进 Prompt」有何本质差别？

## C. 本仓库代码

7. 多模型时直接 `ChatClient.create(chatModel)` 会丢掉什么？本仓库 `MultiModelConfig` 如何避免？
8. `call().content()` 和 `call().chatResponse()` 你分别在什么时候用？token 用量从哪读？
9. Spring Boot 4 里业务重试为什么不用旧 `spring-retry` 依赖？本仓库 RetryTemplate 在哪个包？

## D. 量化验收

10. MarketBriefAgent 本阶段为什么 **禁止** 去拉行情？如果模型在 JSON 里编了一个收盘价，产品上你怎么处理？

## E. 教别人（口试）

11. 对着空气讲 3 分钟：从浏览器 SSE 到模型吐出第一个 token，发生了什么。（录音或写逐字稿）
