package com.edy.firstai.quant.data;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主力资金类工具：main-force/stocks、main-force/seats。
 */
@Component
public class MainForceProvider {

    private final MarketDataClient client;

    public MainForceProvider(MarketDataClient client) {
        this.client = client;
    }

    @Tool(name = "getMainForceStocks", description = "龙虎榜个股级主力合力：上榜原因、净买/总买/总卖、买/卖席位数、机构/游资/北向净买、合力强度、可信度。symbol(6位数字) 可选。")
    public MarketDataClient.Result<List<Object>> getMainForceStocks(String symbol, String date) {
        String path = "/main-force/stocks";
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

    @Tool(name = "getMainForceSeats", description = "抱团席位：同席位跨多股净买。返回席位名、类型（机构/营业部/沪股通）、涉及个股数、净买合计、涉及股票代码列表。symbol 可传空。")
    public MarketDataClient.Result<List<Object>> getMainForceSeats(String symbol, String date) {
        String path = "/main-force/seats";
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
