package com.edy.firstai.quant.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 行情数据 Agent —— 阶段 1 只读工具调用验收。
 *
 * <h2>Agent 工具循环全景（三步）</h2>
 * <pre>
 *   第 1 步 ─ 用户提问
 *            openAiChatClient
 *              .prompt().tools(marketDataProvider)   ← 把 @Tool 对象注册进本次 prompt
 *              .user("帮我查 600519 最近的行情")      ← 用户问题
 *            → 发给 LongCat
 *
 *   第 2 步 ─ 模型决定调工具（tool call 请求）
 *            LongCat 返回："我需要调 getDailyBar(symbol=600519, tradeDate=...)"
 *            → ToolCallingAdvisor 拦截到这个 tool call
 *            → 反射调用 MarketDataProvider.getDailyBar(...)   ← 【日志：工具进入 / 工具返回】
 *            → 把 DailyBar 拼回 instructions 作为 tool response
 *
 *   第 3 步 ─ 第二次模型调用（带工具结果）
 *            ChatClient 把「用户问题 + 工具调用 + 工具返回」一起再发给 LongCat
 *            → LongCat 这次拿到真实数据，生成最终自然语言回答
 *            → 循环结束（模型不再请求工具）
 * </pre>
 *
 * <h2>maxToolCalls 守卫</h2>
 * 若工具失败、模型反复重试同一工具，{@code while(isToolCall)} 会死循环。
 * {@link com.edy.firstai.quant.tool.MaxToolCallsEligibilityChecker} 在 advisor 上兜底：
 * 超限后强制返回 false 切断循环。
 *
 * <h2>阶段 1 约束</h2>
 * <ul>
 *   <li>✅ 只读工具：注册的是 {@link MarketDataProvider}，只有 getDailyBar</li>
 *   <li>🚫 禁 Execution / RAG / Memory-Redis：本类未注册任何写工具、未接 VectorStore</li>
 * </ul>
 */
@Component
public class MarketDataAgent {

    private static final Logger log = LoggerFactory.getLogger(MarketDataAgent.class);

    private final ChatClient openAiChatClient;
    private final MarketDataProvider marketDataProvider;

    /** 最大工具调用次数，超限强制终止——防死循环 */
    @Value("${app.ai.quant.max-tool-calls:5}")
    private int maxToolCalls;

    public MarketDataAgent(ChatClient openAiChatClient, MarketDataProvider marketDataProvider) {
        this.openAiChatClient = openAiChatClient;
        this.marketDataProvider = marketDataProvider;
    }

    /**
     * 查询指定股票的行情摘要。
     *
     * @param symbol 股票代码，如 "600519"
     * @return 模型基于工具返回数据生成的自然语言回答
     */
    public String queryMarket(String symbol) {
        log.info("[Agent 入口] queryMarket: symbol={}, maxToolCalls={}", symbol, maxToolCalls);

        // 阶段 1 验收要点：.tools(marketDataProvider) 把 @Tool 方法注册为可用工具；
        // ToolCallingAdvisor 会在循环里自动识别 tool call → 调用 getDailyBar → 再喂给模型。
        ChatResponse response = openAiChatClient
                .prompt()
                .tools(marketDataProvider) // ← 注册只读行情工具
                .user(String.format("请帮我查询股票 %s 最近的行情，并给出简要分析。", symbol))
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
        log.info("[Agent 出口] 模型最终回答长度={}", content != null ? content.length() : 0);
        return content;
    }
}
