package com.lancode.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class GrepTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String name() { return "grep"; }
    @Override public String description() {
        return "Search file contents for a regex pattern. Returns matching lines with file paths.";
    }
    @Override public ObjectNode inputSchema() {
        var schema = MAPPER.createObjectNode();
        var props = MAPPER.createObjectNode();
        var pat = MAPPER.createObjectNode(); pat.put("type","string"); pat.put("description","Regex pattern to search for.");
        var dir = MAPPER.createObjectNode(); dir.put("type","string"); dir.put("description","Directory to search in (default: current directory).");
        var glob = MAPPER.createObjectNode(); glob.put("type","string"); glob.put("description","Optional file glob filter, e.g. *.java");
        props.set("pattern", pat); props.set("directory", dir); props.set("include", glob);
        schema.put("type","object"); schema.set("properties", props);
        schema.set("required", MAPPER.createArrayNode().add("pattern"));
        return schema;
    }
    @Override public ToolResult execute(Map<String, Object> params) {
        String pattern = (String) params.get("pattern");
        String dirStr = (String) params.getOrDefault("directory", System.getProperty("user.dir"));
        String include = (String) params.getOrDefault("include", null);
        Path dir = Path.of(dirStr).toAbsolutePath();
        try {
            Pattern regex = Pattern.compile(pattern);
            PathMatcher fileMatcher = include != null
                ? FileSystems.getDefault().getPathMatcher("glob:" + include) : null;
            List<String> results = new ArrayList<>();
            try (var stream = Files.walk(dir)) {
                stream.filter(Files::isRegularFile)
                    .filter(p -> fileMatcher == null || fileMatcher.matches(p.getFileName()))
                    .forEach(p -> {
                        try {
                            List<String> lines = Files.readAllLines(p);
                            for (int i = 0; i < lines.size(); i++) {
                                if (regex.matcher(lines.get(i)).find()) {
                                    results.add(dir.relativize(p) + ":" + (i+1) + ": " + lines.get(i).strip());
                                }
                            }
                        } catch (Exception ignored) {}
                    });
            }
            if (results.isEmpty()) return ToolResult.ok("(no matches)");
            List<String> output = results.size() > 200 ? new ArrayList<>(results.subList(0, 200)) : results;
            if (results.size() > 200) output.add("... (truncated at 200 matches)");
            return ToolResult.ok(String.join("\n", output));
        } catch (PatternSyntaxException e) {
            return ToolResult.error("Error: invalid regex: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Error: " + e.getMessage());
        }
    }
}
