---
title: 第四章：对话上下文
nav_order: 5
---

# 第四章：对话上下文

## 是什么

对话上下文维护 Agent 与 API 之间的消息列表，负责：

1. **消息管理** — 将用户输入、Assistant 响应、工具结果按 Anthropic 协议格式化并存储
2. **自动截断** — 当消息数超过 `maxContextMessages` 时自动删除中间历史，保留首条消息（初始指令）和最新的对话
3. **System Prompt 存储** — 加载并维护系统提示词

对应文件：`ConversationContext.java`

## 关键代码片段

**1. 三种消息类型的添加方法**

```java
/** 添加用户文本消息 */
public void addUserMessage(String content) {
    messages.add(
        MessageParam.builder()
            .role(MessageParam.Role.USER)
            .content(content)
            .build()
    );
    _truncateIfNeeded();
}

/** 添加 assistant 响应（含 text + tool_use blocks） */
public void addAssistantMessage(List<ContentBlock> content) {
    List<ContentBlockParam> params = content.stream()
        .map(ContentBlock::toParam)
        .toList();
    messages.add(
        MessageParam.builder()
            .role(MessageParam.Role.ASSISTANT)
            .contentOfBlockParams(params)
            .build()
    );
    _truncateIfNeeded();
}

/**
 * 添加工具结果（role=user，内容是 tool_result blocks）。
 * 每个 map 需包含：tool_use_id(String)、content(String)、is_error(Boolean, optional)
 */
public void addToolResults(List<Map<String, Object>> results) {
    List<ContentBlockParam> blocks = results.stream()
        .map(r -> {
            String toolUseId = (String) r.get("tool_use_id");
            String content = (String) r.getOrDefault("content", "");
            boolean isError = (boolean) r.getOrDefault("is_error", false);

            ToolResultBlockParam param = ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content(content)
                .isError(isError)
                .build();
            return ContentBlockParam.ofToolResult(param);
        })
        .toList();

    messages.add(
        MessageParam.builder()
            .role(MessageParam.Role.USER)
            .contentOfBlockParams(blocks)
            .build()
    );
    _truncateIfNeeded();
}
```

**约定**：工具结果以 `role=user` 发送，内容是 `tool_result` block——这是 Anthropic API 的协议要求，工具结果必须包在用户消息里。

**2. 截断策略 — _truncateIfNeeded()**

```java
/** 超出 maxContextMessages 时保留第一条 + 最新 N 条 */
void _truncateIfNeeded() {
    int max = config.maxContextMessages;
    if (messages.size() <= max) return;

    // 保留第一条 + 最新 (max-1) 条
    MessageParam first = messages.get(0);
    List<MessageParam> tail = new ArrayList<>(
        messages.subList(messages.size() - (max - 1), messages.size())
    );
    messages.clear();
    messages.add(first);
    messages.addAll(tail);
}
```

**策略说明**：
- 每当消息列表超出 `maxContextMessages` 限制时自动触发
- **保留首条** — 通常是初始用户指令，承载用户的核心意图
- **保留最新 N-1 条** — 保留最近的对话和工具交互历史，维持上下文连贯性
- 中间历史被删除，实现了简单但有效的**滑动窗口**策略

## 小结

ConversationContext 是 Agent Loop 的"记忆"。它的关键设计取舍是：

- **牺牲中间历史** — 删除过期的、不相关的对话片段，节省 token 成本
- **保留意图和最近上下文** — 首条消息锚定初始请求，最新消息维持当前任务的连贯性

这样即使对话很长，API 调用也能保持在可控的 token 预算内，同时不失总体的对话连贯性。
