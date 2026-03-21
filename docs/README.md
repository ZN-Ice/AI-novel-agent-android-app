# AI小说安卓App 文档中心

> **文档索引**：本目录包含所有设计文档和任务记录，是研发过程的唯一信息源。

---

## 目录结构

```
docs/
├── design/              # 设计文档（固化归档）
│   ├── ui.md           # UI设计规范
│   ├── api.md          # AI接口对接文档
│   └── voice.md        # ffmpeg语音交互设计
├── task/                # 任务记录
│   └── task_records.json  # 全量任务记录
└── README.md            # 本文档
```

---

## 设计文档

### [ui.md](./design/ui.md) - UI设计规范

包含内容：
- 界面原型说明
- 页面流程图
- 组件库规范
- 交互设计说明

**适用场景**：前端界面开发、UI适配、交互实现

### [api.md](./design/api.md) - AI接口对接文档

包含内容：
- API接口规范
- 请求/响应格式
- 错误码定义
- 超时重试策略

**适用场景**：AI接口开发、接口联调、问题排查

### [voice.md](./design/voice.md) - ffmpeg语音交互设计

包含内容：
- 语音处理流程
- 指令白名单
- 权限申请流程
- 异常处理方案

**适用场景**：语音模块开发、指令扩展、问题排查

---

## 任务记录

### [task_records.json](./task/task_records.json)

**用途**：记录所有研发任务、迭代记录、Bug修复

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| task_id | String | 任务唯一ID，格式：T + 日期 + 序号 |
| task_type | String | 任务类型：功能开发/BUG修复/迭代优化 |
| task_desc | String | 任务描述 |
| module | String | 所属模块：voice/ui/ai/harness |
| executor | String | 执行者：Claude Code/人工 |
| status | String | 状态：待执行/进行中/已完成/驳回 |
| create_time | String | 创建时间 |
| finish_time | String | 完成时间（可空） |
| check_result | String | 校验结果：通过/不通过（可空） |
| remark | String | 备注信息 |

---

## 文档管理规则

### 固化文档（design/）

- **修改规则**：所有设计文档变更需人工审批
- **版本控制**：重大变更需记录版本号和变更日期
- **禁止事项**：禁止Claude Code自主修改设计文档

### 任务记录（task/）

- **写入规则**：所有任务必须通过Harness写入
- **格式约束**：严格遵守JSON格式，禁止修改字段结构
- **追溯性**：任务记录永久保留，支持历史查询

---

## 更新日志

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-03-21 | v1.0.0 | 初始化文档结构 |

---

## 相关链接

- [研发规则手册](../AGENTS.md)
- [Lint校验规则](../lint.xml)
- [项目构建配置](../build.gradle.kts)
