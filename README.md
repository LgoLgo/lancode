# lancode

A minimal Claude Code agent loop in Java. ~800 lines. Runs against any Anthropic-compatible API.

Distilled from Claude Code's architecture: the agentic loop, tool system, permission model, and context management — stripped to the essential structure.

## Install

Requires Java 17+ and Maven.

```
git clone <repo>
cd lancode
mvn package -q -DskipTests
```

## Configure

```
mkdir -p ~/.lancode
cat > ~/.lancode/settings.json << 'EOF'
{
  "model": "LongCat-Flash-Lite",
  "baseUrl": "https://api.longcat.chat/anthropic",
  "apiKey": "your-key-here",
  "permissionMode": "AUTO"
}
EOF
```

All fields are optional. Without a config file, `ANTHROPIC_API_KEY` is read from the environment and the official Anthropic endpoint is used.

## Run

```
# interactive REPL
java -jar target/lancode-0.1.0.jar

# one-shot
java -jar target/lancode-0.1.0.jar "list files in the current directory"

# override config at runtime
java -jar target/lancode-0.1.0.jar --model claude-opus-4-5 --mode ask
```

## REPL commands

```
/tools      list available tools
/mode       show or change permission mode (ask / auto / plan)
/help       show this list
/quit       exit
```

## settings.json reference

| Field            | Default               | Description                                      |
|------------------|-----------------------|--------------------------------------------------|
| `model`          | `LongCat-Flash-Lite`  | Model name passed to the API                     |
| `baseUrl`        | Anthropic official    | API endpoint, e.g. `https://api.longcat.chat/anthropic` |
| `apiKey`         | `$ANTHROPIC_API_KEY`  | API key; env var used as fallback                |
| `permissionMode` | `AUTO`                | `AUTO` \| `ASK` \| `PLAN`                       |
| `maxTurns`       | `30`                  | Max agent loop iterations per message            |
| `maxContextMessages` | `100`             | Message history limit before truncation          |

**Permission modes**

- `AUTO` — all tools execute without confirmation
- `ASK` — bash commands not on the safe list prompt `[y/N]`
- `PLAN` — read-only; bash, write_file, and edit_file are blocked

## Tools

| Tool         | Description                                      |
|--------------|--------------------------------------------------|
| `bash`       | Execute shell commands via `ProcessBuilder`      |
| `read_file`  | Read file contents                               |
| `write_file` | Write or create a file                           |
| `edit_file`  | Replace an exact string in a file (StrReplace)   |
| `glob`       | Find files by glob pattern                       |
| `grep`       | Search file contents by regex                    |

## Architecture

```
Main                    CLI entry point, REPL, arg parsing
AgentLoop               Core loop: prompt -> API -> tool_use -> execute -> repeat
  ConversationContext   Message list with truncation
  PermissionGate        Two-layer: tool self-check + mode enforcement
  SystemPrompt          Assembles system prompt from tools + CLAUDE.md + mode
  ToolRegistry          Registers tools, produces API schemas
    Tool (interface)    name / description / inputSchema / execute
    ToolResult (record) output + isError
```

The loop terminates when the model returns a response with no tool_use blocks, or when `maxTurns` is reached.

## How it relates to Claude Code

Claude Code is ~500k lines across 28 subsystems. lancode keeps 4:

| Subsystem         | Claude Code               | lancode                        |
|-------------------|---------------------------|--------------------------------|
| Agent loop        | SSE streaming, parallel   | Synchronous, sequential        |
| Tools             | 26+ types, MCP extension  | 6 core tools                   |
| Permissions       | 5 layers + hooks          | 2 layers                       |
| Context           | Persistent, compaction    | In-memory, truncation          |

## Development

```
mvn test          # run tests
mvn compile       # compile only
mvn package -DskipTests   # build fat jar
```

Tests live in `src/test/java/com/lancode/tools/`.

## License

MIT
