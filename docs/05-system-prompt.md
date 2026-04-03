---
title: 第五章：系统提示词
---

# 第五章：系统提示词

## 是什么

`SystemPrompt` 在 Agent 启动时组装 system prompt 字符串。它负责将工具列表、权限模式说明和可选的项目指令（来自 `AGENT.md`）合并为完整的"行为契约"提示词。

对应文件：`SystemPrompt.java`

## 关键代码片段

### build() 方法——组装逻辑

```java
public static String build(ToolRegistry registry, Config.PermissionMode mode, String projectDir) {
    String toolList = registry.allTools().stream()
        .map(t -> "- **" + t.name() + "**: " + t.description())
        .reduce("", (a, b) -> a + "\n" + b).strip();
    String instructions = loadAgentMd(projectDir);
    String projectSection = instructions.isEmpty() ? "" :
        "## Project Instructions (from AGENT.md)\n\n" + instructions;
    return TEMPLATE.formatted(toolList, mode.name(), MODE_DESC.get(mode.name()), projectSection).strip();
}
```

**说明**：工具列表在运行时动态生成——注册了哪些工具，system prompt 里就列出哪些。这保证了工具描述和实际能力的一致性。

### loadAgentMd() 方法——加载项目指令

```java
private static String loadAgentMd(String dir) {
    Path p = Path.of(dir).resolve("AGENT.md");
    return Files.exists(p) ? Files.readString(p).strip() : "";
}
```

**说明**：如果项目目录下有 `AGENT.md`，其内容会作为"项目专属指令"追加到 system prompt 末尾。这复刻了 Claude Code 的 `CLAUDE.md` 机制。

## 小结

System prompt 是 Agent 的"行为契约"。最小化的组装逻辑包括三部分：

1. **工具列表**——当前注册的所有工具及其描述
2. **权限模式说明**——STRICT / ALLOW_ALL / ASK 的行为约定
3. **可选项目指令**——从 `AGENT.md` 加载的项目专属指令

这种设计确保了 Agent 的能力和约束与运行时配置保持一致。
