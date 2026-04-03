package com.lancode;

import com.lancode.tools.Tool;
import com.lancode.tools.ToolResult;
import java.util.*;

public class PermissionGate {
    private final Config config;
    private static final Set<String> WRITE_TOOLS = Set.of("bash", "write_file", "edit_file");

    public PermissionGate(Config config) { this.config = config; }

    /** Returns null if allowed, or a denied ToolResult. */
    public ToolResult check(Tool tool, Map<String, Object> params) {
        // Layer 1: tool self-check
        ToolResult denial = tool.checkPermissions(params);
        if (denial != null) return ToolResult.error("Permission denied: " + denial.output());

        // Layer 2: mode check
        if (config.permissionMode == Config.PermissionMode.PLAN && WRITE_TOOLS.contains(tool.name())) {
            return ToolResult.error("Permission denied: '" + tool.name() + "' is blocked in plan (read-only) mode.");
        }
        if (config.permissionMode == Config.PermissionMode.ASK && tool.name().equals("bash")) {
            String cmd = (String) params.getOrDefault("command", "");
            if (!isSafeCommand(cmd) && !askUser(tool.name(), params)) {
                return ToolResult.error("Permission denied: user rejected.");
            }
        }
        return null;
    }

    private boolean isSafeCommand(String cmd) {
        String lower = cmd.strip().toLowerCase();
        return config.allowedCommands.stream().anyMatch(lower::startsWith);
    }

    private boolean askUser(String toolName, Map<String, Object> params) {
        String detail = toolName.equals("bash") ? (String) params.getOrDefault("command", "") : "";
        String prompt = "\n[Permission] Allow '" + toolName + "'" + (detail.isEmpty() ? "" : ": " + detail) + "? [y/N] ";
        System.out.print(prompt);
        System.out.flush();
        try {
            var sc = new java.util.Scanner(System.in);
            String answer = sc.nextLine().strip().toLowerCase();
            return answer.equals("y") || answer.equals("yes");
        } catch (Exception e) { return false; }
    }
}
