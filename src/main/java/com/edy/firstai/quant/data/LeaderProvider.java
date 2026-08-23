package com.edy.firstai.quant.data;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 龙头/主线类工具：leaders、leader/trade-idea、mainline。
 */
@Component
public class LeaderProvider {

    private final MarketDataClient client;

    public LeaderProvider(MarketDataClient client) {
        this.client = client;
    }

    @Tool(name = "getLeaders", description = "龙头个股列表：代码、名称、所属板块、连板数、角色（龙一/龙二/妖股/独狼）、龙头相评分、成交额。可按 symbol(6位数字) 过滤。")
    public MarketDataClient.Result<List<Object>> getLeaders(String symbol, String date) {
        String path = "/leaders";
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

    @Tool(name = "getTradeIdea", description = "龙头买卖建议：买卖动作（买入/低吸/持有/减仓/卖出/观望）、买卖信号、风险等级、买卖评分、理由。可按 symbol(6位数字) 过滤。")
    public MarketDataClient.Result<List<Object>> getTradeIdea(String symbol, String date) {
        String path = "/leader/trade-idea";
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

    @Tool(name = "getMainline", description = "主线板块列表：板块代码/名称、主线层级（一线/二线/三线）、综合强度、排名。symbol 可传空。")
    public MarketDataClient.Result<List<Object>> getMainline(String symbol, String date) {
        String path = "/mainline";
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
