package com.edy.firstai.quant.tool;

import com.edy.firstai.quant.data.MarketDataClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工具质量采集器（per-request）。
 *
 * <h2>解决什么问题</h2>
 * Agent 最终 JSON 的 quality 必须反映<b>工具执行的真实结果</b>，不能靠模型自报。
 * 每个工具执行后把 {@link MarketDataClient.Quality} 记到这里，
 * Agent 据此计算最终 quality：
 * <ul>
 *   <li>全部 REAL → "REAL"</li>
 *   <li>任一 FAIL → "FAIL"（此时不得编造 close 等数据）</li>
 *   <li>无 FAIL 但有 EMPTY → "PARTIAL"</li>
 * </ul>
 *
 * <h2>为什么用 ThreadLocal</h2>
 * advisor 是单例、工具 provider 是单例，但每次 Agent 调用跑在各自线程。
 * ThreadLocal 保证并发调用互不污染；请求结束必须 {@link #clear()} 防泄漏。
 */
@Component
public class ToolQualityCollector {

    private final ThreadLocal<List<MarketDataClient.Quality>> qualities =
            ThreadLocal.withInitial(ArrayList::new);

    /** 记录一次工具调用的质量 */
    public void record(MarketDataClient.Quality quality) {
        qualities.get().add(quality);
    }

    /** 计算最终质量 */
    public String overall() {
        List<MarketDataClient.Quality> list = qualities.get();
        if (list.isEmpty()) return "EMPTY";
        boolean hasFail = list.contains(MarketDataClient.Quality.FAIL);
        boolean hasEmpty = list.contains(MarketDataClient.Quality.EMPTY);
        if (hasFail) return "FAIL";
        if (hasEmpty) return "PARTIAL";
        return "REAL";
    }

    /** 是否有 FAIL（Agent 据此决定是否允许返回 close 等数据） */
    public boolean hasFail() {
        return qualities.get().contains(MarketDataClient.Quality.FAIL);
    }

    /** 是否有 EMPTY（Agent 据此给出 PARTIAL 降级文案） */
    public boolean hasEmpty() {
        return qualities.get().contains(MarketDataClient.Quality.EMPTY);
    }

    /** 清理，防 ThreadLocal 泄漏 */
    public void clear() {
        qualities.remove();
    }
}
