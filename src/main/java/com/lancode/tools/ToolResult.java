package com.lancode.tools;

public record ToolResult(String output, boolean isError) {
    public static ToolResult ok(String output) {
        return new ToolResult(output, false);
    }
    public static ToolResult error(String output) {
        return new ToolResult(output, true);
    }
}
