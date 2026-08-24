package com.edy.firstai.quant.data;

import com.edy.firstai.quant.tool.ToolQualityCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 行情数据 Agent —— 阶段 1 真实 HTTP 工具调用的「大脑」。
 *
 * <h2>这个类是干什么的</h2>
 * 它是整个行情查询的<b>协调者</b>（Coordinator）：
 * <ol>
 *   <li>接收用户要查哪只股票（symbol）</li>
 *   <li>把「所有可用的行情工具」一次性注册给 ChatClient，让 LLM 自己决定调哪些</li>
 *   <li>LLM 选工具 → 工具执行 → 结果喂回 LLM → LLM 生成最终回答</li>
 *   <li>根据工具执行的<b>真实结果</b>计算 quality，决定最终 JSON 里能不能给 close 等数据</li>
 * </ol>
 *
 * <h2>为什么需要注入 7 个 Provider（而不是 1 个）</h2>
 * <b>核心原因：工具越多，description 边界越要清晰，模型才越不容易选错。</b>
 *
 * <p>如果把所有 ~15 个 @Tool 方法塞进一个类，它们的 description 挤在一起，
 * 模型容易把「查情绪」误配到「查资金」的工具上。按领域拆成 7 个 Provider：
 * <ul>
 *   <li>每个 Provider 只负责一个领域的 2-3 个工具，description 互不干扰</li>
 *   <li>模型先「感知到」这个 Provider 的工具集，再在小区间内精确选择</li>
 *   <li>未来新增工具（比如加一个「北向资金」）只加对应 Provider，不影响其它</li>
 * </ul>
 *
 * <h2>7 个 Provider 各自管什么</h2>
 * <table border="1">
 *   <tr><th>Provider</th><th>领域</th><th>包含的 @Tool</th><th>模型什么时候会选它</th></tr>
 *   <tr><td>TrendDataProvider</td><td>技术趋势</td><td>scanTrend、getLeadingStocks</td>
 *       <td>用户问「某股票技术面怎么样」「趋势是否成立」</td></tr>
 *   <tr><td>SentimentProvider</td><td>情绪/大盘</td><td>getSentiment、getOverview、getLimitPool</td>
 *       <td>用户问「今天市场情绪」「涨停有多少」「大盘值不值得参与」</td></tr>
 *   <tr><td>LeaderProvider</td><td>龙头/主线</td><td>getLeaders、getTradeIdea、getMainline</td>
 *       <td>用户问「龙头是谁」「该买还是该卖」「主线板块」</td></tr>
 *   <tr><td>MainForceProvider</td><td>主力资金</td><td>getMainForceStocks、getMainForceSeats</td>
 *       <td>用户问「主力在买什么」「龙虎榜」「席位抱团」</td></tr>
 *   <tr><td>FundFlowProvider</td><td>资金流向</td><td>getFundFlowBoard、getDragonTiger、getDragonTigerDetail</td>
 *       <td>用户问「板块资金流入」「龙虎榜明细」</td></tr>
 *   <tr><td>ThemeProvider</td><td>题材</td><td>getThemeFactor</td>
 *       <td>用户问「哪个题材有炒作价值」</td></tr>
 *   <tr><td>RealtimeProvider</td><td>实时盘口</td><td>getRealtimeStatus、getStage</td>
 *       <td>用户问「现在多少钱」「当前情绪阶段」</td></tr>
 * </table>
 *
 * <h2>整体执行流程（一次 queryMarket 调用发生了什么）</h2>
 * <pre>
 * 1. quality.clear()                     ← 重置质量采集器（防止上次请求的残留）
 *
 * 2. openAiChatClient.prompt()
 *      .tools(trend, sentiment, ...)     ← 把 7 个 Provider（~15 个工具）全部注册
 *      .user("查询股票 600519 的行情")    ← 用户问题
 *      .call()                           ← 触发 LLM 工具循环：
 *          ① 第 1 次调 LLM：模型读所有工具 description，决定「先调 scanTrend」
 *          ② advisor 执行 scanTrend → MarketDataClient 发 HTTP → 结果返回
 *             ↑ 同时 ToolQualityCollector 自动记录这次调用的 quality（REAL/FAIL/EMPTY）
 *          ③ 第 2 次调 LLM：模型看到 scanTrend 的结果，决定「再调 getSentiment」
 *          ④ advisor 执行 getSentiment → 记录 quality
 *          ⑤ 第 3 次调 LLM：模型觉得数据够了 → 返回纯文本（不再调工具）
 *             ↑ while(isToolCall)=false → 循环结束
 *
 * 3. .content()                         ← 取模型最终生成的自然语言回答
 *
 * 4. quality.overall()                  ← 根据所有工具调用的 quality 算最终质量：
 *                                          全部 REAL → "REAL"
 *                                          任一 FAIL → "FAIL"
 *                                          无 FAIL 有 EMPTY → "PARTIAL"
 *
 * 5. 组装响应：
 *      REAL → 正常返回 answer
 *      FAIL → answer + close=null + dataAvailable=false + notice（绝不编造数据）
 *
 * 6. finally { quality.clear() }        ← 清理 ThreadLocal，防线程池复用泄漏
 * </pre>
 *
 * <h2>为什么 quality 要在 finally 里 clear()</h2>
 * {@link ToolQualityCollector} 内部用 ThreadLocal 存质量标记。
 * Tomcat 线程池会复用线程——如果不清理，下次请求可能读到上次的残留值，
 * 导致 quality 判断错误。
 *
 * <h2>为什么用 .content() 而不是 .entity()</h2>
 * <ul>
 *   <li>.entity(XX.class) 强迫模型按指定 JSON 格式输出 → 模型要自己填 quality
 *       → 但模型看不到工具执行的真实 quality 元数据 → 可能编造 quality="REAL" 实际却 FAIL</li>
 *   <li>.content() 让模型生成自然语言回答 → quality 由 ToolQualityCollector 独立计算
 *       → 彻底杜绝模型在 quality 上造假</li>
 * </ul>
 *
 * <h2>阶段 1 约束（本类如何体现）</h2>
 * <ul>
 *   <li>✅ 只读：注册的 7 个 Provider 全是 GET 行情/分析接口，无任何写操作</li>
 *   <li>🚫 禁 Execution：未注册任何下单/改状态工具</li>
 *   <li>🚫 禁 RAG：未接 VectorStore</li>
 *   <li>🚫 禁 Memory-Redis：无 ChatMemory，每次请求独立，不记历史</li>
 * </ul>
 */
