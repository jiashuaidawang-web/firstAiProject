package com.edy.firstai.quant.data;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 情绪/大盘类工具：sentiment、overview、limit-pool。
 */
@Component
public class SentimentProvider {

    private final MarketDataClient client;

    public SentimentProvider(MarketDataClient client) {
        this.client = client;
    }

    @Tool(name = "getSentiment", description = "查询市场情绪：涨停/跌停家数、最高连板数、昨日涨停股今日均涨幅（赚钱效应）、情绪温度、情绪阶段（冰点/修复/升温/高潮/退潮）。symbol 可传空。")
    public MarketDataClient.Result<Map<String, Object>> getSentiment(String symbol, String date) {
        String path = "/sentiment";
        boolean has = false;
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
            has = true;
        }
        if (date != null && !date.isBlank()) {
            path += (has ? "&" : "?") + "date=" + date;
        }
        return client.get(path, Map.class).cast();
    }

    @Tool(name = "getOverview", description = "大势择时总览：四维度评分（技术/情绪/资金/政策）、牛熊周期、情绪温度、是否值得参与、策略建议。symbol 可传空。")
    public MarketDataClient.Result<Map<String, Object>> getOverview(String symbol, String date) {
        String path = "/overview";
        boolean has = false;
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
            has = true;
        }
        if (date != null && !date.isBlank()) {
            path += (has ? "&" : "?") + "date=" + date;
        }
        return client.get(path, Map.class).cast();
    }

    @Tool(name = "getLimitPool", description = "当日涨停股列表：代码、名称、所属板块、连板数、涨停风格（换手/一字/T字）、涨跌幅、成交额。symbol 可传空。")
    public MarketDataClient.Result<List<Object>> getLimitPool(String symbol, String date, Integer minPos) {
        String path = "/limit-pool";
        boolean has = false;
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
            has = true;
        }
        if (date != null && !date.isBlank()) {
            path += (has ? "&" : "?") + "date=" + date;
            has = true;
        }
        if (minPos != null) {
            path += (has ? "&" : "?") + "minPos=" + minPos;
        }
        return client.getList(path);
    }
}
