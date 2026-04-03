# lancode 设计方案

## 项目定位

miniClaudeCode（Python 版）的 Java 17 移植版。目标：功能完全对等，交互格式一致，可直接运行。

## 架构

Maven 单模块，手动 JSON schema 工具注册（动态，非 Jackson 注解），使用 Anthropic Java SDK 标准 `messages()` API。

```
Main.java                  ← CLI 入口 + REPL（交互/单次两种模式）
AgentLoop.java             ← 核心循环：prompt → API → tool_use → execute → loop
├── ConversationContext    ← 消息列表管理（含截断）
├── PermissionGate         ← 2层权限门（工具自检 + 模式检查）
├── SystemPrompt           ← 系统提示构建（含 CLAUDE.md 加载）
└── ToolRegistry           ← 工具注册表 + apiSchemas() → SDK Tool 列表
    ├── BashTool           ← ProcessBuilder 执行，危险命令检测
    ├── FileReadTool
    ├── FileWriteTool
    ├── FileEditTool       ← StrReplace 语义，要求 old_string 唯一
    ├── GlobTool
    └── GrepTool
```

## 关键技术决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 工具定义方式 | 手动 JSON schema（`ObjectNode`） | 工具动态注册，不适合静态 Jackson 注解 |
| API 调用 | `client.messages().create()` | 非 beta，稳定 API |
| ContentBlock 判断 | `isText()`/`isToolUse()` + `asText()`/`asToolUse()` | SDK union type 的安全访问方式 |
| tool input 转换 | `MAPPER.convertValue(block._input(), TypeReference)` | JsonValue → Map<String,Object> |
| tools 参数类型 | `ToolUnion.ofTool(...)` 包装 | SDK 要求 `List<ToolUnion>`，非 `List<Tool>` |
| 上下文截断 | 保留第一条 + 最新 N 条 | 简单截断，对应 Python 版逻辑 |
| 打包 | maven-shade-plugin fat jar | 单文件分发，`java -jar` 直接运行 |

## 与 Python 版对比

| 特性 | Python 版 | Java 版 |
|------|-----------|---------|
| 权限层数 | 2层 | 2层（一致） |
| 工具数量 | 6 | 6（一致） |
| 交互格式 | `[Tool: bash] {...}` | 完全一致 |
| 上下文截断 | 简单截断 | 简单截断（一致） |
| API 调用 | 同步 | 同步（一致） |
| 流式输出 | 否 | 否（一致） |

## 运行方式

```bash
export ANTHROPIC_API_KEY=sk-ant-...

# 交互模式
java -jar target/lancode-0.1.0.jar

# 单次模式
java -jar target/lancode-0.1.0.jar "列出当前目录的文件"

# 指定参数
java -jar target/lancode-0.1.0.jar --mode auto --max-turns 10 "你的问题"
```

## 依赖

```xml
com.anthropic:anthropic-java:0.8.0
com.fasterxml.jackson.core:jackson-databind:2.17.0
org.junit.jupiter:junit-jupiter:5.10.2 (test)
```

## 文件结构

```
lancode/
├── pom.xml
├── .gitignore
├── claude-md/
│   └── design.md          ← 本文件
└── src/
    ├── main/java/com/lancode/
    │   ├── Main.java
    │   ├── AgentLoop.java
    │   ├── Config.java
    │   ├── ConversationContext.java
    │   ├── PermissionGate.java
    │   ├── SystemPrompt.java
    │   └── tools/
    │       ├── Tool.java
    │       ├── ToolResult.java
    │       ├── ToolRegistry.java
    │       ├── BashTool.java
    │       ├── FileReadTool.java
    │       ├── FileWriteTool.java
    │       ├── FileEditTool.java
    │       ├── GlobTool.java
    │       └── GrepTool.java
    └── test/java/com/lancode/
        └── tools/
            ├── ToolResultTest.java
            ├── BashToolTest.java
            └── FileToolsTest.java
```
