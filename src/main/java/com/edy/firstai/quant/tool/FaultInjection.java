package com.edy.firstai.quant.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 故障注入开关（测试/演练用，默认关闭）。
 *
 * <h2>为什么需要它</h2>
 * 提示词 3 要求：「注入：工具抛 TimeoutException、返回空 bar。用户应收到降级文案，进程不得空转。」
 * 真实行情后端不一定配合故障演练，所以用配置开关在 <b>Client 层</b> 模拟两类故障：
 * <ul>
 *   <li>超时：指定工具抛 {@link java.util.concurrent.TimeoutException}，模拟响应超时</li>
 *   <li>空 bar：指定工具直接返回 {@link com.edy.firstai.quant.data.MarketDataClient.Quality#EMPTY}，模拟 204 无数据</li>
 * </ul>
 * 开关为空 = 不注入，不影响生产路径。
 *
 * <h2>与 maxToolCalls 守卫的关系</h2>
 * 故障注入制造失败；{@link MaxToolCallsEligibilityChecker} 保证失败不会导致工具循环空转——
 * 两者配合完成「熔断 + 降级」验收。
 */
@Component
public class FaultInjection {

    /** 需要模拟超时的工具名（精确匹配 @Tool name），空 = 不注入 */
    private final String timeoutTool;

    /** 需要模拟返回空 bar 的工具名（精确匹配 @Tool name），空 = 不注入 */
    private final String emptyTool;

    public FaultInjection(
            @Value("${app.ai.quant.fault-timeout-tool:}") String timeoutTool,
            @Value("${app.ai.quant.fault-empty-tool:}") String emptyTool) {
        this.timeoutTool = timeoutTool;
        this.emptyTool = emptyTool;
    }

    /** 该工具是否应模拟超时 */
    public boolean shouldThrowTimeout(String toolName) {
        return toolName != null && !timeoutTool.isBlank() && toolName.equals(timeoutTool);
    }

    /** 该工具是否应模拟返回空 bar */
    public boolean shouldReturnEmpty(String toolName) {
        return toolName != null && !emptyTool.isBlank() && toolName.equals(emptyTool);
    }
}
