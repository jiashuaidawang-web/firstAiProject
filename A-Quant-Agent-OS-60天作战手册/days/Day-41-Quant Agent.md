# Day 41 · Quant Agent

## 今日目标
让专业 Agent 调用因子、策略和回测能力。

## 最终产物
- 根据今日需求生成代码、测试、文档和可运行结果
- Git Commit：`feat: add quant specialist agent`

## 今日需求
- [ ] Quant Agent
- [ ] Backtest Tool Usage
- [ ] Quant Result Contract

## 🔴 必须理解
Quant Agent 负责解释和编排，不应该自己虚构计算结果。

### 今天必须能回答
1. 为什么要这样设计？
2. 如果不用这个设计会发生什么？
3. 它和上一阶段如何衔接？
4. 它未来会被哪个 Agent / RAG / Quant 模块使用？

## 🟡 应该理解
结果置信度。

## 🟢 可以交给 Agent
prompt。

## 开发步骤
1. 先阅读现有代码和上一天产物。
2. 使用主开发 Agent Prompt。
3. 先让 Agent 输出设计和修改计划。
4. 自己确认模块边界。
5. 再允许 Agent 修改代码。
6. 运行编译和测试。
7. 使用 Code Review Prompt。
8. 使用今日验收标准。
9. 提交 Git。

## Claude / Cursor 开发 Prompt
```text
你正在开发 A-Quant Agent OS 的 Day 41：Quant Agent。

今天的目标：
让专业 Agent 调用因子、策略和回测能力。

今天必须完成：
- Quant Agent
- Backtest Tool Usage
- Quant Result Contract

关键架构原则：
Quant Agent 负责解释和编排，不应该自己虚构计算结果。

实现 Quant Agent：选择已有策略/指标，调用真实计算工具，解释 metrics。

开始前：
1. 阅读当前仓库结构和相关代码。
2. 不要立即写代码。
3. 先输出：现状分析、设计方案、影响模块、文件修改计划、测试计划。
4. 等方案明确后实施最小必要修改。
5. 不允许大规模重构无关模块。
6. 完成后必须运行编译和相关测试。

最终输出：
- 修改文件
- 核心设计
- 测试结果
- 已知风险
- 下一步建议
```

## Code Review Prompt
```text
请作为 A-Quant Agent OS 的 Staff Engineer Review Day 41：Quant Agent。

重点检查：
- 是否完成今日需求
- 是否符合模块边界
- 是否把 Infrastructure/AI 细节泄漏到 Domain
- 是否存在 Demo 级 Shortcut
- 是否有异常处理、日志和测试
- 是否存在数据时间边界错误
- 是否影响后续 RAG、Tool、MCP、Agent、Eval 阶段

特别检查：
检查 Agent 输出是否完全可追溯到真实 backtest result。

输出：
1. A/B/C/D/F
2. P0/P1/P2 问题
3. 最小修复方案
4. 是否允许进入下一天
```

## 验收 Prompt
```text
你是 CTO、AI Agent 架构师和量化系统 Reviewer。

请对 Day 41：Quant Agent 做严格验收。

今日验收目标：
- 无虚构指标
- 结果可追溯
- 异常数据可提示

必须基于代码、测试、日志、接口或实际运行证据判断。
不要接受“理论上可以”。

输出：
- 已完成
- 未完成
- 证据
- 风险
- 最终评级 A/B/C/D/F
- 是否允许进入 Day 42
```

## 验收标准
- [ ] 无虚构指标
- [ ] 结果可追溯
- [ ] 异常数据可提示

## Git Commit
```text
feat: add quant specialist agent
```

## 今日复盘
- 今天真正理解了什么：
- 今天 AI 替我完成了什么：
- 今天发现的架构问题：
- 哪段代码我必须自己重新读一遍：
- 明天开始前必须解决：
