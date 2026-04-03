package com.lancode.tools;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import com.lancode.Config;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, com.lancode.tools.Tool> tools = new LinkedHashMap<>();

    public void register(com.lancode.tools.Tool tool) {
        tools.put(tool.name(), tool);
    }

    public com.lancode.tools.Tool get(String name) {
        return tools.get(name);
    }

    public List<com.lancode.tools.Tool> allTools() {
        return List.copyOf(tools.values());
    }

    public List<Tool> apiSchemas() {
        List<Tool> result = new ArrayList<>();
        for (com.lancode.tools.Tool tool : tools.values()) {
            var schema = tool.inputSchema();
            Tool.InputSchema inputSchema = Tool.InputSchema.builder()
                .properties(JsonValue.from(schema.get("properties")))
                .build();
            Tool apiTool = Tool.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(inputSchema)
                .build();
            result.add(apiTool);
        }
        return result;
    }

    public static ToolRegistry defaultRegistry(Config config) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new BashTool(config));
        return registry;
    }
}
