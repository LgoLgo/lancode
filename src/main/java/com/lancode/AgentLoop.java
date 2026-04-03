package com.lancode;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lancode.tools.Tool;
import com.lancode.tools.ToolRegistry;
import com.lancode.tools.ToolResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentLoop {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Config config;
    private final ConversationContext context;
    private final ToolRegistry registry;
    private final PermissionGate permissionGate;
    private final AnthropicClient client;

    public AgentLoop(Config config, ConversationContext context, ToolRegistry registry, PermissionGate permissionGate) {
        this.config = config;
        this.context = context;
        this.registry = registry;
        this.permissionGate = permissionGate;
        this.client = AnthropicOkHttpClient.fromEnv();
    }

    public String run(String userMessage) {
        context.addUserMessage(userMessage);
        String finalText = "";

        for (int turn = 0; turn < config.maxTurns; turn++) {
            Message response = callApi();
            List<ContentBlock> contentBlocks = response.content();

            // 解析 text parts 和 tool calls
            List<String> textParts = new ArrayList<>();
            List<Map<String, Object>> toolCalls = new ArrayList<>();

            for (ContentBlock block : contentBlocks) {
                if (block.isText()) {
                    String text = block.asText().text();
                    textParts.add(text);
                    System.out.print(text);
                    System.out.flush();
                } else if (block.isToolUse()) {
                    ToolUseBlock toolUse = block.asToolUse();
                    Map<String, Object> inputMap = convertJsonValue(toolUse._input());
                    String name = toolUse.name();
                    String id = toolUse.id();

                    System.out.println("\n[Tool: " + name + "] " + inputMap);
                    System.out.flush();

                    Map<String, Object> callInfo = new HashMap<>();
                    callInfo.put("id", id);
                    callInfo.put("name", name);
                    callInfo.put("input", inputMap);
                    toolCalls.add(callInfo);
                }
            }

            if (!textParts.isEmpty()) {
                finalText = String.join("", textParts);
            }

            if (toolCalls.isEmpty()) {
                context.addAssistantMessage(contentBlocks);
                break;
            }

            context.addAssistantMessage(contentBlocks);
            executeToolCalls(toolCalls);
        }

        return finalText;
    }

    private Message callApi() {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(config.model)
                .maxTokens(8192)
                .system(context.getSystemPrompt())
                .tools(registry.apiSchemas().stream().map(ToolUnion::ofTool).toList())
                .messages(context.getMessages())
                .build();
        return client.messages().create(params);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertJsonValue(JsonValue jsonValue) {
        try {
            return MAPPER.convertValue(jsonValue, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void executeToolCalls(List<Map<String, Object>> toolCalls) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> call : toolCalls) {
            String id = (String) call.get("id");
            String name = (String) call.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> input = (Map<String, Object>) call.get("input");

            ToolResult result = executeSingleTool(name, input);

            String prefix = result.isError() ? "  -> Permission denied: " : "  -> [OK] ";
            // 如果是错误但不是 permission denied，用通用前缀
            if (result.isError() && !result.output().startsWith("Permission denied:")) {
                prefix = "  -> [Error] ";
            } else if (result.isError()) {
                prefix = "  -> ";
            }
            System.out.println(prefix + result.output());
            System.out.flush();

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("tool_use_id", id);
            resultMap.put("content", result.output());
            resultMap.put("is_error", result.isError());
            results.add(resultMap);
        }

        context.addToolResults(results);
    }

    private ToolResult executeSingleTool(String name, Map<String, Object> input) {
        Tool tool = registry.get(name);
        if (tool == null) {
            return ToolResult.error("Tool not found: " + name);
        }

        ToolResult denied = permissionGate.check(tool, input);
        if (denied != null) {
            return denied;
        }

        try {
            return tool.execute(input);
        } catch (Exception e) {
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }
}
