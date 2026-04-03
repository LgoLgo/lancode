---
title: 第二章：工具系统
---

# 第二章：工具系统

## 是什么

工具系统是 Agent Loop 的执行引擎。它由三部分组成：

1. **Tool 接口** — 定义工具的统一契约（4 个方法）
2. **ToolRegistry** — 工具的注册表和解耦层
3. **6 个内置工具** — BashTool、FileReadTool、FileWriteTool、FileEditTool、GlobTool、GrepTool

对应文件：`tools/Tool.java`、`tools/ToolRegistry.java`、`tools/BashTool.java`、`tools/FileReadTool.java`、`tools/FileWriteTool.java`、`tools/FileEditTool.java`、`tools/GlobTool.java`、`tools/GrepTool.java`

## 关键代码片段

**1. Tool 接口**

```java
public interface Tool {
    String name();
    String description();
    ObjectNode inputSchema();

    default ToolResult checkPermissions(Map<String, Object> params) {
        return null; // null = 允许；非null = 拒绝
    }

    ToolResult execute(Map<String, Object> params);
}
```

每个工具只需实现 4 个方法。`checkPermissions` 有默认实现（返回 `null`，即允许执行），只有需要自检的工具才覆盖它。

**2. apiSchemas() — 内部契约转外部契约**

```java
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
```

Registry 在 API 调用前把内部 `com.lancode.tools.Tool` 接口转换成 Anthropic SDK 的 `Tool` 对象。两者同名但类型不同——这是工具系统对外的唯一接口。

**3. defaultRegistry() — 6 个工具的注册**

```java
public static ToolRegistry defaultRegistry(Config config) {
    ToolRegistry registry = new ToolRegistry();
    registry.register(new BashTool(config));
    registry.register(new FileReadTool());
    registry.register(new FileWriteTool());
    registry.register(new FileEditTool());
    registry.register(new GlobTool());
    registry.register(new GrepTool());
    return registry;
}
```

6 个工具覆盖了 Claude Code 的核心能力：

| 工具 | 职责 |
|------|------|
| BashTool | 执行 shell 命令 |
| FileReadTool | 读文件 |
| FileWriteTool | 写文件 |
| FileEditTool | 精确编辑（StrReplace） |
| GlobTool | 文件搜索（glob 模式） |
| GrepTool | 内容搜索（正则表达式） |

## 小结

工具系统通过 **Tool 接口** 实现统一契约，通过 **ToolRegistry** 实现解耦。Registry 同时封装了两个职责：1）管理内部工具的注册和查询；2）在 API 调用时把内部工具转换成 SDK 工具对象。这个设计使得添加新工具只需实现 `Tool` 接口，无需修改 API 调用逻辑。
