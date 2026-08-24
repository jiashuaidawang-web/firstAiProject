package com.edy.firstai.quant.data;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 趋势类工具：trend/scan、trend/leading。
 */
@Component
public class TrendDataProvider {

    private final MarketDataClient client;

    public TrendDataProvider(MarketDataClient client) {
        this.client = client;
    }

    @Tool(name = "scanTrend", description = "扫描个股技术面趋势。返回收盘价、MA10、MA30、RSI、八大技术特征命中数、趋势是否成立。入参 symbol 为 6 位数字如 600519。")
    public MarketDataClient.Result<List<Object>> scanTrend(String symbol, Integer minFeature) {
        // trend/scan 返回 JSON 数组（List<TrendScanVO>），用 getList 接
        String tsCode = client.toTsCode(symbol);
        String path = "/trend/scan?tsCode=" + tsCode;
        if (minFeature != null) path += "&minFeature=" + minFeature;
        return client.getList(path);
    }

    @Tool(name = "getLeadingStocks", description = "获取领涨股监控列表。返回个股代码、名称、命中特征数、RS 代理、趋势是否成立。入参 symbol 为 6 位数字如 600519（用于过滤；不过滤则传空）。")
    public MarketDataClient.Result<List<Object>> getLeadingStocks(String symbol, Integer minFeature) {
        String path = "/trend/leading";
        boolean hasParam = false;
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
            hasParam = true;
        }
        if (minFeature != null) {
            path += (hasParam ? "&" : "?") + "minFeature=" + minFeature;
        }
        return client.getList(path);
    }
}
