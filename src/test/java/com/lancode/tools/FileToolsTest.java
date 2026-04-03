package com.lancode.tools;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FileToolsTest {
    private Path tempDir;

    @BeforeEach void setup() throws Exception {
        tempDir = Files.createTempDirectory("lancode-test");
    }

    @AfterEach void cleanup() throws Exception {
        try (var s = Files.walk(tempDir)) {
            s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    @Test void read_existing_file() throws Exception {
        Path f = tempDir.resolve("test.txt");
        Files.writeString(f, "hello world");
        var result = new FileReadTool().execute(Map.of("path", f.toString()));
        assertFalse(result.isError());
        assertTrue(result.output().contains("hello world"));
    }

    @Test void read_missing_file_returns_error() {
        var result = new FileReadTool().execute(Map.of("path", "/nonexistent/file.txt"));
        assertTrue(result.isError());
    }

    @Test void write_creates_file() throws Exception {
        Path f = tempDir.resolve("new.txt");
        var result = new FileWriteTool().execute(Map.of("path", f.toString(), "content", "data"));
        assertFalse(result.isError());
        assertEquals("data", Files.readString(f));
    }

    @Test void edit_replaces_unique_string() throws Exception {
        Path f = tempDir.resolve("edit.txt");
        Files.writeString(f, "foo bar baz");
        var result = new FileEditTool().execute(Map.of(
            "path", f.toString(), "old_string", "bar", "new_string", "QUX"));
        assertFalse(result.isError());
        assertEquals("foo QUX baz", Files.readString(f));
    }

    @Test void edit_fails_when_string_not_found() throws Exception {
        Path f = tempDir.resolve("edit2.txt");
        Files.writeString(f, "hello");
        var result = new FileEditTool().execute(Map.of(
            "path", f.toString(), "old_string", "xyz", "new_string", "abc"));
        assertTrue(result.isError());
    }

    @Test void glob_finds_files() throws Exception {
        Files.writeString(tempDir.resolve("a.java"), "");
        Files.writeString(tempDir.resolve("b.java"), "");
        Files.writeString(tempDir.resolve("c.txt"), "");
        var result = new GlobTool().execute(Map.of(
            "pattern", "*.java", "directory", tempDir.toString()));
        assertFalse(result.isError());
        assertTrue(result.output().contains("a.java"));
        assertTrue(result.output().contains("b.java"));
        assertFalse(result.output().contains("c.txt"));
    }

    @Test void grep_finds_content() throws Exception {
        Files.writeString(tempDir.resolve("src.java"), "public class Foo {}");
        var result = new GrepTool().execute(Map.of(
            "pattern", "class Foo", "directory", tempDir.toString()));
        assertFalse(result.isError());
        assertTrue(result.output().contains("src.java"));
    }
}
