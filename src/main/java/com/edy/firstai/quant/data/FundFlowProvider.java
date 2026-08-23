package com.edy.firstai.quant.data;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资金流向类工具：fund-flow/board、fund-flow/dragon-tiger、fund-flow/dragon-tiger/detail。
 */
@Component
public class FundFlowProvider {

    private final MarketDataClient client;

    public FundFlowProvider(MarketDataClient client) {
        this.client = client;
    }

    @Tool(name = "getFundFlowBoard", description = "板块资金流向：板块代码/名称、主力/超大单/大单净流入、板块内上涨/下跌家数。symbol(6位数字，即 boardCode) 可选。")
    public MarketDataClient.Result<List<Object>> getFundFlowBoard(String symbol, String date, Integer top) {
        String path = "/fund-flow/board";
        boolean has = false;
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
            has = true;
        }
        if (date != null && !date.isBlank()) {
            path += (has ? "&" : "?") + "date=" + date;
            has = true;
        }
        if (top != null) {
            path += (has ? "&" : "?") + "top=" + top;
        }
        return client.getList(path);
    }

    @Tool(name = "getDragonTiger", description = "龙虎榜个股：上榜原因、解读、净买/总买/总卖、上榜成交额、涨跌幅、收盘价、换手率、流通市值。symbol(6位数字) 可选。")
    public MarketDataClient.Result<List<Object>> getDragonTiger(String symbol, String date) {
        String path = "/fund-flow/dragon-tiger";
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

    @Tool(name = "getDragonTigerDetail", description = "龙虎榜席位买卖明细：席位名、类型、排名、买/卖/净买、净买占比、成交额。必传 symbol(6位数字)。")
    public MarketDataClient.Result<List<Object>> getDragonTigerDetail(String symbol, String date) {
        String tsCode = client.toTsCode(symbol); // 必传，校验
        String path = "/fund-flow/dragon-tiger/detail?tsCode=" + tsCode;
        if (date != null && !date.isBlank()) path += "&date=" + date;
        return client.getList(path);
    }
}
