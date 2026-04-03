package com.lancode.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

public interface Tool {
    String name();
    String description();
    ObjectNode inputSchema();

    default ToolResult checkPermissions(Map<String, Object> params) {
        return null; // null = 允许；非null = 拒绝
    }

    ToolResult execute(Map<String, Object> params);
}
