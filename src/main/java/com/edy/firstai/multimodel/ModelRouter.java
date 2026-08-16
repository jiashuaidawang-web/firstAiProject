package com.edy.firstai.multimodel;

import org.springframework.ai.chat.client.ChatClient;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 模型路由器：把「选哪个模型」从业务代码中抽离。
 *
 * <h2>架构动机（你做微服务治理时的同构问题）</h2>
 * <ul>
 *   <li>成本：便宜模型处理简单问答，贵模型处理复杂推理</li>
 *   <li>能力：代码 / 数学 / 中文 / 长上下文各有所长</li>
 *   <li>可用性：主模型限流时降级到备用 Provider（Circuit Breaker 思维）</li>
 *   <li>合规：数据出境 vs 国内模型</li>
 * </ul>
 *
 * 进阶可演进为：
 * <ul>
 *   <li>基于意图分类的 Semantic Router（先用小模型分类再路由）</li>
 *   <li>与 Spring Cloud Gateway / Resilience4j 组合做 failover</li>
 * </ul>
 *
 * LangChain 对照：RunnableBranch / 自定义 routing runnable
 * https://python.langchain.com/docs/how_to/routing/
 */
public class ModelRouter {

    private final Map<String, ChatClient> clients;

    public ModelRouter(Map<String, ChatClient> clients) {
        this.clients = Map.copyOf(clients);
    }

    public ChatClient route(String modelName) {
        ChatClient client = clients.get(modelName.toLowerCase(Locale.ROOT));
        if (client == null) {
            throw new IllegalArgumentException(
                    "不支持的模型: " + modelName + "，可选: " + clients.keySet());
        }
        return client;
    }

    /**
     * 演示用启发式路由（生产请换成规则引擎 / 分类模型 / 配置中心策略）。
     */
    public ChatClient smartRoute(String task) {
        String lower = task.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "中文", "国内", "合规", "qwen", "通义")) {
            return route("qwen");
        }
        if (containsAny(lower, "推理", "数学", "复杂", "reason", "deepseek")) {
            return route("deepseek");
        }
        return route("longcat");
    }

    public Set<String> availableModels() {
        return clients.keySet();
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
