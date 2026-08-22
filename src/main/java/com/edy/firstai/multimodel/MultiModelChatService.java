package com.edy.firstai.multimodel;

import com.edy.firstai.exception.AIException;
import com.edy.firstai.metrics.AIMetricsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 多模型业务服务层：演示「路由 + 重试 + 指标」的生产骨架。
 *
 * <p>对应你熟悉的分层：Controller 薄、Service 编排、基础设施（Retry/Metrics）横切。
 * AI 应用并不神秘，只是下游从 DB/RPC 变成了 LLM Provider。
 */
@Service
public class MultiModelChatService {

    private final ModelRouter modelRouter;
    private final RetryTemplate aiRetryTemplate;
    private final AIMetricsService metricsService;

    public MultiModelChatService(
            ModelRouter modelRouter,
            RetryTemplate aiRetryTemplate,
            AIMetricsService metricsService) {
        this.modelRouter = modelRouter;
        this.aiRetryTemplate = aiRetryTemplate;
        this.metricsService = metricsService;
    }

    public String chatWithModel(String modelName, String prompt) {
        return invoke(modelName, modelRouter.route(modelName), prompt);
    }

    public String smartChat(String prompt) {
        // smartRoute 内部已决定模型；这里用模型名标签打点
        ChatClient client = modelRouter.smartRoute(prompt);
        String modelTag = resolveModelTag(client);
        return invoke(modelTag, client, prompt);
    }

    /**
     * A/B / 评测场景：同一 prompt 打多个<b>可用</b>模型，便于人工或自动评估。
     * 评估体系可参考：LLM-as-a-Judge（Zheng et al., "Judging LLM-as-a-Judge...", NeurIPS 2023）
     * <p>
     * 迭代：不再硬编码模型列表，改为从 {@link ModelRouter#availableModels()} 取——
     * 未配置 / 未开启的 Provider（阶段 0 的 DeepSeek / Qwen）不会出现，也就不会误触。
     */
    public Map<String, String> compareModels(String prompt) {
        Map<String, String> results = new HashMap<>();
        for (String modelName : modelRouter.availableModels()) {
            try {
                results.put(modelName, chatWithModel(modelName, prompt));
            } catch (Exception e) {
                results.put(modelName, "错误: " + e.getMessage());
            }
        }
        return results;
    }

    private String invoke(String modelName, ChatClient client, String prompt) {
        long start = System.currentTimeMillis();
        boolean success = false;
        try {
            // Framework 7：RetryTemplate.execute(Retryable<T>) —— 无 RetryContext 参数
            String content = aiRetryTemplate.execute(() -> client.prompt()
                    .user(prompt)
                    .call()
                    .content());
            success = true;
            return content;
        } catch (Exception e) {
            throw new AIException("模型[" + modelName + "]调用失败: " + e.getMessage(), e);
        } finally {
            metricsService.recordChatMetrics(modelName, System.currentTimeMillis() - start, success);
        }
    }

    private String resolveModelTag(ChatClient client) {
        for (String name : modelRouter.availableModels()) {
            if (modelRouter.route(name) == client) {
                return name;
            }
        }
        return "unknown";
    }
}
