package com.lancode.tools;

import com.lancode.Config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

class BashToolTest {
    private final BashTool tool = new BashTool(new Config());

    @Test void blocks_dangerous_command() {
        var result = tool.checkPermissions(Map.of("command", "rm -rf /"));
        assertNotNull(result);
        assertTrue(result.isError());
    }

    @Test void executes_safe_command() {
        var result = tool.execute(Map.of("command", "echo hello"));
        assertFalse(result.isError());
        assertTrue(result.output().contains("hello"));
    }

    @Test void empty_command_returns_error() {
        var result = tool.execute(Map.of("command", ""));
        assertTrue(result.isError());
    }
}
