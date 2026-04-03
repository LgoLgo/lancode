package com.lancode;

import com.lancode.tools.ToolRegistry;
import java.nio.file.*;

public class SystemPrompt {
    private static final String TEMPLATE = """
        You are lancode, a lightweight AI coding assistant that operates in the terminal.

        You have access to the following tools:
        %s

        ## Operating Rules
        1. Always read a file before editing it.
        2. Use tools to accomplish tasks -- don't just describe what to do.
        3. When running bash commands, prefer non-destructive read operations.
        4. For file edits, provide enough context in old_string to uniquely match.
        5. Be concise and direct in your responses.

        ## Current Permission Mode: %s
        %s

        %s""";

    private static final java.util.Map<String, String> MODE_DESC = java.util.Map.of(
        "ASK", "In ASK mode, potentially dangerous operations will require user confirmation.",
        "AUTO", "In AUTO mode, all operations are auto-approved (use with caution).",
        "PLAN", "In PLAN mode, only read-only operations are allowed. Write operations are blocked."
    );

    public static String build(ToolRegistry registry, Config.PermissionMode mode, String projectDir) {
        String toolList = registry.allTools().stream()
            .map(t -> "- **" + t.name() + "**: " + t.description())
            .reduce("", (a, b) -> a + "\n" + b).strip();
        String instructions = loadClaudeMd(projectDir);
        String projectSection = instructions.isEmpty() ? "" :
            "## Project Instructions (from CLAUDE.md)\n\n" + instructions;
        return TEMPLATE.formatted(toolList, mode.name(), MODE_DESC.get(mode.name()), projectSection).strip();
    }

    private static String loadClaudeMd(String dir) {
        try {
            Path p = Path.of(dir != null ? dir : System.getProperty("user.dir")).resolve("CLAUDE.md");
            return Files.exists(p) ? Files.readString(p).strip() : "";
        } catch (Exception e) { return ""; }
    }
}
