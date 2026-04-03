package com.lancode.tools;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolResultTest {
    @Test void ok_result_not_error() {
        var r = ToolResult.ok("hello");
        assertEquals("hello", r.output());
        assertFalse(r.isError());
    }
    @Test void error_result_is_error() {
        var r = ToolResult.error("fail");
        assertTrue(r.isError());
    }
}
