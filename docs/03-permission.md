---
title: 第三章：权限模型
nav_order: 4
---

# 第三章：权限模型

## 是什么

权限模型是工具执行前的**双层检查机制**。它负责：

1. **第一层 — 工具自检** — 每个工具可实现 `Tool.checkPermissions()` 自定义检查逻辑（如 BashTool 检查危险命令）
2. **第二层 — 模式检查** — 根据全局 `permissionMode` 拒绝特定工具或操作（如 PLAN 模式禁止所有写操作）

对应文件：`PermissionGate.java`、`Config.java`（PermissionMode 枚举）

## 关键代码片段

**1. check() 方法 — 双层检查逻辑**

```java
public ToolResult check(Tool tool, Map<String, Object> params) {
    // 第一层：工具自检
    ToolResult denial = tool.checkPermissions(params);
    if (denial != null) return ToolResult.error("Permission denied: " + denial.output());

    // 第二层：模式检查
    if (config.permissionMode == Config.PermissionMode.PLAN && WRITE_TOOLS.contains(tool.name())) {
        return ToolResult.error("Permission denied: '" + tool.name() + "' is blocked in plan (read-only) mode.");
    }
    if (config.permissionMode == Config.PermissionMode.ASK && tool.name().equals("bash")) {
        String cmd = (String) params.getOrDefault("command", "");
        if (!isSafeCommand(cmd) && !askUser(tool.name(), params)) {
            return ToolResult.error("Permission denied: user rejected.");
        }
    }
    return null;
}
```

**约定**：返回 `null` 表示允许执行，返回 `ToolResult.error(...)` 表示拒绝。这个约定与 `Tool.checkPermissions()` 的返回值语义保持一致。

**2. PermissionMode 枚举 — 三种权限模式**

```java
public enum PermissionMode { ASK, AUTO, PLAN }
```

| 模式 | 行为 | 适用场景 |
|------|------|--------|
| `AUTO` | 全部自动允许，无任何拒绝 | 受信任的开发环境（本地开发、受控容器） |
| `ASK` | bash 工具需用户确认；白名单内的安全命令除外（如 `ls`、`cat` 等） | 交互式开发，需要人工审批高危操作 |
| `PLAN` | 只读模式，所有写操作（bash、write_file、edit_file）全部拒绝 | 规划/设计阶段，防止意外修改系统 |

## 小结

双层权限检查设计实现了**职责分离**：

- **工具层** — 工具负责自己的业务逻辑检查（如 BashTool 拦截危险命令）
- **系统层** — PermissionGate 负责全局策略执行（如 PLAN 模式禁止所有写操作）

这样即使添加新工具也无需修改 PermissionGate，只需在工具内实现 `checkPermissions()`；同时新的权限模式也无需修改工具，只需在 PermissionGate 添加检查逻辑。
