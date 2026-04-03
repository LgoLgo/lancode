<div align="center">

<img src="img/img.png" alt="lancode" width="480" />

**用 Java 实现的极简 Claude Code Agent Loop。**

[![CI](https://github.com/LgoLgo/lancode/actions/workflows/ci.yml/badge.svg)](https://github.com/LgoLgo/lancode/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

~700 行。可对接任何 Anthropic 兼容 API。

</div>

---

从 Claude Code 的架构中提炼而来 —— Agent Loop、工具系统、权限模型、上下文管理 —— 剥离到最核心的结构，方便阅读和学习。

## 安装

需要 Java 17+ 和 Maven。

```bash
git clone https://github.com/LgoLgo/lancode
cd lancode
mvn package -q -DskipTests
```

## 配置

```bash
mkdir -p ~/.lancode
cat > ~/.lancode/settings.json << 'EOF'
{
  "model": "claude-opus-4-5",
  "apiKey": "your-key-here",
  "permissionMode": "AUTO"
}
EOF
```

所有字段均为可选。未提供配置文件时，从环境变量 `ANTHROPIC_API_KEY` 读取密钥，使用官方 Anthropic 端点。设置 `baseUrl` 可对接任何兼容 API。

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
| `apiKey` | `$ANTHROPIC_API_KEY` | API 密钥，环境变量作为回退 |
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

## 架构

```
Main                    CLI 入口、REPL、参数解析
AgentLoop               核心循环：prompt → API → tool_use → execute → 循环
  ConversationContext   消息列表，含截断逻辑
  PermissionGate        两层权限：工具自检 + 模式强制
  SystemPrompt          组装系统提示（工具列表 + CLAUDE.md + 模式）
  ToolRegistry          工具注册表，生成 API Schema
    Tool (interface)    name / description / inputSchema / execute
    ToolResult (record) output + isError
```

循环在模型返回不含 `tool_use` 的响应时退出，或达到 `maxTurns` 上限时终止。

## 与 Claude Code 的对比

Claude Code 有 ~50 万行代码，横跨 28 个子系统。lancode 只保留其中 4 个：

| 子系统 | Claude Code | lancode |
|--------|-------------|---------|
| Agent Loop | SSE 流式、并行 | 同步、顺序 |
| 工具 | 26+ 种，支持 MCP 扩展 | 6 个核心工具 |
| 权限 | 5 层 + Hook 系统 | 2 层 |
| 上下文 | 持久化、压缩 | 内存存储、截断 |

## 开发

```bash
mvn test                   # 运行测试
mvn compile                # 仅编译
mvn package -DskipTests    # 构建 fat jar
```

测试位于 `src/test/java/com/lancode/tools/`。

## 许可证

Apache 2.0
