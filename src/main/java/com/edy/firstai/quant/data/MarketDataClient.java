package com.edy.firstai.quant.data;

import com.edy.firstai.metrics.AIMetricsService;
import com.edy.firstai.quant.tool.FaultInjection;
import com.edy.firstai.quant.tool.ToolQualityCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 行情 HTTP 基础层：symbol 校验 + 市场后缀映射 + Result 包装 + 可观测性。
 *
 * <h2>symbol 校验（防模型绕过）</h2>
 * 工具入参统一收 6 位数字。非法直接抛 {@link IllegalArgumentException} 并审计日志——
 * 在工具入口就挡住，模型无法「说服」工具接受非法入参。
 *
 * <h2>6 位数字 → tsCode（带市场后缀）</h2>
 * 真实 API 要 tsCode=600519.SH。映射规则（A 股通用）：
 * <ul>
 *   <li>600/601/603/605/688/689 → .SH（沪市主板/科创板）</li>
 *   <li>000/001/002/003/300/301 → .SZ（深市主板/创业板）</li>
 *   <li>430/83x/87x/88x/920 → .BJ（北交所）</li>
 *   <li>未知前缀 → 默认 .SH（并打 warn，让 API 决定）</li>
 * </ul>
 *
 * <h2>Result 包装 + quality</h2>
 * 每个工具返回 {@link Result}，带 {@link Quality} 标记：
 * REAL=真实数据 / FAIL=接口异常 / EMPTY=204 无数据。
 * Agent 据此决定最终 JSON 的 quality 字段，FAIL 时不得编造 close。
 *
 * <h2>工具名感知（提示词 3）</h2>
 * 带 {@code toolName} 的重载是主路径。工具 Provider 调用时传入自身工具名，用于：
 * <ol>
 *   <li>故障注入：按工具名决定是否抛 TimeoutException / 返回空 bar（{@link FaultInjection}）</li>
 *   <li>打点：{@code quant.tool.calls{tool,status}} 计数器（{@link AIMetricsService}）</li>
 * </ol>
 * 不带 toolName 的重载保留给非工具调用路径，tool 标记载 "unknown"。
 *
 * <h2>进程不得空转</h2>
 * <ul>
 *   <li>HTTP 层：{@code responseTimeout(10s)}，超时变 FAIL 质量，绝不阻塞</li>
 *   <li>循环层：{@code MaxToolCallsEligibilityChecker} 限制 maxToolCalls=5，工具失败不会死循环</li>
 * </ul>
 */
@Component
public class MarketDataClient {

    private static final Logger log = LoggerFactory.getLogger(MarketDataClient.class);

    /** 6 位数字校验正则 */
    private static final String SYMBOL_PATTERN = "^\\d{6}$";

    private final RestClient restClient;
    private final ToolQualityCollector quality;
    private final FaultInjection faultInjection;
    private final AIMetricsService aiMetrics;

