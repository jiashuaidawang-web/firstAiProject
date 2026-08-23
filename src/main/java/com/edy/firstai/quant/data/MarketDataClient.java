package com.edy.firstai.quant.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.edy.firstai.quant.tool.ToolQualityCollector;

import java.util.List;
import java.util.Map;
import java.time.Duration;

/**
 * 行情 HTTP 基础层：symbol 校验 + 市场后缀映射 + Result 包装。
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
 */
@Component
public class MarketDataClient {

    private static final Logger log = LoggerFactory.getLogger(MarketDataClient.class);

    /** 6 位数字校验正则 */
    private static final String SYMBOL_PATTERN = "^\\d{6}$";

    private final RestClient restClient;
    private final ToolQualityCollector quality;

    public MarketDataClient(@Value("${app.ai.market.base-url:http://localhost:8090/api/v1}") String baseUrl,
                            ToolQualityCollector quality) {
        // 直连目标，绕过本机代理（Clash/V2Ray 等默认会拦截导致 Connection reset）
        // Reactor Netty 的 HttpClient.create() 默认不走系统代理 = 直连
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.ReactorClientHttpRequestFactory(
                        reactor.netty.http.client.HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10))))
                .build();
        this.quality = quality;
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

    // ------------------------------------------------------------------ HTTP

    /**
     * GET 请求，返回 Result<T>。统一处理 204/异常，永不抛出让模型编造数据。
     *
     * @param path  路径（含 query），如 /trend/scan?tsCode=600519.SH&minFeature=4
     * @param clazz 目标类型
     */
    public <T> Result<T> get(String path, Class<T> clazz) {
        return get(path, clazz, this.quality);
    }

    /** GET 请求，可选采集质量 */
    public <T> Result<T> get(String path, Class<T> clazz, ToolQualityCollector collector) {
        long start = System.currentTimeMillis();
        log.info("[工具HTTP] ▶ 开始请求 | path={}", path);
        try {
            T body = restClient.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.warn("[工具HTTP] 错误状态码: {} path={}", response.getStatusCode(), path);
                    })
                    .body(clazz);
            long elapsed = System.currentTimeMillis() - start;
            if (body == null) {
                log.info("[工具HTTP] ■ 结束 | 耗时: {}ms | 结果: 空(204)", elapsed);
                Result<T> r = Result.empty("无数据(204): " + path);
                if (collector != null) collector.record(r.quality());
                return r;
            }
            int size = body instanceof Map ? ((Map<?, ?>) body).size()
                    : body instanceof List ? ((List<?>) body).size() : 1;
            log.info("[工具HTTP] ■ 结束 | 耗时: {}ms | 结果: {} 条/字段", elapsed, size);
            Result<T> r = Result.real(body);
            if (collector != null) collector.record(r.quality());
            return r;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[工具HTTP] ■ 异常 | 耗时: {}ms | 错误: {}", elapsed, e.getMessage());
            Result<T> r = Result.fail("接口异常: " + e.getMessage());
            if (collector != null) collector.record(r.quality());
            return r;
        }
    }

    /** GET 返回 List */
    public Result<List<Object>> getList(String path) {
        return getList(path, this.quality);
    }

    /** GET 返回 List，可选采集质量 */
    public Result<List<Object>> getList(String path, ToolQualityCollector collector) {
        long start = System.currentTimeMillis();
        log.info("[工具HTTP] ▶ 开始请求 | path={}", path);
        try {
            List<Object> body = restClient.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) ->
                            log.warn("[工具HTTP] 错误状态码: {} path={}", response.getStatusCode(), path))
                    .body(new ParameterizedTypeReference<List<Object>>() {});
            long elapsed = System.currentTimeMillis() - start;
            Result<List<Object>> r = body == null ? Result.empty("无数据: " + path) : Result.real(body);
            log.info("[工具HTTP] ■ 结束 | 耗时: {}ms | 结果: {} 条", elapsed,
                    body == null ? 0 : body.size());
            if (collector != null) collector.record(r.quality());
            return r;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[工具HTTP] ■ 异常 | 耗时: {}ms | 错误: {}", elapsed, e.getMessage());
            Result<List<Object>> r = Result.fail("接口异常: " + e.getMessage());
            if (collector != null) collector.record(r.quality());
            return r;
        }
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
