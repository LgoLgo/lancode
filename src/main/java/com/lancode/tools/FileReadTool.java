package com.lancode.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.Map;

public class FileReadTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "read_file"; }
    @Override public String description() {
        return "Read the contents of a file. Returns the file content as text.";
    }
    @Override public ObjectNode inputSchema() {
        var schema = MAPPER.createObjectNode();
        var props = MAPPER.createObjectNode();
        var path = MAPPER.createObjectNode();
        path.put("type", "string"); path.put("description", "Path to the file to read.");
        props.set("path", path);
        schema.put("type", "object"); schema.set("properties", props);
        schema.set("required", MAPPER.createArrayNode().add("path"));
        return schema;
    }
    @Override public ToolResult execute(Map<String, Object> params) {
        var p = Path.of((String) params.get("path")).toAbsolutePath();
        if (!Files.exists(p)) return ToolResult.error("Error: file not found: " + p);
        try {
            return ToolResult.ok(Files.readString(p));
        } catch (Exception e) {
            return ToolResult.error("Error reading file: " + e.getMessage());
        }
    }
}
