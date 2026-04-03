package com.lancode.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.Map;

public class FileWriteTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "write_file"; }
    @Override public String description() {
        return "Write content to a file, creating it if it doesn't exist.";
    }
    @Override public ObjectNode inputSchema() {
        var schema = MAPPER.createObjectNode();
        var props = MAPPER.createObjectNode();
        var path = MAPPER.createObjectNode(); path.put("type","string"); path.put("description","Path to write.");
        var content = MAPPER.createObjectNode(); content.put("type","string"); content.put("description","Content to write.");
        props.set("path", path); props.set("content", content);
        schema.put("type","object"); schema.set("properties", props);
        schema.set("required", MAPPER.createArrayNode().add("path").add("content"));
        return schema;
    }
    @Override public ToolResult execute(Map<String, Object> params) {
        var p = Path.of((String) params.get("path")).toAbsolutePath();
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, (String) params.getOrDefault("content", ""));
            return ToolResult.ok("Wrote " + Files.size(p) + " chars to " + p);
        } catch (Exception e) {
            return ToolResult.error("Error writing file: " + e.getMessage());
        }
    }
}
