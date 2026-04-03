package com.lancode;

import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.ToolResultBlockParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationContext {

    private final Config config;
    private final List<MessageParam> messages = new ArrayList<>();
    private String systemPrompt = "";

    public ConversationContext(Config config) {
        this.config = config;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

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

    /** 返回消息列表供 API 调用 */
    public List<MessageParam> getMessages() {
        return List.copyOf(messages);
    }

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
}
