package com.lancode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
    public enum PermissionMode { ASK, AUTO, PLAN }

    public String model = "claude-opus-4-5";
    public String baseUrl = null;
    public String apiKey = null;    // 优先于环境变量 ANTHROPIC_API_KEY，发 x-api-key 头
    public String authToken = null; // 第三方兼容 API 用 Authorization: Bearer，与 apiKey 二选一
    public int maxTurns = 30;
    public int maxContextMessages = 100;
    public PermissionMode permissionMode = PermissionMode.AUTO;
    public Map<String, String> env = null;  // 注入额外环境变量（仅用于传给子进程，不影响 JVM）
    public List<String> allowedCommands = List.of(
        "ls", "cat", "head", "tail", "wc", "find", "grep",
        "git status", "git diff", "git log", "git branch",
        "python", "python3", "pip", "npm", "node",
        "echo", "pwd", "which", "env", "date"
    );
    public List<String> dangerousPatterns = List.of(
        "rm -rf /", "rm -rf ~", "sudo rm",
        "git push --force", "git reset --hard",
        "> /dev/sda", "mkfs", "dd if=",
        ":(){ :|:& };:"
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 从 ~/.lancode/settings.json 加载，文件不存在时返回默认 Config。 */
    public static Config load() {
        Path settings = Path.of(System.getProperty("user.home"), ".lancode", "settings.json");
        if (!Files.exists(settings)) return new Config();
        try {
            return MAPPER.readValue(settings.toFile(), Config.class);
        } catch (Exception e) {
            System.err.println("[warn] Failed to parse ~/.lancode/settings.json: " + e.getMessage());
            return new Config();
        }
    }
}
