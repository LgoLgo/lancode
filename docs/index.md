---
title: 首页
nav_order: 1
---

# lancode 课程

**lancode** 是一个 ~900 行的 Java 实现，还原了 Claude Code 的核心 Agent Loop。

本课程面向已有 LLM 使用经验、想深入了解 tool-use / agent loop 内部机制的工程师。

## 学习路径

| 章节 | 内容 |
|------|------|
| [第一章：Agent Loop](01-agent-loop) | 核心循环逻辑 |
| [第二章：工具系统](02-tool-system) | Tool 接口、Registry、6 个内置工具 |
| [第三章：权限模型](03-permission) | PermissionGate 双层检查机制 |
| [第四章：对话上下文](04-context) | ConversationContext 消息管理与截断 |
| [第五章：系统提示词](05-system-prompt) | SystemPrompt 组装逻辑 |

## 源码

[github.com/LgoLgo/lancode](https://github.com/LgoLgo/lancode)

## 参考

特别感谢 [LinuxDO](https://linux.do/) 社区的支持与交流。
