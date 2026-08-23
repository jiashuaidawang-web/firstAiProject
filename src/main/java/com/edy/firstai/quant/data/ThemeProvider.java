package com.edy.firstai.quant.data;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 题材类工具：theme/factor。
 */
@Component
public class ThemeProvider {

    private final MarketDataClient client;

    public ThemeProvider(MarketDataClient client) {
        this.client = client;
    }

    @Tool(name = "getThemeFactor", description = "题材炒作因子：稀缺/想象/突发/确定/最小阻力方向、综合炒作因子分。传 symbol(6位数字，即 boardCode) 返回单题材；不传返回全题材按综合分降序。")
    public MarketDataClient.Result<List<Object>> getThemeFactor(String symbol, String date) {
        String path = "/theme/factor";
        boolean has = false;
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
            has = true;
        }
        if (date != null && !date.isBlank()) {
            path += (has ? "&" : "?") + "date=" + date;
        }
        return client.getList(path);
    }
}