@Component
public class MarketDataAgent {

    private static final Logger log = LoggerFactory.getLogger(MarketDataAgent.class);

    /**
     * 主模型 ChatClient（@Primary，指向 LongCat）。
     * 阶段 1 已装配 ToolCallingAdvisor（5 参数 builder），具备工具调用能力。
     */
    private final ChatClient openAiChatClient;

    /**
     * 工具质量采集器。
     * 每个工具执行后自动记录 quality，Agent 据此算最终 quality。
     * ThreadLocal 实现，线程隔离。
     */
    private final ToolQualityCollector quality;

    // ------------------------------------------------------------------ 7 个领域 Provider

    /** 技术趋势：scanTrend（个股技术面扫描）、getLeadingStocks（领涨股监控） */
    private final TrendDataProvider trend;

    /** 情绪/大盘：getSentiment（情绪温度）、getOverview（大势择时）、getLimitPool（涨停池） */
    private final SentimentProvider sentiment;

    /** 龙头/主线：getLeaders（龙头个股）、getTradeIdea（买卖建议）、getMainline（主线板块） */
    private final LeaderProvider leader;

    /** 主力资金：getMainForceStocks（龙虎榜个股合力）、getMainForceSeats（抱团席位） */
    private final MainForceProvider mainForce;

    /** 资金流向：getFundFlowBoard（板块资金）、getDragonTiger（龙虎榜）、getDragonTigerDetail（席位明细） */
    private final FundFlowProvider fundFlow;

    /** 题材：getThemeFactor（题材炒作因子） */
    private final ThemeProvider theme;

    /** 实时盘口：getRealtimeStatus（实时快照）、getStage（当前情绪阶段） */
    private final RealtimeProvider realtime;

    /**
     * 工具循环上限（从配置读，默认 5）。
     * 防工具失败时模型反复重试同一工具导致死循环。
     */
    @Value("${app.ai.quant.max-tool-calls:5}")
    private int maxToolCalls;

