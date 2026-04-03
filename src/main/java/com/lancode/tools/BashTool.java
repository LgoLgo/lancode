package com.lancode.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lancode.Config;
import java.util.Map;

public class BashTool implements Tool {
    private final Config config;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public BashTool(Config config) { this.config = config; }

    @Override public String name() { return "bash"; }

    @Override public String description() {
        return "Execute a bash command. Use for running scripts, git operations, and shell tasks. Commands run in the current working directory.";
    }

    @Override public ObjectNode inputSchema() {
        var schema = MAPPER.createObjectNode();
        var props = MAPPER.createObjectNode();
        var cmd = MAPPER.createObjectNode();
        cmd.put("type", "string");
        cmd.put("description", "The bash command to execute.");
        props.set("command", cmd);
        schema.put("type", "object");
        schema.set("properties", props);
        schema.set("required", MAPPER.createArrayNode().add("command"));
        return schema;
    }

    @Override public ToolResult checkPermissions(Map<String, Object> params) {
        String cmd = (String) params.getOrDefault("command", "");
        for (String pattern : config.dangerousPatterns) {
            if (cmd.contains(pattern)) {
                return ToolResult.error("Blocked: matches dangerous pattern '" + pattern + "'");
            }
        }
        return null;
    }

    @Override public ToolResult execute(Map<String, Object> params) {
        String command = (String) params.getOrDefault("command", "");
        if (command.isBlank()) return ToolResult.error("Error: empty command");
        try {
            Process process = new ProcessBuilder("bash", "-c", command)
                .redirectErrorStream(false)
                .start();
            boolean finished = process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.error("Error: command timed out after 120s");
            }
            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());
            StringBuilder out = new StringBuilder();
            if (!stdout.isEmpty()) out.append(stdout);
            if (!stderr.isEmpty()) out.append("STDERR:\n").append(stderr);
            String output = out.isEmpty() ? "(no output)" : out.toString();
            if (output.length() > 50_000) output = output.substring(0, 50_000) + "\n... (truncated)";
            return new ToolResult(output, process.exitValue() != 0);
        } catch (Exception e) {
            return ToolResult.error("Error: " + e.getMessage());
        }
    }
}
