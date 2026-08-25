# A-Quant Agent OS · 60天生产级 AI 量化 / Agent 架构师作战手册

## 目标
60天内完成一个面向 A 股研究场景的生产级 AI Quant Agent 平台。

最终能力：
- Java 企业级后端工程
- LLM Gateway
- RAG / Hybrid Retrieval / Reranker
- Tool Calling
- MCP
- Single Agent / Multi-Agent
- Memory
- Agent Eval / Regression
- A股数据工程
- 因子、策略、回测
- 可观测性、可靠性、Docker 化

## 安全边界
本项目第一阶段只做：
- 数据研究
- 历史回测
- 信号研究
- 模拟组合
- AI 投研报告

不做真实自动交易下单。所有量化结果均为研究用途，不构成投资建议。

## 推荐仓库结构
```text
a-quant-agent-os/
├── quant-domain/
├── quant-application/
├── quant-infrastructure/
├── ai-gateway/
├── agent-runtime/
├── rag-service/
├── tool-service/
├── mcp-server/
├── evaluation/
├── market-data-python/
├── deploy/
├── docs/
└── tests/
```

## 每日执行流程
1. 阅读当天目标
2. 先理解 🔴 必须理解
3. 将“开发 Prompt”交给 Claude/Cursor
4. 自己 Review 设计
5. Agent 实现代码
6. 运行测试
7. 使用 Code Review Prompt 审查
8. 使用验收 Prompt 验收
9. 提交 Git

## 状态定义
- 🔴 必须理解：面试和架构设计时必须能自己讲清楚
- 🟡 应该理解：知道为什么这样设计，能查文档复习
- 🟢 可以交给 Agent：可以 AI 生成，但必须 Review

## 阶段
- P0 Day 01-03：项目定义与架构
- P1 Day 04-10：A股数据底座
- P2 Day 11-17：量化策略与回测
- P3 Day 18-24：LLM Gateway 与金融 RAG
- P4 Day 25-31：Tool Calling 与 MCP
- P5 Day 32-38：Single Agent Runtime
- P6 Day 39-45：Multi-Agent
- P7 Day 46-52：Memory 与 Evaluation
- P8 Day 53-60：生产化、验证与答辩