    public MarketDataAgent(ChatClient openAiChatClient, ToolQualityCollector quality,
                           TrendDataProvider trend, SentimentProvider sentiment,
                           LeaderProvider leader, MainForceProvider mainForce,
                           FundFlowProvider fundFlow, ThemeProvider theme,
                           RealtimeProvider realtime) {
        this.openAiChatClient = openAiChatClient;
        this.quality = quality;
        this.trend = trend;
        this.sentiment = sentiment;
        this.leader = leader;
        this.mainForce = mainForce;
        this.fundFlow = fundFlow;
        this.theme = theme;
        this.realtime = realtime;
    }

    /**
     * 查询指定股票的行情摘要。
     *
     * @param symbol 6 位数字股票代码，如 "600519"
     * @return 带 quality 字段的 JSON：
     *         <ul>
     *           <li>quality=REAL：工具全部成功，answer 含真实数据</li>
     *           <li>quality=FAIL：有工具失败（超时/异常），close=null，notice 提示数据不可用</li>
     *           <li>quality=PARTIAL：部分工具无数据（204），answer 含可用部分</li>
     *         </ul>
     *         <b>进程不空转保障</b>：工具循环受 maxToolCalls 上限约束（{@link MaxToolCallsEligibilityChecker}），
     *         HTTP 受 responseTimeout 约束；即使所有工具都失败，也会在有限次调用后落到降级文案。
     */
    public Map<String, Object> queryMarket(String symbol) {
        log.info("[Agent 入口] queryMarket: symbol={}, maxToolCalls={}", symbol, maxToolCalls);

        // 必须先 clear：Tomcat 线程池复用线程，不清理会读到上次请求的残留 quality
        quality.clear();

        String answer;
        try {
            // 把 7 个 Provider（~15 个工具）全部注册，让 LLM 按用户意图自主选工具。
            // 工具执行时 quality 被 MarketDataClient 自动采集，无需手动记录。
            answer = openAiChatClient.prompt()
                    .tools(trend, sentiment, leader, mainForce, fundFlow, theme, realtime)
                    .user(String.format(
                            "查询股票 %s 的行情，返回技术面、情绪面、资金面、龙头/主线信息。"
                            + "重要：不要传 date 参数，让服务端自动使用最新交易日。"
                            + "如果某类数据获取失败请如实说明，不要编造数据。", symbol))
                    .call()
                    .content();
        } finally {
            // finally 里 clear：无论是否异常都要清理 ThreadLocal，防泄漏
            quality.clear();
        }

        // quality 由工具执行的真实结果决定，不是模型自报
        String overallQuality = quality.overall();
        log.info("[Agent 出口] symbol={}, quality={}, answerLen={}, toolFail={}, toolEmpty={}",
                symbol, overallQuality, answer != null ? answer.length() : 0,
                quality.hasFail(), quality.hasEmpty());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("symbol", symbol);
        response.put("quality", overallQuality);
        response.put("answer", (answer == null || answer.isBlank()) ? null : answer);

        // —— 降级文案：用户必须收到明确的产品提示，而不是空对象或异常 ——
        if ("FAIL".equals(overallQuality)) {
            // 有工具失败（超时/异常）→ 绝不编造 close 等数据
            response.put("close", null);
            response.put("dataAvailable", false);
            response.put("degraded", true);
            response.put("notice", "行情接口异常（部分工具超时或报错），数据暂不可用，请勿基于编造数据决策");
        } else if ("PARTIAL".equals(overallQuality)) {
            // 无失败但部分工具无数据（204）→ 可用数据已进 answer，提示缺失
            response.put("dataAvailable", true);
            response.put("degraded", true);
            response.put("notice", "部分行情数据源暂无数据（如情绪/资金接口返回空），已基于可用数据整理");
        } else if (answer == null || answer.isBlank()) {
            // 模型未给出最终回答（常见于 maxToolCalls 耗尽，最后一轮是未执行的工具调用）
            response.put("quality", "DEGRADED");
            response.put("dataAvailable", false);
            response.put("degraded", true);
            response.put("notice", "行情查询未能在工具调用上限内完成，数据不完整，请稍后重试或缩小查询范围");
        }
        return response;
    }
}
