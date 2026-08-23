package com.edy.firstai.quant.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 行情数据提供者 —— 阶段 1 只读行情工具。
 *
 * <h2>阶段 1 约束（本类体现）</h2>
 * <ul>
 *   <li>✅ 只读：{@link #getDailyBar} 只返回数据，不触发任何下单/改状态</li>
 *   <li>❌ 不接真实 HTTP 行情：当前写死返回 600519 一根 K 线（阶段 1 验收用）</li>
 * </ul>
 *
 * <h2>@Tool 怎么被 ChatClient 识别</h2>
 * {@code tools(marketDataProvider)} 传入 POJO，Spring AI 2.0 走
 * {@code ToolCallbacks.from(...)} 扫描其 {@code @Tool} 方法，转成 {@code ToolCallback}。
 * 所以工具类不需要实现任何接口，加注解 + 是 Spring bean（注入用）即可。
 */
@Component
public class MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(MarketDataProvider.class);

    /**
     * 查询某只股票某日的日 K 线。
     *
     * <p>当前实现写死返回一根 K 线，便于阶段 1 验收
     * 「工具调用 → 模型拿到数据 → 第二次模型调用」三步链路，无需接真实行情。
     *
     * @param symbol    股票代码，如 "600519"
     * @param tradeDate 交易日，如 "2024-01-02"
     * @return 该日 K 线
     */
    @Tool(name = "getDailyBar", description = "查询指定股票在指定交易日的日K线（开高低收与成交量）")
    public DailyBar getDailyBar(String symbol, String tradeDate) {
        log.info("[工具进入] getDailyBar 被调用: symbol={}, tradeDate={}", symbol, tradeDate);
        // TODO 阶段 2+ 接真实行情 HTTP 源；当前写死返回一根 K 线
        DailyBar bar = new DailyBar(
                symbol,
                LocalDate.parse(tradeDate),
                new BigDecimal("1700.00"),
                new BigDecimal("1720.50"),
                new BigDecimal("1695.00"),
                new BigDecimal("1710.00"),
                1234567L);
        log.info("[工具返回] getDailyBar 返回: close={}, volume={}", bar.close(), bar.volume());
        return bar;
    }
}
