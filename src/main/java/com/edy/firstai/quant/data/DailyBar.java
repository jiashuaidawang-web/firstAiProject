package com.edy.firstai.quant.data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日 K 线（Daily Bar）—— 行情只读工具的输出契约（DTO）。
 *
 * <p>用 record：不可变 + 清晰 schema，天然适合做 LLM 工具返回值。
 * 阶段 1 只读：这是纯数据对象，没有任何写行为。
 */
public record DailyBar(
        String symbol,
        LocalDate tradeDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume) {
}
