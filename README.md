<div align="center">

# lancode

**A minimal Claude Code agent loop in Java.**

[![CI](https://github.com/LgoLgo/lancode/actions/workflows/ci.yml/badge.svg)](https://github.com/LgoLgo/lancode/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)
[![中文](https://img.shields.io/badge/文档-中文版-red.svg)](README.zh.md)

~700 lines. Runs against any Anthropic-compatible API.

</div>

---

Distilled from Claude Code's architecture — the agentic loop, tool system, permission model, and context management — stripped to the essential structure.

## Install

Requires Java 17+ and Maven.

```bash
git clone https://github.com/LgoLgo/lancode
cd lancode
mvn package -q -DskipTests
```

## Configure

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

All fields are optional. Without a config file, `ANTHROPIC_API_KEY` is read from the environment and the official Anthropic endpoint is used. Set `baseUrl` to point at any Anthropic-compatible API.

## Run

```bash
# interactive REPL
java -jar target/lancode-0.1.0.jar

# one-shot
java -jar target/lancode-0.1.0.jar "list files in the current directory"

# override config at runtime
java -jar target/lancode-0.1.0.jar --model claude-opus-4-5 --mode ask
```

## REPL commands

| Command | Description |
|---------|-------------|
| `/tools` | List available tools |
| `/mode [ask\|auto\|plan]` | Show or change permission mode |
| `/help` | Show help |
| `/quit` | Exit |

## settings.json reference

| Field | Default | Description |
|-------|---------|-------------|
| `model` | `claude-opus-4-5` | Model name passed to the API |
| `baseUrl` | Anthropic official | Override API endpoint |
| `apiKey` | `$ANTHROPIC_API_KEY` | API key; env var used as fallback |
| `permissionMode` | `AUTO` | `AUTO` \| `ASK` \| `PLAN` |
| `maxTurns` | `30` | Max agent loop iterations per message |
| `maxContextMessages` | `100` | Message history limit before truncation |

**Permission modes**

- `AUTO` — all tools execute without confirmation
- `ASK` — bash commands not on the safe list prompt `[y/N]`
- `PLAN` — read-only; bash, write_file, and edit_file are blocked

## Tools

| Tool | Description |
|------|-------------|
| `bash` | Execute shell commands via `ProcessBuilder` |
| `read_file` | Read file contents |
| `write_file` | Write or create a file |
| `edit_file` | Replace an exact string in a file (StrReplace) |
| `glob` | Find files by glob pattern |
| `grep` | Search file contents by regex |

## Architecture

```
Main                    CLI entry point, REPL, arg parsing
AgentLoop               Core loop: prompt → API → tool_use → execute → repeat
  ConversationContext   Message list with truncation
  PermissionGate        Two-layer: tool self-check + mode enforcement
  SystemPrompt          Assembles system prompt from tools + CLAUDE.md + mode
  ToolRegistry          Registers tools, produces API schemas
    Tool (interface)    name / description / inputSchema / execute
    ToolResult (record) output + isError
```

The loop terminates when the model returns a response with no `tool_use` blocks, or when `maxTurns` is reached.

## How it relates to Claude Code

Claude Code is ~500k lines across 28 subsystems. lancode keeps 4:

| Subsystem | Claude Code | lancode |
|-----------|-------------|---------|
| Agent loop | SSE streaming, parallel | Synchronous, sequential |
| Tools | 26+ types, MCP extension | 6 core tools |
| Permissions | 5 layers + hooks | 2 layers |
| Context | Persistent, compaction | In-memory, truncation |

## Development

```bash
mvn test                   # run tests
mvn compile                # compile only
mvn package -DskipTests    # build fat jar
```

Tests live in `src/test/java/com/lancode/tools/`.

## License

Apache 2.0
