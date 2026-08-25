package com.edy.firstai.quant.data;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 实时类工具：realtime/status、realtime/stage。
 *
 * <p>realtime/stream 是 SSE 流式事件（FEATURE/DECISION/SIM_TRADE），
 * 不适合同步 tool call 的「请求-响应」模型，故不注册为 tool。
 */
@Component
public class RealtimeProvider {

    private final MarketDataClient client;

    public RealtimeProvider(MarketDataClient client) {
        this.client = client;
    }

    @Tool(name = "getRealtimeStatus", description = "实时盘口快照：当前情绪阶段、窗口标的数、关注池大小、各标的最新价/涨跌幅/封单额/成交额。symbol(6位数字) 可选过滤。")
    public MarketDataClient.Result<Map<String, Object>> getRealtimeStatus(String symbol) {
        String path = "/realtime/status";
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
        }
        return client.get("getRealtimeStatus", path, Map.class).cast();
    }

    @Tool(name = "getStage", description = "当前情绪阶段码。合法值：ICE/CHAOS/DIVERGE/DIVERGE_CONSENSUS/CONSENSUS/CLIMAX/STARTUP/REPAIR。symbol 可传空。")
    public MarketDataClient.Result<Map<String, Object>> getStage(String symbol) {
        String path = "/realtime/stage";
        if (symbol != null && !symbol.isBlank()) {
            path += "?tsCode=" + client.toTsCode(symbol);
        }
        return client.get("getStage", path, Map.class).cast();
    }
}
