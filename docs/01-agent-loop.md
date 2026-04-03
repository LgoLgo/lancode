---
title: 第一章：Agent Loop
---

# 第一章：Agent Loop

## 是什么

Agent Loop 是整个系统的核心驱动器。它负责：向 API 发送消息、解析响应中的文本和工具调用、执行工具、把结果追加到对话上下文，然后再次调用 API——循环直到没有更多工具调用或达到最大轮次。

对应文件：`AgentLoop.java`

## 关键代码片段

**1. 主循环结构**

```java
for (int turn = 0; turn < config.maxTurns; turn++) {
    Message response = callApi();
    // 解析 text + tool_use blocks
    if (toolCalls.isEmpty()) {
        context.addAssistantMessage(contentBlocks);
        break;          // 没有工具调用，结束循环
    }
    context.addAssistantMessage(contentBlocks);
    executeToolCalls(toolCalls);
}
```

循环的退出条件只有两个：没有工具调用（正常结束），或达到 `maxTurns`（防止无限循环）。

**2. 区分 text 和 tool_use block**

```java
for (ContentBlock block : contentBlocks) {
    if (block.isText()) {
        System.out.print(block.asText().text());
    } else if (block.isToolUse()) {
        ToolUseBlock toolUse = block.asToolUse();
        // 收集工具调用信息
    }
}
```

Anthropic Java SDK 用 `isText()` / `isToolUse()` 判断 block 类型，而不是 `instanceof`。

**3. 把响应 block 转回请求参数**

```java
context.addAssistantMessage(contentBlocks);
// 内部调用：content.stream().map(ContentBlock::toParam).toList()
```

`ContentBlock.toParam()` 是多轮对话的关键：它把 API 返回的 block 转换成下一轮请求所需的 `ContentBlockParam` 格式，保持对话历史完整。

## 小结

Agent Loop 的本质是一个"调用 API → 执行工具 → 再调用 API"的 for 循环。所有状态都存在 `ConversationContext` 里，Loop 本身是无状态的。
