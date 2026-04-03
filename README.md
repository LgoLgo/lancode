<div align="center">

<img src="img/logo.png" alt="lancode" width="480" />

**用 Java 实现的极简 Claude Code Agent Loop。**

[![CI](https://github.com/LgoLgo/lancode/actions/workflows/ci.yml/badge.svg)](https://github.com/LgoLgo/lancode/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

~900 行。可对接任何 Anthropic 兼容 API。

</div>

---

从 Claude Code 的架构中提炼而来 —— Agent Loop、工具系统、权限模型、上下文管理 —— 剥离到最核心的结构，方便阅读和学习。

## 课程

配套课程逐章解析各子系统的实现原理，适合已有 LLM 基础、想深入了解 Agent Loop 内部机制的工程师。

| 章节 | 内容 | 链接 |
|------|------|------|
| 第一章：Agent Loop | 核心循环逻辑 | [阅读](https://lgolgo.github.io/lancode/01-agent-loop) |
| 第二章：工具系统 | Tool 接口、Registry、6 个内置工具 | [阅读](https://lgolgo.github.io/lancode/02-tool-system) |
| 第三章：权限模型 | PermissionGate 双层检查机制 | [阅读](https://lgolgo.github.io/lancode/03-permission) |
| 第四章：对话上下文 | ConversationContext 消息管理与截断 | [阅读](https://lgolgo.github.io/lancode/04-context) |
| 第五章：系统提示词 | SystemPrompt 组装逻辑 | [阅读](https://lgolgo.github.io/lancode/05-system-prompt) |

## 系统设计

### 架构

<img src="img/lancode-architecture.svg" alt="lancode 架构图" width="100%" />

### 时序

一次典型 Agent Loop 的执行过程：

```mermaid
---
title: lancode — 一次典型 Agent Loop
---
%%{
  init: {
    'theme': 'base',
    'themeVariables': {
      'fontFamily': 'Georgia, serif',
      'actorBkg': '#E6E2DA',
      'actorBorder': '#8C867F',
      'actorTextColor': '#2C2C2C',
      'actorLineColor': '#8C867F',
      'signalColor': '#8C867F',
      'signalTextColor': '#2C2C2C',
      'labelBoxBkgColor': '#E6E2DA',
      'labelBoxBorderColor': '#8C867F',
      'labelTextColor': '#2C2C2C',
      'loopTextColor': '#2C2C2C',
      'activationBkgColor': '#CFE8D7',
      'activationBorderColor': '#71AE88',
      'noteBkgColor': '#F3E4DA',
      'noteBorderColor': '#C88E6A',
      'noteTextColor': '#2C2C2C'
    }
  }
}%%
sequenceDiagram
    participant 用户
    participant Main as Main（REPL）
    participant AgentLoop
    participant API as Anthropic API
    participant ToolRegistry
    participant Tool
    participant ConversationContext

    用户 ->> Main: 输入请求
    Main ->> AgentLoop: run(userMessage)
    AgentLoop ->> ConversationContext: addUserMessage()

    loop 循环直到无 tool_use 或达到 maxTurns
        AgentLoop ->> API: callApi()（含消息、系统提示、工具 schema）
        API -->> AgentLoop: 返回响应（text 或 tool_use）

        alt 返回 tool_use
            AgentLoop ->> ToolRegistry: get(toolName)
            ToolRegistry -->> AgentLoop: 返回 Tool 实例
            AgentLoop ->> Tool: execute(input)（先经 PermissionGate 检查）
            Tool -->> AgentLoop: ToolResult
            AgentLoop ->> ConversationContext: addToolResults()
        else 返回纯文本
            AgentLoop ->> ConversationContext: addAssistantMessage()
        end
    end

    AgentLoop -->> Main: 返回最终文本
    Main -->> 用户: 输出结果
```

循环在模型返回不含 `tool_use` 的响应时退出，或达到 `maxTurns` 上限时终止。

### 实现范围

<img src="img/lancode-scope.png" alt="lancode 子系统覆盖范围" width="100%" />

## 安装

需要 Java 17+ 和 Maven。

```bash
git clone https://github.com/LgoLgo/lancode
cd lancode
mvn package -q -DskipTests
```

## 配置

**官方 Anthropic API**

```bash
mkdir -p ~/.lancode
cat > ~/.lancode/settings.json << 'EOF'
{
  "model": "claude-opus-4-5",
  "apiKey": "sk-ant-...",
  "permissionMode": "AUTO"
}
EOF
```

**第三方兼容 API**（如 LongCat、OpenRouter、自托管服务等）

```bash
mkdir -p ~/.lancode
cat > ~/.lancode/settings.json << 'EOF'
{
  "model": "your-model-name",
  "baseUrl": "https://your-api-endpoint",
  "authToken": "your-key-here",
  "permissionMode": "AUTO"
}
EOF
```

第三方 API 通常使用 `Authorization: Bearer` 认证，应使用 `authToken` 字段。`apiKey` 仅用于官方 Anthropic API（发 `x-api-key` 头）。

未提供配置文件时，从环境变量 `ANTHROPIC_API_KEY` 读取密钥，使用官方 Anthropic 端点。

## AGENT.md

在项目根目录放置 `AGENT.md` 文件，可为 lancode 提供项目专属指令，启动时自动加载并注入系统提示。

```
your-project/
├── AGENT.md        ← lancode 自动读取
└── src/
```

这是 lancode 对 Claude Code `CLAUDE.md` 机制的等价实现——命名为 `AGENT.md` 以区分：这是给 agent 的指令，而非给 Claude Code 工具本身的配置。

## 运行

```bash
# 交互式 REPL
java -jar target/lancode-0.1.0.jar

# 单次执行
java -jar target/lancode-0.1.0.jar "列出当前目录的文件"

# 运行时覆盖配置
java -jar target/lancode-0.1.0.jar --model claude-opus-4-5 --mode ask
```

## REPL 命令

| 命令 | 说明 |
|------|------|
| `/tools` | 列出可用工具 |
| `/mode [ask\|auto\|plan]` | 查看或切换权限模式 |
| `/help` | 显示帮助 |
| `/quit` | 退出 |

## settings.json 字段说明

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `model` | `claude-opus-4-5` | 传给 API 的模型名 |
| `baseUrl` | Anthropic 官方端点 | 自定义 API 地址 |
| `apiKey` | `$ANTHROPIC_API_KEY` | 官方 Anthropic API，发 `x-api-key` 头 |
| `authToken` | — | 第三方 API，发 `Authorization: Bearer` 头 |
| `permissionMode` | `AUTO` | `AUTO` \| `ASK` \| `PLAN` |
| `maxTurns` | `30` | 每条消息最大 Agent Loop 轮数 |
| `maxContextMessages` | `100` | 触发截断前的消息历史上限 |

**权限模式说明**

- `AUTO` — 所有工具自动执行，无需确认
- `ASK` — 不在安全列表中的 bash 命令会提示 `[y/N]`
- `PLAN` — 只读模式；bash、write_file、edit_file 被禁用

## 工具列表

| 工具 | 说明 |
|------|------|
| `bash` | 通过 `ProcessBuilder` 执行 shell 命令 |
| `read_file` | 读取文件内容 |
| `write_file` | 写入或创建文件 |
| `edit_file` | 精确字符串替换（StrReplace） |
| `glob` | 按 glob 模式查找文件 |
| `grep` | 按正则搜索文件内容 |

## 开发

```bash
mvn test                   # 运行测试
mvn compile                # 仅编译
mvn package -DskipTests    # 构建 fat jar
```

测试位于 `src/test/java/com/lancode/tools/`。

## 参考

- [ultraworkers/claw-code](https://github.com/ultraworkers/claw-code)
- [bcefghj/miniClaudeCode](https://github.com/bcefghj/miniClaudeCode)
- Special Thanks: [LinuxDO](https://linux.do/)

## 许可证

Apache 2.0