    public MarketDataClient(@Value("${app.ai.market.base-url:http://localhost:8090/api/v1}") String baseUrl,
                            ToolQualityCollector quality,
                            FaultInjection faultInjection,
                            AIMetricsService aiMetrics) {
        // 直连目标，绕过本机代理（Clash/V2Ray 等默认会拦截导致 Connection reset）
        // Reactor Netty 的 HttpClient.create() 默认不走系统代理 = 直连
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.ReactorClientHttpRequestFactory(
                        reactor.netty.http.client.HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10))))
                .build();
        this.quality = quality;
        this.faultInjection = faultInjection;
        this.aiMetrics = aiMetrics;
        log.info("行情 HTTP 客户端初始化, baseUrl={}, 直连(无代理)", baseUrl);
    }

    // ------------------------------------------------------------------ 校验

    /**
     * 校验 symbol 必须是 6 位数字。非法直接拒绝并审计。
     * @return 校验通过的原值
     * @throws IllegalArgumentException 非法时
     */
    public String requireValidSymbol(String symbol) {
        if (symbol == null || !symbol.matches(SYMBOL_PATTERN)) {
            log.warn("[审计] 非法 symbol 入参被拒绝: {}", symbol);
            throw new IllegalArgumentException(
                    "symbol 必须是 6 位数字，收到: " + symbol);
        }
        return symbol;
    }

    /** 6 位数字 → tsCode（带 .SH/.SZ/.BJ 后缀） */
    public String toTsCode(String symbol) {
        requireValidSymbol(symbol);
        String prefix = symbol.substring(0, 3);
        String suffix;
        switch (prefix) {
            case "600", "601", "603", "605", "688", "689" -> suffix = ".SH";
            case "000", "001", "002", "003", "300", "301" -> suffix = ".SZ";
            default -> {
                // 430/83x/87x/88x/920 → 北交所；其它未知前缀保守走 .SH
                char first = symbol.charAt(0);
                if (first == '4' || first == '8' || first == '9') {
                    suffix = ".BJ";
                } else {
                    suffix = ".SH";
                }
                log.warn("未知 symbol 前缀 {}, 默认映射到 {}, symbol={}", prefix, suffix, symbol);
            }
        }
        return symbol + suffix;
    }

    // ------------------------------------------------------------------ HTTP（工具名感知主路径）

    /** GET 请求，工具名感知（主路径）：带故障注入 + quant.tool.calls 打点 */
    public <T> Result<T> get(String toolName, String path, Class<T> clazz) {
        return get(toolName, path, clazz, this.quality);
    }

    /** GET 请求，工具名感知 + 可选质量采集 */
    public <T> Result<T> get(String toolName, String path, Class<T> clazz, ToolQualityCollector collector) {
        // —— 故障注入：超时（在发 HTTP 前抛出，模拟响应超时） ——
        if (faultInjection.shouldThrowTimeout(toolName)) {
            log.warn("[故障注入] 工具 {} 模拟 TimeoutException", toolName);
            // 超时 = 接口异常 → FAIL 质量，与真实超时语义一致
            if (collector != null) collector.record(Quality.FAIL);
            aiMetrics.recordToolCall(toolName, "timeout");
            throw new RuntimeException(new TimeoutException("模拟超时: " + toolName));
        }
        // —— 故障注入：空 bar（在发 HTTP 前返回 EMPTY，模拟 204） ——
        if (faultInjection.shouldReturnEmpty(toolName)) {
            log.warn("[故障注入] 工具 {} 模拟返回空 bar", toolName);
            if (collector != null) collector.record(Quality.EMPTY);
            aiMetrics.recordToolCall(toolName, "empty");
            return Result.empty("模拟空 bar(无数据): " + path);
        }

        long start = System.currentTimeMillis();
        log.info("[工具HTTP] ▶ 开始请求 | tool={} | path={}", toolName, path);
        try {
            T body = restClient.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) ->
                            log.warn("[工具HTTP] 错误状态码: {} tool={} path={}", response.getStatusCode(), toolName, path))
                    .body(clazz);
            long elapsed = System.currentTimeMillis() - start;
            if (body == null) {
                log.info("[工具HTTP] ■ 结束 | tool={} | 耗时: {}ms | 结果: 空(204)", toolName, elapsed);
                if (collector != null) collector.record(Quality.EMPTY);
                aiMetrics.recordToolCall(toolName, "empty");
                return Result.empty("无数据(204): " + path);
            }
            int size = body instanceof Map ? ((Map<?, ?>) body).size()
                    : body instanceof List ? ((List<?>) body).size() : 1;
            log.info("[工具HTTP] ■ 结束 | tool={} | 耗时: {}ms | 结果: {} 条/字段", toolName, elapsed, size);
            if (collector != null) collector.record(Quality.REAL);
            aiMetrics.recordToolCall(toolName, "success");
            return Result.real(body);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            // 真实超时（Reactor Netty responseTimeout）也归类为 timeout 状态
            String status = (e.getCause() instanceof TimeoutException) ? "timeout" : "error";
            log.error("[工具HTTP] ■ 异常 | tool={} | 耗时: {}ms | status={} | 错误: {}",
                    toolName, elapsed, status, e.getMessage());
            if (collector != null) collector.record(Quality.FAIL);
            aiMetrics.recordToolCall(toolName, status);
            return Result.fail("接口异常: " + e.getMessage());
        }
    }

    /** GET 返回 List，工具名感知（主路径） */
    public Result<List<Object>> getList(String toolName, String path) {
        return getList(toolName, path, this.quality);
    }

    /** GET 返回 List，工具名感知 + 可选质量采集 */
    public Result<List<Object>> getList(String toolName, String path, ToolQualityCollector collector) {
        // —— 故障注入：超时 ——
        if (faultInjection.shouldThrowTimeout(toolName)) {
            log.warn("[故障注入] 工具 {} 模拟 TimeoutException", toolName);
            if (collector != null) collector.record(Quality.FAIL);
            aiMetrics.recordToolCall(toolName, "timeout");
            throw new RuntimeException(new TimeoutException("模拟超时: " + toolName));
        }
        // —— 故障注入：空 bar ——
        if (faultInjection.shouldReturnEmpty(toolName)) {
            log.warn("[故障注入] 工具 {} 模拟返回空 bar", toolName);
            if (collector != null) collector.record(Quality.EMPTY);
            aiMetrics.recordToolCall(toolName, "empty");
            return Result.empty("模拟空 bar(无数据): " + path);
        }

        long start = System.currentTimeMillis();
        log.info("[工具HTTP] ▶ 开始请求 | tool={} | path={}", toolName, path);
        try {
            List<Object> body = restClient.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) ->
                            log.warn("[工具HTTP] 错误状态码: {} tool={} path={}", response.getStatusCode(), toolName, path))
                    .body(new ParameterizedTypeReference<List<Object>>() {});
            long elapsed = System.currentTimeMillis() - start;
            log.info("[工具HTTP] ■ 结束 | tool={} | 耗时: {}ms | 结果: {} 条", toolName, elapsed,
                    body == null ? 0 : body.size());
            if (body == null) {
                if (collector != null) collector.record(Quality.EMPTY);
                aiMetrics.recordToolCall(toolName, "empty");
                return Result.empty("无数据: " + path);
            }
            if (collector != null) collector.record(Quality.REAL);
            aiMetrics.recordToolCall(toolName, "success");
            return Result.real(body);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            String status = (e.getCause() instanceof TimeoutException) ? "timeout" : "error";
            log.error("[工具HTTP] ■ 异常 | tool={} | 耗时: {}ms | status={} | 错误: {}",
                    toolName, elapsed, status, e.getMessage());
            if (collector != null) collector.record(Quality.FAIL);
            aiMetrics.recordToolCall(toolName, status);
            return Result.fail("接口异常: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ HTTP（兼容旧路径，tool=unknown）

    /** 兼容：不带工具名的 GET（tool 标记载 unknown，无故障注入） */
    public <T> Result<T> get(String path, Class<T> clazz) {
        return get("unknown", path, clazz, this.quality);
    }

    /** 兼容：不带工具名的 GET + 可选质量采集 */
    public <T> Result<T> get(String path, Class<T> clazz, ToolQualityCollector collector) {
        return get("unknown", path, clazz, collector);
    }

    /** 兼容：不带工具名的 getList */
    public Result<List<Object>> getList(String path) {
        return getList("unknown", path, this.quality);
    }

    /** 兼容：不带工具名的 getList + 可选质量采集 */
    public Result<List<Object>> getList(String path, ToolQualityCollector collector) {
        return getList("unknown", path, collector);
    }

    // ------------------------------------------------------------------ 质量标记

    /** 数据质量：REAL=真实 / FAIL=接口异常 / EMPTY=无数据 */
    public enum Quality { REAL, FAIL, EMPTY }

    /** 工具内部返回的包装：带 quality + 数据/错误 */
    public record Result<T>(T data, Quality quality, String message) {
        public static <T> Result<T> real(T data) {
            return new Result<T>(data, Quality.REAL, null);
        }
        public static <T> Result<T> fail(String message) {
            return new Result<T>(null, Quality.FAIL, message);
        }
        public static <T> Result<T> empty(String message) {
            return new Result<T>(null, Quality.EMPTY, message);
        }
        public boolean isReal() { return quality == Quality.REAL; }

        /** 类型转换（HTTP 返回 Map 转更具体类型时用，quality 保留） */
        @SuppressWarnings("unchecked")
        public <U> Result<U> cast() {
            return new Result<>((U) data, quality, message);
        }
    }

}
