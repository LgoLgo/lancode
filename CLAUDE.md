# lancode — Claude context

## What this is

A minimal Java implementation of Claude Code's agent loop. ~800 lines, single Maven module, Java 17.

The goal is pedagogical fidelity: each class maps directly to a Claude Code subsystem. Do not add abstractions that don't exist in the original.

## Build and test

```bash
mvn compile -q                    # compile
mvn test -q                       # run all tests
mvn package -q -DskipTests        # build fat jar -> target/lancode-0.1.0.jar
```

All changes must pass `mvn test -q` before committing. Do not modify test logic to make tests pass.

## Module map

| File | Responsibility |
|------|---------------|
| `Main.java` | CLI arg parsing, REPL loop |
| `AgentLoop.java` | Core agent loop: call API, parse response, execute tools, repeat |
| `Config.java` | All configuration fields + `Config.load()` from `~/.lancode/settings.json` |
| `ConversationContext.java` | Message list, truncation, system prompt storage |
| `PermissionGate.java` | Two-layer permission check before tool execution |
| `SystemPrompt.java` | Assembles system prompt string; loads `CLAUDE.md` from project root |
| `tools/Tool.java` | Interface: `name()`, `description()`, `inputSchema()`, `execute()` |
| `tools/ToolResult.java` | Record: `output`, `isError` |
| `tools/ToolRegistry.java` | Registry + `apiSchemas()` converting to SDK `Tool` objects |
| `tools/BashTool.java` | Shell execution via `ProcessBuilder` |
| `tools/FileReadTool.java` | `Files.readString` |
| `tools/FileWriteTool.java` | `Files.writeString` with parent dir creation |
| `tools/FileEditTool.java` | StrReplace: requires `old_string` to be unique in file |
| `tools/GlobTool.java` | `Files.walk` + `PathMatcher` |
| `tools/GrepTool.java` | `Files.walk` + `Pattern.compile` per line |

## Key SDK decisions (Anthropic Java SDK 0.8.0)

- `ContentBlock` uses `isText()`/`asText()` and `isToolUse()`/`asToolUse()` — not `instanceof`
- Tool schemas passed as `List<ToolUnion>` via `ToolUnion.ofTool(...)`
- Tool `_input()` returns `JsonValue`; convert with `MAPPER.convertValue(..., TypeReference)`
- `ContentBlock.toParam()` converts response blocks back to `ContentBlockParam` for next turn
- Client built with `AnthropicOkHttpClient.builder().fromEnv()` then optionally `.apiKey()` / `.baseUrl()`

## Configuration loading

`Config.load()` reads `~/.lancode/settings.json` via Jackson. CLI args override after load.
Fields: `model`, `baseUrl`, `apiKey`, `permissionMode`, `maxTurns`, `maxContextMessages`.
File missing = use defaults. Parse error = warn + use defaults.

## Constraints

- Java 17. No records with mutable state. No Lombok.
- All class references via `import`, never fully-qualified inline (e.g., no `com.foo.Bar` in method bodies).
- Do not add dependencies beyond: `anthropic-java`, `jackson-databind`, `junit-jupiter`.
- Do not add tools beyond the current 6 without explicit instruction.
- Do not add a SubAgent / Task mechanism — that is intentionally out of scope.
- `dangerousPatterns` and `allowedCommands` in `Config` are the canonical safety lists; do not duplicate them in individual tools.

## What intentionally does not exist here

These Claude Code subsystems are out of scope and should not be added:

- Session persistence (`~/.claude/sessions/`)
- Context compaction (summarizing old messages)
- MCP tool extension
- Streaming / SSE
- Hook system (PreToolUse / PostToolUse)
- SubAgent / Teams
- Settings allowlist/denylist (glob patterns)
- Sandbox policy
