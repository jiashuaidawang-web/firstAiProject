package com.edy.firstai.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 调用可观测性。
 *
 * <h2>架构师视角：AI 系统必须盯的黄金指标</h2>
 * <ul>
 *   <li>QPS / 错误率（按 model、错误码）</li>
 *   <li>延迟：TTFT（流式首 token）与端到端 latency</li>
 *   <li>Token 用量：prompt / completion（成本中心）</li>
 *   <li>限流与降级次数</li>
 * </ul>
 *
 * Spring AI 2.0 在 ChatClient 层也接入了 Micrometer Observation；
 * 本组件是业务侧补充打点，便于按「业务模型名」聚合。
 *
 * OpenTelemetry GenAI semantic conventions（持续演进）：
 * https://opentelemetry.io/docs/specs/semconv/gen-ai/
 */
@Component
public class AIMetricsService {

    private final MeterRegistry meterRegistry;

    public AIMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordChatMetrics(String modelName, long durationMs, boolean success) {
        meterRegistry.counter(
                "ai.chat.calls",
                "model", modelName,
                "status", success ? "success" : "failure"
        ).increment();

        meterRegistry.timer("ai.chat.duration", "model", modelName)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
