package com.lancode;

import java.util.List;

public class Config {
    public enum PermissionMode { ASK, AUTO, PLAN }

    public String model = "claude-sonnet-4-5-20251001";
    public int maxTurns = 30;
    public int maxContextMessages = 100;
    public PermissionMode permissionMode = PermissionMode.ASK;
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
}
