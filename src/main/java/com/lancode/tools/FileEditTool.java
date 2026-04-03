package com.lancode.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.Map;

public class FileEditTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "edit_file"; }
    @Override public String description() {
        return "Edit a file by replacing an exact string. old_string must be unique in the file.";
    }
    @Override public ObjectNode inputSchema() {
        var schema = MAPPER.createObjectNode();
        var props = MAPPER.createObjectNode();
        for (var e : Map.of(
                "path", "Path to the file.",
                "old_string", "Exact text to find (must be unique).",
                "new_string", "Text to replace it with.").entrySet()) {
            var n = MAPPER.createObjectNode(); n.put("type","string"); n.put("description", e.getValue());
            props.set(e.getKey(), n);
        }
        schema.put("type","object"); schema.set("properties", props);
        schema.set("required", MAPPER.createArrayNode().add("path").add("old_string").add("new_string"));
        return schema;
    }
    @Override public ToolResult execute(Map<String, Object> params) {
        var p = Path.of((String) params.get("path")).toAbsolutePath();
        String oldStr = (String) params.get("old_string");
        String newStr = (String) params.getOrDefault("new_string", "");
        if (!Files.exists(p)) return ToolResult.error("Error: file not found: " + p);
        if (oldStr == null || oldStr.isEmpty()) return ToolResult.error("Error: old_string must not be empty");
        try {
            String content = Files.readString(p);
            int count = 0, idx = 0;
            while ((idx = content.indexOf(oldStr, idx)) != -1) { count++; idx += oldStr.length(); }
            if (count == 0) return ToolResult.error("Error: old_string not found in file");
            if (count > 1) return ToolResult.error("Error: old_string found " + count + " times -- must be unique");
            Files.writeString(p, content.replace(oldStr, newStr));
            return ToolResult.ok("Replaced 1 occurrence in " + p);
        } catch (Exception e) {
            return ToolResult.error("Error: " + e.getMessage());
        }
    }
}
