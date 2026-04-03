# lancode 测试报告

测试日期：2026-04-03
模型：LongCat-Flash-Lite
构建：`mvn package -q -DskipTests`

---

## 单元测试

```bash
mvn test -q
```

**结果：PASS**（无输出即全部通过）

---

## 功能测试

### 基础对话

```bash
echo "what is 1+1" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 模型返回 `1 + 1 equals 2.`

---

### 工具调用：read_file

```bash
echo "read the file README.md and tell me the first line of actual content" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 正确调用 `read_file`，返回 `**A minimal Claude Code agent loop in Java.**`

---

### 工具调用：bash

```bash
echo "run 'pwd' and tell me the result" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 正确调用 `bash`，返回当前目录路径

---

### 工具调用：glob

```bash
echo "list all java files in src/" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 正确调用 `glob`，列出全部 18 个 `.java` 文件

---

### 工具调用：grep

```bash
echo "find all occurrences of 'permissionMode' in src/" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 正确调用 `grep`，找到 6 处匹配并逐一说明

---

## 权限模式测试

### PLAN 模式：写操作被拒绝

```bash
echo "create a file /tmp/lancode_test.txt with content 'hello from lancode'" | java -jar target/lancode-0.1.0.jar --mode plan
```

**结果：PASS** — `write_file` 被 PermissionGate 拦截，返回 `Permission denied: 'write_file' is blocked in plan (read-only) mode.`

---

### ASK 模式：stdin 竞争 bug

```bash
printf "run the command whoami\ny\n" | java -jar target/lancode-0.1.0.jar --mode ask
```

**结果：FAIL（已知 bug）**

`PermissionGate.askUser()` 内部 `new Scanner(System.in)` 与 REPL 主循环的 Scanner 竞争同一个 stdin。`y` 被 REPL 的 Scanner 抢先消费，导致权限确认永远读不到用户输入，直接返回拒绝。

交互式终端手动输入时此 bug 不触发（REPL scanner 阻塞等待时 askUser 的 scanner 能正常读到），管道输入时必现。

---

## 危险命令拦截

### 模型层拒绝

```bash
echo "run 'rm -rf /'" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 模型自主拒绝，未调用工具

### BashTool.checkPermissions 拦截

```bash
echo "use the bash tool to run exactly this command: rm -rf /" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 工具层拦截，返回 `Blocked: matches dangerous pattern 'rm -rf /'`

---

## CLI 参数覆盖

### 覆盖 model

```bash
java -jar target/lancode-0.1.0.jar --model LongCat-Flash-Chat "say your model name if you know it"
```

**结果：PASS** — 模型切换为 LongCat-Flash-Chat，返回 Qwen 2.5 72B 架构说明

### 覆盖 max-turns=1

```bash
java -jar target/lancode-0.1.0.jar --max-turns 1 "list all java files then read Config.java"
```

**结果：PASS** — 只执行第一轮（glob），第二轮（read_file）因 maxTurns 截断未执行

---

## REPL 命令测试

```bash
printf "/tools\n/mode\n/mode plan\n/mode\n/help\n/quit\n" | java -jar target/lancode-0.1.0.jar
```

**结果：PASS**

- `/tools` — 列出全部 6 个工具
- `/mode` — 显示当前模式 `auto` 及用法
- `/mode plan` — 切换成功，返回 `Mode changed to: plan`
- `/mode`（再次）— 显示当前模式 `plan`
- `/help` — 显示 banner
- `/quit` — 正常退出

---

## 多轮上下文保持

```bash
printf "my name is Lance\nmy favorite color is blue\n...\nwhat is my name\nwhat is my favorite color\n/quit\n" \
  | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 7 轮对话后仍能正确回答姓名和颜色

---

## 上下文截断（maxContextMessages=4）

设置 `maxContextMessages: 4`，发送 5 条消息后询问第 1 条内容：

```bash
printf "my secret word is BANANA\ntell me a joke\n×3\nwhat was my secret word\n/quit\n" \
  | java -jar target/lancode-0.1.0.jar
```

**结果：PASS** — 截断后仍记得 BANANA（第 1 条消息被保留策略保护）

---

## CLAUDE.md 加载

```bash
mkdir -p /tmp/lancode_claudemd_test
echo "# Test Instructions\nAlways respond in Spanish." > /tmp/lancode_claudemd_test/CLAUDE.md
cd /tmp/lancode_claudemd_test && echo "say hello" | java -jar .../target/lancode-0.1.0.jar
```

**结果：FAIL（已知 bug）**

`SystemPrompt.build()` 从未被调用，`ConversationContext.setSystemPrompt()` 也从未被调用。系统提示始终为空字符串，CLAUDE.md 内容不生效。

---

## 汇总

| 测试项 | 结果 |
|--------|------|
| 单元测试 | ✅ PASS |
| 基础对话 | ✅ PASS |
| read_file 工具 | ✅ PASS |
| bash 工具 | ✅ PASS |
| glob 工具 | ✅ PASS |
| grep 工具 | ✅ PASS |
| PLAN 模式写拦截 | ✅ PASS |
| ASK 模式 stdin 竞争 | ❌ FAIL — Scanner 竞争，管道输入时权限确认失效 |
| 危险命令拦截（模型层） | ✅ PASS |
| 危险命令拦截（工具层） | ✅ PASS |
| CLI --model 覆盖 | ✅ PASS |
| CLI --max-turns 覆盖 | ✅ PASS |
| REPL 命令（/tools /mode /help /quit） | ✅ PASS |
| 多轮上下文保持 | ✅ PASS |
| 上下文截断保留第 1 条 | ✅ PASS |
| CLAUDE.md 加载 | ❌ FAIL — SystemPrompt 从未被组装和注入 |

**12 PASS / 2 FAIL**
