package com.edy.firstai.quant.data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 阶段 1 行情验收接口。
 *
 * <pre>
 * GET /api/quant/market?symbol=600519
 * </pre>
 *
 * 日志应能看到三步：tool call 请求 → Java 方法进入 → 第二次模型调用。
 */
@RestController
@RequestMapping("/api/quant")
public class MarketController {

    private final MarketDataAgent marketDataAgent;

    public MarketController(MarketDataAgent marketDataAgent) {
        this.marketDataAgent = marketDataAgent;
    }

    @GetMapping("/market")
    public Map<String, Object> market(@RequestParam String symbol) {
        return marketDataAgent.queryMarket(symbol);
    }
}
