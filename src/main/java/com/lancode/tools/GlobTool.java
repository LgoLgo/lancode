package com.lancode.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class GlobTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "glob"; }
    @Override public String description() {
        return "Find files matching a glob pattern. Returns matching file paths.";
    }
    @Override public ObjectNode inputSchema() {
        var schema = MAPPER.createObjectNode();
        var props = MAPPER.createObjectNode();
        var pat = MAPPER.createObjectNode(); pat.put("type","string"); pat.put("description","Glob pattern, e.g. *.java or **/*.xml");
        var dir = MAPPER.createObjectNode(); dir.put("type","string"); dir.put("description","Directory to search in (default: current directory).");
        props.set("pattern", pat); props.set("directory", dir);
        schema.put("type","object"); schema.set("properties", props);
        schema.set("required", MAPPER.createArrayNode().add("pattern"));
        return schema;
    }
    @Override public ToolResult execute(Map<String, Object> params) {
        String pattern = (String) params.get("pattern");
        String dirStr = (String) params.getOrDefault("directory", System.getProperty("user.dir"));
        Path dir = Path.of(dirStr).toAbsolutePath();
        if (!Files.isDirectory(dir)) return ToolResult.error("Error: not a directory: " + dir);
        try (var stream = Files.walk(dir)) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            List<String> matches = stream
                .filter(Files::isRegularFile)
                .filter(p -> matcher.matches(p.getFileName()))
                .map(p -> dir.relativize(p).toString())
                .sorted()
                .collect(Collectors.toList());
            if (matches.isEmpty()) return ToolResult.ok("(no matches)");
            return ToolResult.ok(String.join("\n", matches));
        } catch (Exception e) {
            return ToolResult.error("Error: " + e.getMessage());
        }
    }
}
