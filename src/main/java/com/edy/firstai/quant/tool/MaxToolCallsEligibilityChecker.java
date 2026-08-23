package com.edy.firstai.quant.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;

/**
 * 阶段 1 守卫：给 Agent 工具循环加上 maxToolCalls 上限。
 *
 * <h2>为什么需要它？</h2>
 * Spring AI 2.0.0 的 {@code ToolCallingAdvisor} 工具循环是 {@code while(isToolCall)}，
 * 靠「模型不再请求工具」自然终止——<b>公开 API 里没有 maxToolCalls</b>。
 * 若工具失败、模型反复重试同一工具，就可能死循环。
 *
 * <h2>实现原理</h2>
 * {@link ToolExecutionEligibilityChecker} 是 advisor 工具循环的门控：
 * {@code isToolCallResponse()} 返回 false → {@code while} 循环立即终止。
 * 这里包一层默认判断，并叠加「已调用次数」计数：
 * <ul>
 *   <li>未超限 → 放行（返回默认判断）</li>
 *   <li>已超限 → 返回 false，强制终止循环</li>
 * </ul>
 * 计数用 {@link ThreadLocal}：advisor 是单例，但每次工具循环跑在各自调用线程上，
 * 且循环是同步阻塞的（{@code call()}），所以 ThreadLocal 线程隔离安全。
 *
 * <h2>跨请求不残留（自清理）</h2>
 * {@code while} 循环只在 {@code isToolCallResponse} 返回 false 时退出，
 * 所以「模型给出最终回答」与「超限终止」这两条出口都调用 {@link #clear()}。
 * 下一请求（即便是线程池复用的同一线程）计数都从 0 开始，无需外部 reset。
 *
 * <h2>「工具失败不得死循环」的保障</h2>
 * 工具失败时，错误信息会被拼回 instructions 再喂给模型；模型可能再次调用同一工具。
 * maxToolCalls 兜底：超限后循环强制退出，不再喂给模型重试。
 */
public class MaxToolCallsEligibilityChecker implements ToolExecutionEligibilityChecker {

    private static final Logger log = LoggerFactory.getLogger(MaxToolCallsEligibilityChecker.class);

    /** 兜底默认判断：沿用 Spring AI「响应是否包含工具调用」的内置逻辑 */
    private final ToolExecutionEligibilityChecker delegate;

    /** 单次请求最多允许的工具调用次数 */
    private final int maxToolCalls;

    /** 每请求计数器：ThreadLocal 保证线程隔离 */
    private final ThreadLocal<Integer> callCount = ThreadLocal.withInitial(() -> 0);

    public MaxToolCallsEligibilityChecker(ToolExecutionEligibilityChecker delegate, int maxToolCalls) {
        this.delegate = delegate;
        this.maxToolCalls = maxToolCalls;
    }

    @Override
    public boolean isToolCallResponse(ChatResponse chatResponse) {
        // 先过默认判断：这次响应本身是不是工具调用
        boolean toolCall = this.delegate.isToolCallResponse(chatResponse);
        if (!toolCall) {
            // 正常结束（模型给出最终回答）→ 清理计数
            clear();
            return false;
        }
        // 是工具调用：检查是否已超限
        int count = this.callCount.get();
        if (count >= this.maxToolCalls) {
            log.warn("工具调用已达上限 maxToolCalls={}，强制终止工具循环，防止死循环", this.maxToolCalls);
            clear(); // 超限终止 → 清理计数
            return false; // ← 关键：返回 false 切断 while(isToolCall) 循环
        }
        this.callCount.set(count + 1);
        return true;
    }

    /** 实现 Function<ChatResponse, Boolean> —— 委托给 isToolCallResponse */
    @Override
    public Boolean apply(ChatResponse chatResponse) {
        return isToolCallResponse(chatResponse);
    }

    /** 清理 ThreadLocal，避免线程池复用导致计数残留 */
    private void clear() {
        this.callCount.remove();
    }
}
