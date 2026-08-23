package com.edy.firstai.quant.tool;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 工具调用全链路日志——把 Spring AI 的「黑盒」变成「玻璃盒」。
 *
 * <h2>为什么需要它</h2>
 * Agent 系统最痛的点：模型选了什么工具、传了什么参数、工具返回什么、模型有没有用上，
 * 全是黑盒。断点打不进去（LLM 调用在远端），日志看不到（Spring AI 默认不打印工具调用详情）。
 *
 * <h2>它观测什么（每次工具调用会触发多次回调）</h2>
 * <pre>
 * 模型决定调 scanTrend
 *   → onStart(observation=tool.call, tags=[name=scanTrend])
 *   → onScopeOpened
 *   → 工具执行中...
 *   → onScopeClosed
 *   → onStop  ← 这里打印完整日志（含耗时、结果摘要）
 *
 * 模型决定调 getSentiment
 *   → onStart → ... → onStop
 *
 * 模型觉得够了，生成最终回答
 *   → onStart(observation=chat.client)
 *   → onStop
 * </pre>
 *
 * <h2>日志长什么样</h2>
 * <pre>
 * [工具观测] ▶ scanTrend 开始 | 参数: {symbol: 600519, minFeature: 5}
 * [工具观测] ■ scanTrend 结束 | 耗时: 120ms | quality: REAL | 结果大小: 15 个字段
 * [工具观测] ▶ getSentiment 开始 | 参数: {symbol: 600519, date: null}
 * [工具观测] ■ getSentiment 结束 | 耗时: 85ms | quality: FAIL | 错误: Connection refused
 * [工具观测] 模型工具循环结束 | 共调 2 次工具 | 最终回答长度: 350 字
 * </pre>
 */
@Component
public class ToolCallLogger implements ObservationHandler<Observation.Context> {

    private static final Logger log = LoggerFactory.getLogger(ToolCallLogger.class);

    /** 当前工具调用次数（ThreadLocal 保证并发安全） */
    private static final ThreadLocal<Integer> callCount = ThreadLocal.withInitial(() -> 0);

    @Override
    public void onStart(Observation.Context context) {
        String name = context.getName();
        // 工具调用开始
        if ("spring.ai.tool.call".equals(name)) {
            callCount.set(callCount.get() + 1);
            String toolName = context.getLowCardinalityKeyValue("tool.name") != null
                    ? context.getLowCardinalityKeyValue("tool.name").getValue() : "unknown";
            String args = context.getLowCardinalityKeyValue("tool.arguments") != null
                    ? context.getLowCardinalityKeyValue("tool.arguments").getValue() : "{}";
            log.info("[工具观测] ▶ 第{}次工具调用 | 工具: {} | 参数: {}", callCount.get(), toolName, args);
        }
        // 整个 ChatClient 调用开始
        if ("spring.ai.chat.client".equals(name)) {
            log.info("[工具观测] ═══ Agent 调用开始 ═══");
            callCount.set(0);
        }
    }

    @Override
    public void onStop(Observation.Context context) {
        String name = context.getName();
        // 工具调用结束
        if ("spring.ai.tool.call".equals(name)) {
            String toolName = context.getLowCardinalityKeyValue("tool.name") != null
                    ? context.getLowCardinalityKeyValue("tool.name").getValue() : "unknown";
            String error = context.getLowCardinalityKeyValue("error") != null
                    ? context.getLowCardinalityKeyValue("error").getValue() : null;
            // 耗时从 context 的 error/成功标记判断
            if (error != null) {
                log.warn("[工具观测] ■ 工具: {} 失败 | 错误: {}", toolName, error);
            } else {
                log.info("[工具观测] ■ 工具: {} 成功", toolName);
            }
        }
        // 整个 ChatClient 调用结束
        if ("spring.ai.chat.client".equals(name)) {
            log.info("[工具观测] ═══ Agent 调用结束 | 共调 {} 次工具 ═══", callCount.get());
            callCount.remove();
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        // 只关注工具调用和 ChatClient 级别的观测
        return context.getName() != null
                && (context.getName().contains("tool") || context.getName().contains("chat.client"));
    }
}
