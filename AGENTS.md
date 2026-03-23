# AI小说安卓App 研发Agent手册

> **核心定位**：本文档是AI小说安卓App研发的唯一准则，所有开发行为必须遵循本手册规范。
>
> **适用对象**：前端界面Agent、语音交互Agent、AI接口Agent、校验质检Agent、Claude Code协同开发

---

## 一、项目概述

| 项目属性 | 规范值 |
|---------|--------|
| 包名 | `com.novelapp.aiagent` |
| 最低SDK | API 24 (Android 7.0) |
| 目标SDK | API 34 (Android 14) |
| 开发语言 | Kotlin |
| 架构模式 | MVVM + Repository |
| 数据库 | Room (SQLite) |
| 语音处理 | ffmpeg |

---

## 二、安卓前端界面规范

### 2.1 UI组件标准

```kotlin
// 命名规范：功能_类型_描述
// 示例：
// - novel_list_item.xml        小说列表项
// - novel_create_dialog.xml    新建小说弹窗
// - editor_fragment.xml        创作空间页
// - voice_indicator_view.xml   语音指示器
```

**组件分类**：
| 组件类型 | 命名前缀 | 示例 |
|---------|---------|------|
| Activity | 页面名 + Activity | `HomeActivity` |
| Fragment | 页面名 + Fragment | `EditorFragment` |
| Dialog | 功能名 + Dialog | `CreateNovelDialog` |
| Adapter | 功能名 + Adapter | `NovelListAdapter` |
| ViewHolder | 功能名 + ViewHolder | `NovelViewHolder` |
| 自定义View | 功能名 + View | `VoiceIndicatorView` |

### 2.2 页面路由规则

```
标准流程（禁止乱跳）：
┌─────────────┐    新建     ┌─────────────┐    创建成功    ┌─────────────┐
│   首页      │ ─────────> │  新建小说页  │ ────────────> │  创作空间   │
│ (小说列表)  │            │             │               │  (编辑器)   │
└─────────────┘            └─────────────┘               └─────────────┘
       │                                                        │
       │ 删除                                                   │
       ▼                                                        │
┌─────────────┐                                                │
│  删除确认框  │ <─────────────────────────────────────────────┘
└─────────────┘
```

**路由约束**：
- 首页只能跳转到新建页或删除确认框
- 新建页只能跳转到创作空间或返回首页
- 创作空间只能返回首页或触发删除确认
- 禁止跨层级跳转

### 2.3 布局约束

```xml
<!-- 布局文件命名：类型_模块_描述.xml -->
<!-- activity_home.xml / fragment_editor.xml / dialog_delete.xml -->

<!-- 根布局必须使用 ConstraintLayout 或 RecyclerView -->
<!-- 禁止嵌套超过3层 LinearLayout -->

<!-- 示例规范 -->
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ui.home.HomeActivity">

    <!-- 使用 tools: 命名空间预览 -->
    <!-- 所有文本必须引用 strings.xml -->
</androidx.constraintlayout.widget.ConstraintLayout>
```

### 2.4 配色/字体统一

```xml
<!-- colors.xml -->
<resources>
    <!-- 主色调 -->
    <color name="primary">#2196F3</color>
    <color name="primary_dark">#1976D2</color>
    <color name="primary_light">#BBDEFB</color>

    <!-- 强调色 -->
    <color name="accent">#FF5722</color>

    <!-- 背景色 -->
    <color name="background">#FAFAFA</color>
    <color name="surface">#FFFFFF</color>

    <!-- 文本色 -->
    <color name="text_primary">#212121</color>
    <color name="text_secondary">#757575</color>

    <!-- 状态色 -->
    <color name="success">#4CAF50</color>
    <color name="warning">#FFC107</color>
    <color name="error">#F44336</color>
</resources>

<!-- dimens.xml -->
<resources>
    <!-- 间距标准 -->
    <dimen name="spacing_xs">4dp</dimen>
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">16dp</dimen>
    <dimen name="spacing_lg">24dp</dimen>
    <dimen name="spacing_xl">32dp</dimen>

    <!-- 字体大小 -->
    <dimen name="text_h1">24sp</dimen>
    <dimen name="text_h2">20sp</dimen>
    <dimen name="text_body">16sp</dimen>
    <dimen name="text_caption">12sp</dimen>

    <!-- 圆角 -->
    <dimen name="corner_sm">4dp</dimen>
    <dimen name="corner_md">8dp</dimen>
    <dimen name="corner_lg">16dp</dimen>
</resources>
```

### 2.5 屏幕适配规则

```kotlin
// 使用 dp/sp 单位，禁止 px
// 适配方案：smallestWidth 限定符

// values/dimens.xml        (默认)
// values-sw320dp/dimens.xml (小屏手机)
// values-sw600dp/dimens.xml (平板)
// values-land/dimens.xml    (横屏)

// 布局中使用 ConstraintLayout 百分比
// app:layout_constraintWidth_percent="0.5"
```

---

## 三、语音模块开发规范

### 3.1 ffmpeg接入标准

```kotlin
// FFmpegWrapper.kt
package com.novelapp.aiagent.voice

/**
 * ffmpeg命令封装
 * 所有语音处理必须通过此类调用
 */
class FFmpegWrapper private constructor() {

    companion object {
        @Volatile
        private var instance: FFmpegWrapper? = null

        fun getInstance(): FFmpegWrapper {
            return instance ?: synchronized(this) {
                instance ?: FFmpegWrapper().also { instance = it }
            }
        }
    }

    /**
     * 录音转码为PCM
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param callback 回调结果
     */
    fun convertToPcm(inputPath: String, outputPath: String, callback: FFmpegCallback) {
        // ffmpeg -i input -f s16le -acodec pcm_s16le output
    }

    /**
     * 音频降噪处理
     */
    fun denoise(inputPath: String, outputPath: String, callback: FFmpegCallback) {
        // ffmpeg -i input -af "highpass=f=200,lowpass=f=3000" output
    }

    interface FFmpegCallback {
        fun onSuccess(outputPath: String)
        fun onError(errorCode: Int, message: String)
        fun onProgress(progress: Int)
    }
}
```

### 3.2 语音指令映射规则

```kotlin
// VoiceCommandParser.kt
package com.novelapp.aiagent.voice

/**
 * 语音指令解析器
 * 支持的指令白名单
 */
enum class VoiceCommand(val keywords: List<String>, val action: String) {
    // 新建相关
    CREATE_NOVEL(listOf("新建小说", "创建小说", "开始写作"), "ACTION_CREATE"),

    // 删除相关
    DELETE_NOVEL(listOf("删除小说", "移除小说"), "ACTION_DELETE"),
    CONFIRM_DELETE(listOf("确认删除", "是的删除"), "ACTION_CONFIRM_DELETE"),
    CANCEL_DELETE(listOf("取消", "不删除"), "ACTION_CANCEL_DELETE"),

    // 创作相关
    ENTER_EDITOR(listOf("进入创作", "开始创作", "打开编辑"), "ACTION_ENTER_EDITOR"),
    START_WRITING(listOf("写一段", "继续写", "生成内容"), "ACTION_START_WRITING"),
    PAUSE_WRITING(listOf("暂停", "停一下"), "ACTION_PAUSE"),
    UNDO(listOf("撤销", "回退"), "ACTION_UNDO"),

    // 导航相关
    GO_HOME(listOf("返回首页", "回到主页"), "ACTION_GO_HOME");

    companion object {
        fun parse(text: String): VoiceCommand? {
            return values().find { cmd ->
                cmd.keywords.any { keyword -> text.contains(keyword) }
            }
        }
    }
}
```

### 3.3 权限申请流程

```kotlin
// 权限申请规范
// 1. 仅申请必要权限
// 2. 申请前必须说明用途
// 3. 拒绝后提供手动入口

// 必要权限列表
val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.RECORD_AUDIO,      // 语音识别
    Manifest.permission.INTERNET,          // AI接口
    Manifest.permission.WRITE_EXTERNAL_STORAGE  // 草稿缓存 (API < 29)
)

// 权限申请流程
// 1. 检查权限状态
// 2. 未授权 -> 显示说明弹窗
// 3. 用户同意 -> 申请权限
// 4. 拒绝 -> 引导到设置页
// 5. 永久拒绝 -> 显示降级方案（手动操作）
```

### 3.4 异常容错逻辑

```kotlin
// VoiceStateManager.kt
sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class Success(val command: VoiceCommand) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

// 异常处理规范
// 1. 录音失败 -> 提示检查麦克风权限
// 2. 识别超时(5s) -> 自动重试一次，失败则降级手动
// 3. 网络异常 -> 使用离线指令缓存
// 4. 无匹配指令 -> 提示"请说出有效指令"
```

---

## 四、AI创作模块规范

### 4.1 接口请求/响应格式

```kotlin
// AIRequest.kt
data class AIRequest(
    val novelId: String,           // 小说ID
    val chapterId: String,         // 章节ID
    val context: String,           // 上下文（前文摘要）
    val instruction: String?,      // 用户指令（可选）
    val maxTokens: Int = 2000,     // 最大生成长度
    val temperature: Float = 0.8f  // 创造性参数
)

// AIResponse.kt
data class AIResponse(
    val code: Int,                 // 状态码：0成功，其他失败
    val message: String,           // 状态信息
    val data: AIContent?           // 生成内容
)

data class AIContent(
    val text: String,              // 生成的文本
    val tokens: Int,               // 实际token数
    val finishReason: String       // 结束原因：stop/length/error
)
```

### 4.2 上下文存储规则

```kotlin
// ContextManager.kt
/**
 * 上下文管理器
 * 负责拼接历史章节、人设、大纲
 */
class ContextManager {

    // 上下文结构
    data class NovelContext(
        val novelId: String,
        val title: String,           // 小说标题
        val genre: String,           // 类型
        val outline: String,         // 大纲
        val characters: List<Character>,  // 角色设定
        val recentChapters: List<String>, // 最近3章摘要
        val currentChapter: String   // 当前章节内容
    )

    // 上下文拼接规则
    // 1. 总长度不超过 4000 tokens
    // 2. 优先级：人设 > 大纲 > 最近章节 > 历史章节
    // 3. 超出时自动截断低优先级内容
    fun buildContext(novelId: String, chapterId: String): NovelContext
}
```

### 4.3 断点续创逻辑

```kotlin
// AIRepository.kt
/**
 * AI请求仓库
 * 支持断点续创、超时重试
 */
class AIRepository {

    // 断点续创规则
    // 1. 每生成500字自动保存草稿
    // 2. 中断后从最后一个检查点继续
    // 3. 保留最近3个检查点

    // 超时熔断
    // 1. 单次请求超时：30秒
    // 2. 总生成超时：5分钟
    // 3. 超时后自动取消，提示用户重试

    suspend fun generateContent(request: AIRequest): Result<AIResponse>
    suspend fun cancelGeneration(requestId: String)
    suspend fun resumeGeneration(checkpointId: String): Result<AIResponse>
}
```

### 4.4 生成超时/失败兜底

```kotlin
// 失败处理策略
enum class FailureStrategy {
    RETRY,           // 重试（最多3次）
    FALLBACK_LOCAL,  // 使用本地模板
    SHOW_ERROR,      // 显示错误提示
    SAVE_DRAFT       // 保存草稿，稍后继续
}

// 错误码映射
val ERROR_STRATEGIES = mapOf(
    401 to FailureStrategy.SHOW_ERROR,      // 认证失败
    429 to FailureStrategy.RETRY,           // 限流
    500 to FailureStrategy.RETRY,           // 服务器错误
    504 to FailureStrategy.SAVE_DRAFT,      // 超时
    -1 to FailureStrategy.FALLBACK_LOCAL    // 网络异常
)
```

---

## 五、核心业务流程规范

### 5.1 新建小说流程

```
触发方式：语音("新建小说") / 手动点击(+按钮)

流程步骤：
1. 显示新建弹窗
2. 输入小说名称（语音/键盘）
3. 校验名称（非空、不超过50字、不重复）
4. 创建数据库记录 + 本地文件夹
5. 跳转到创作空间

数据变更：
- novels表：插入新记录
- 本地存储：创建 novels/{novelId}/ 目录
```

### 5.2 删除小说流程

```
触发方式：语音("删除小说") / 手动滑动删除

流程步骤：
1. 显示二次确认弹窗
2. 用户确认删除
3. 执行软删除（标记is_deleted=true）
4. 更新首页列表
5. 7天后自动清理本地文件

数据变更：
- novels表：UPDATE is_deleted=true, deleted_at=now()
- 定时任务：清理超过7天的已删除数据
```

### 5.3 创作空间流程

```
进入方式：点击小说卡片 / 语音("进入创作")

流程步骤：
1. 加载小说数据（标题、章节列表、人设）
2. 显示最近编辑的章节
3. 初始化AI上下文
4. 监听语音指令

状态管理：
- ViewModel维护：当前章节、编辑内容、AI状态
- 自动保存：每30秒或用户离开时
```

---

## 六、安卓工程规范

### 6.1 包名结构

```
com.novelapp.aiagent/
├── base/           # 基类封装
│   ├── BaseActivity.kt
│   ├── BaseFragment.kt
│   └── BaseViewModel.kt
├── ui/             # 界面层
│   ├── home/
│   ├── create/
│   ├── delete/
│   └── editor/
├── viewmodel/      # 状态管理
├── model/          # 数据实体
├── data/           # 数据层
│   ├── local/      # Room数据库
│   └── repository/ # 数据仓库
├── voice/          # 语音模块
├── ai/             # AI接口模块
├── utils/          # 工具类
└── harness/        # Harness管控
```

### 6.2 代码分层 (MVVM)

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer                            │
│  Activity / Fragment / Dialog / Adapter                 │
│  - 只负责视图渲染和用户交互                              │
│  - 通过ViewModel观察数据变化                            │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   ViewModel Layer                       │
│  HomeViewModel / EditorViewModel                        │
│  - 持有UI状态（StateFlow/LiveData）                     │
│  - 处理用户意图（Intent/Action）                        │
│  - 调用Repository获取数据                               │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   Repository Layer                      │
│  NovelRepository / AIRepository                         │
│  - 统一数据访问接口                                     │
│  - 协调本地/远程数据源                                  │
│  - 缓存策略                                             │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                           │
│  Room Database / API Client / Voice SDK                 │
│  - 具体数据存储和获取实现                               │
└─────────────────────────────────────────────────────────┘
```

### 6.3 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | 大驼峰 | `NovelListAdapter` |
| 方法名 | 小驼峰 | `createNovel()` |
| 变量名 | 小驼峰 | `novelList` |
| 常量 | 全大写下划线 | `MAX_RETRY_COUNT` |
| 布局文件 | 小写下划线 | `activity_home.xml` |
| 资源ID | 小写下划线 | `@+id/novel_title` |
| 字符串 | 小写下划线 | `R.string.create_novel` |

### 6.4 Git提交规范

```
格式：<type>: <description>

类型：
- feat: 新功能
- fix: Bug修复
- refactor: 重构
- docs: 文档更新
- style: 代码格式
- test: 测试相关
- chore: 构建/工具

示例：
feat: 新增语音新建小说功能
fix: 修复创作空间自动保存失效问题
refactor: 重构AI接口超时处理逻辑
docs: 更新语音指令白名单
```

### 6.5 版本号规则

```
格式：MAJOR.MINOR.PATCH

- MAJOR: 重大版本更新（不兼容的API变更）
- MINOR: 功能更新（向后兼容）
- PATCH: Bug修复（向后兼容）

示例：
1.0.0  首次发布
1.1.0  新增语音功能
1.1.1  修复语音识别bug
```

---

## 七、研发Agent职责拆分

### 7.1 前端界面Agent

**职责范围**：
- XML布局文件编写
- UI组件实现
- 页面跳转逻辑
- 状态绑定（DataBinding/ViewBinding）

**产出标准**：
- 布局符合2.3约束
- 控件命名符合2.1规范
- 路由遵循2.2规则
- 适配符合2.5规则

### 7.2 语音交互Agent

**职责范围**：
- ffmpeg SDK接入
- 语音指令解析
- 权限申请处理
- 语音状态同步

**产出标准**：
- 指令映射符合3.2规范
- 权限流程符合3.3规范
- 异常处理符合3.4规范

### 7.3 AI接口Agent

**职责范围**：
- HTTP客户端封装
- 请求/响应处理
- 超时重试机制
- 上下文管理

**产出标准**：
- 接口格式符合4.1规范
- 上下文规则符合4.2规范
- 断点续创符合4.3规范
- 失败处理符合4.4规范

### 7.4 校验质检Agent

**职责范围**：
- 代码规范检查（Lint）
- UI适配校验
- 功能闭环测试
- Bug拦截

**产出标准**：
- 执行 `./gradlew lint` 无error
- 测试覆盖率达到80%+
- 核心流程E2E测试通过

---

## 八、安全护栏

### 8.1 代码安全约束

```kotlin
// 禁止事项
// ❌ 硬编码API密钥
// ❌ 明文存储用户数据
// ❌ UI线程做耗时操作

// 正确做法
// ✅ API密钥存放在 BuildConfig 或 EncryptedSharedPreferences
// ✅ 敏感数据加密存储
// ✅ 耗时操作使用 Coroutines 或 WorkManager
```

### 8.2 资源管控

```
限制规则：
- 单张图片 < 500KB
- 单个音频文件 < 5MB
- 布局嵌套层级 < 4层
- 内存占用峰值 < 200MB

自动回收：
- 语音识别结束后释放录音资源
- AI生成完成后清理临时文件
- 页面销毁时取消未完成的请求
```

### 8.3 权限最小化

```xml
<!-- 仅申请必要权限 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<!-- API 29+ 不需要此权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

---

## 九、Harness管控红线

### 9.1 禁止事项

- ❌ 修改 docs/ 目录下的设计文档（需人工审批）
- ❌ 修改 task_records.json 字段结构
- ❌ 跳过 Pre-commit 校验
- ❌ 引入未授权的第三方依赖
- ❌ 在 harness/ 目录外编写管控代码

### 9.2 必须遵守

- ✅ 所有任务必须写入 task_records.json
- ✅ 代码必须通过 Lint 检查
- ✅ 核心功能必须有单元测试
- ✅ 每次提交必须符合6.4规范

### 9.3 核心开发流程（PR模式）

> **重要**：所有代码变更必须通过PR（Pull Request）方式合入main分支，禁止直接推送到main。
>
> **关键要求**：推送前必须完成本地构建验证，避免CI资源浪费。

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Harness核心开发流程（PR模式）                     │
└─────────────────────────────────────────────────────────────────────┘

     ┌──────────────┐
     │  代码修改完成  │
     └──────┬───────┘
            │
            ▼
     ┌──────────────────────────────┐
     │  Step 1: 创建特性分支        │
     │  git checkout -b feat/xxx    │
     └──────────────┬───────────────┘
                    │
                    ▼
     ┌──────────────────────────────┐      失败      ┌──────────────┐
     │  Step 2: 本地检查            │ ─────────────> │  修复问题    │
     │  bash scripts/check.sh       │                │              │
     └──────────────┬───────────────┘                └──────┬───────┘
                    │ 通过                                  │
                    ▼                                       │
     ┌──────────────────────────────┐                       │
     │  Step 3: 提交到分支          │                       │
     │  git add . && git commit     │ <─────────────────────┘
     └──────────────┬───────────────┘
                    │
                    ▼
     ┌──────────────────────────────┐      失败      ┌──────────────┐
     │  Step 4: 本地构建验证 ⏱️      │ ─────────────> │  修复问题    │
     │  ./gradlew assembleDebug     │                │  重新commit  │
     │  (预计5-10分钟)              │                └──────┬───────┘
     └──────────────┬───────────────┘                       │
                    │ 通过 ←────────────────────────────────┘
                    ▼
     ┌──────────────────────────────┐
     │  Step 5: 推送特性分支        │
     │  git push -u origin feat/xxx │
     └──────────────┬───────────────┘
                    │
                    ▼
     ┌──────────────────────────────┐
     │  Step 6: 创建Pull Request    │
     │  gh pr create --base main    │
     └──────────────┬───────────────┘
                    │
                    ▼
     ┌──────────────────────────────┐
     │  Step 7: 等待门禁检查        │
     │  - 构建测试 ✓                │
     │  - Lint检查 ✓                │
     │  - 单元测试 ✓                │
     │  - 安全检查 ✓                │
     │  - AI代码审查 🤖              │
     └──────────────┬───────────────┘
                    │ 全部通过
                    ▼
     ┌──────────────┐
     │  Step 8: 合并到main          │
     │  Squash and Merge            │
     └──────────────┘
```

> **⏱️ 构建超时说明**：
> - 本地构建通常需要5-10分钟（首次构建更长）
> - 如遇网络问题导致下载超时，可配置国内镜像源（见settings.gradle.kts）
> - 构建日志输出到临时文件便于排查：`./gradlew assembleDebug 2>&1 | tee /tmp/build.log`
> - 后台运行避免中断：`nohup ./gradlew assembleDebug > /tmp/build.log 2>&1 &`

#### 9.3.1 分支命名规范

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| 新功能 | `feat/描述` | `feat/add-voice-input` |
| Bug修复 | `fix/描述` | `fix/login-crash` |
| 重构 | `refactor/描述` | `refactor/ai-module` |
| 文档 | `docs/描述` | `docs/update-agents` |
| 测试 | `test/描述` | `test/viewmodel-coverage` |
| 发布 | `release/版本号` | `release/1.0.0` |

#### 9.3.2 执行命令顺序

```bash
# ========== Step 1: 创建特性分支 ==========
# 从main创建新分支
git checkout main
git pull origin main
git checkout -b feat/your-feature-name

# ========== Step 2: 本地检查 ==========
# 运行Harness检查脚本
bash scripts/check.sh

# 或者手动执行
./gradlew assembleDebug lint test

# ========== Step 3: 提交代码 ==========
git add .
git commit -m "feat: 描述本次修改内容"

# ========== Step 4: 推送分支 ==========
git push -u origin feat/your-feature-name

# ========== Step 5: 创建PR ==========
# 方式1: 使用gh CLI
gh pr create --base main --head feat/your-feature-name \
  --title "feat: 描述" \
  --body "## 变更内容\n- xxx\n\n## 测试计划\n- [ ] 单元测试\n- [ ] 本地验证"

# 方式2: 在GitHub网页上创建

# ========== Step 6: 等待门禁 ==========
# 自动触发，无需手动操作
# 查看门禁状态: gh pr checks

# ========== Step 7: 合并 ==========
# 门禁通过后，使用Squash and Merge
gh pr merge --squash --delete-branch
```

#### 9.3.3 一键执行脚本

```bash
# 文件：scripts/create_pr.sh

#!/bin/bash
set -e

# 配置
BRANCH_TYPE=${1:-feat}
DESCRIPTION=${2:-"update"}
BRANCH_NAME="${BRANCH_TYPE}/${DESCRIPTION}"

echo "===== 开始PR流程 ====="

# Step 1: 创建分支
echo "[1/5] 创建特性分支: $BRANCH_NAME"
git checkout main && git pull origin main
git checkout -b "$BRANCH_NAME"

# Step 2: 本地检查
echo "[2/5] 运行本地检查..."
bash scripts/check.sh

# Step 3: 提交
echo "[3/5] 提交代码..."
git add .
git commit -m "${BRANCH_TYPE}: ${DESCRIPTION}"

# Step 4: 推送
echo "[4/5] 推送分支..."
git push -u origin "$BRANCH_NAME"

# Step 5: 创建PR
echo "[5/5] 创建Pull Request..."
gh pr create --base main --head "$BRANCH_NAME" \
  --title "${BRANCH_TYPE}: ${DESCRIPTION}" \
  --body "## 变更内容\n- ${DESCRIPTION}\n\n## 检查清单\n- [x] 本地构建通过\n- [x] Lint检查通过\n- [x] 单元测试通过"

echo "===== PR创建成功！ ====="
echo "等待门禁检查完成后即可合并"
```

#### 9.3.4 测试报告位置

| 测试类型 | 报告路径 |
|---------|---------|
| Lint报告 | `app/build/reports/lint-results.html` |
| 单元测试报告 | `app/build/reports/tests/test/index.html` |
| UI测试报告 | `app/build/reports/androidTests/connected/index.html` |

#### 9.3.5 豁免条件

以下情况可以跳过部分检查（需在commit message中说明）：

| 豁免类型 | 条件 | 说明示例 |
|---------|------|---------|
| 仅文档修改 | 只修改 `.md` 文件 | `docs: 更新README` |
| 仅资源修改 | 只修改 `res/` 下的非代码文件 | `style: 更新图标资源` |
| 紧急修复 | 生产环境紧急问题 | `fix(hotfix): 修复崩溃问题 [skip-test]` |

> **注意**：豁免仅用于特殊情况，常规开发必须完成全部检查。

#### 9.3.6 PR合入规范

| 规则 | 说明 |
|------|------|
| 门禁全通过 | 必须，不可跳过 |
| 至少1个Review | 推荐但非强制 |
| Squash Merge | 保持main历史整洁 |
| 自动删除分支 | 合并后自动删除特性分支 |
| 禁止Force Push | 保护代码历史 |

---

## 9.4 GitHub Actions门禁规范

### 9.4.1 门禁触发条件

| 事件 | 触发分支 | 说明 |
|------|---------|------|
| `push` | main, develop, release/** | 推送代码时触发 |
| `pull_request` | main, develop | PR创建/更新时触发 |

### 9.4.2 门禁检查项

```
┌─────────────────────────────────────────────────────────────────────┐
│                    GitHub Actions 门禁流程                            │
└─────────────────────────────────────────────────────────────────────┘

     ┌──────────────┐
     │  push / PR   │
     └──────┬───────┘
            │
            ▼
     ┌──────────────┐
     │  构建测试    │ ──> 失败 → ❌ 阻断
     │  assemble    │
     └──────┬───────┘
            │ 通过
            ▼
     ┌──────────────┐
     │  Lint检查    │ ──> 失败 → ❌ 阻断
     │  lint        │
     └──────┬───────┘
            │ 通过
            ▼
     ┌──────────────┐
     │  单元测试    │ ──> 失败 → ❌ 阻断
     │  test        │
     └──────┬───────┘
            │ 通过
            ▼
     ┌──────────────┐
     │  安全检查    │ ──> 失败 → ❌ 阻断
     │  硬编码检测  │
     └──────┬───────┘
            │ 通过
            ▼
     ┌──────────────┐
     │  UI测试      │ ──> 仅main/release分支
     │ (条件执行)   │
     └──────┬───────┘
            │
            ▼
     ┌──────────────┐
     │  ✅ 门禁通过  │
     └──────────────┘
```

### 9.4.3 检查项详细说明

| 检查项 | 命令 | 超时 | 失败处理 |
|-------|------|------|---------|
| 构建 | `./gradlew assembleDebug` | 15分钟 | 查看构建日志修复编译错误 |
| Lint | `./gradlew lint` | 10分钟 | 查看lint报告修复规范问题 |
| 单元测试 | `./gradlew test` | 15分钟 | 查看测试报告修复失败用例 |
| 安全检查 | Trivy + 自定义脚本 | 5分钟 | 移除硬编码密钥 |
| AI审查 | presubmit/ai-reviewer + GLM-4.7 | 10分钟 | 仅PR触发，非阻断 |

### 9.4.4 AI代码审查

#### 审查工具

本项目使用双重AI代码审查方案：

| 工具 | 类型 | 需要Token | 功能 |
|------|------|----------|------|
| **reviewdog** | 自动化linter | ❌ 不需要 | Lint结果自动评论到PR |
| **ai-reviewer + GLM-4.7** | AI智能审查 | ✅ 需要配置 | 深度代码分析 |

#### reviewdog功能（免费，无需配置）

- 自动将Lint警告/错误评论到PR对应代码行
- 支持Android Lint、单元测试结果
- 只评论新增代码的问题（filter_mode: added）
- 零配置，开箱即用

#### ai-reviewer配置

> **注意**：本项目使用 [presubmit/ai-reviewer](https://github.com/presubmit/ai-reviewer) + GLM-4.7 进行AI代码审查，需要在GitHub仓库中配置API密钥。

**配置步骤**：

1. 获取GLM API密钥
   - 访问 [智谱AI开放平台](https://open.bigmodel.cn)
   - 注册/登录账号
   - 进入控制台 → API密钥管理
   - 创建新的API密钥

2. 配置GitHub Secret
   ```
   仓库 → Settings → Secrets and variables → Actions → New repository secret

   Name:  GLM_API_KEY
   Value: [您的GLM API密钥]
   ```

3. 验证配置
   - 创建PR触发workflow
   - 查看AI审查评论是否正常生成

**技术配置**：
- Action: `presubmit/ai-reviewer@latest`
- API地址: `https://open.bigmodel.cn/api/coding/paas/v4`
- 模型: `GLM-4.7`
- Provider: `ai-sdk`

#### AI审查内容

AI审查会自动分析以下方面：

- **代码质量**：可读性、命名规范、代码结构
- **潜在Bug**：逻辑错误、边界条件、空指针风险
- **性能问题**：内存泄漏、ANR风险、过度绘制
- **安全隐患**：注入风险、数据泄露、权限问题
- **最佳实践**：Android/Kotlin开发最佳实践建议

#### AI审查示例

```markdown
### 🤖 AI代码审查报告

- ✅ 优点：
  - 使用了Kotlin协程处理异步操作
  - ViewModel遵循MVVM架构规范
  - 状态管理使用StateFlow，符合最佳实践

- ⚠️ 建议：
  - `LoginViewModel.kt:89` 建议添加异常日志记录
  - `AIRepository.kt:45` 可考虑使用sealed class替代enum表示状态
  - `ModelConfig.kt:120` 建议添加参数校验

- 🐛 潜在问题：
  - `AIProvider.kt:156` JSON解析可能抛出异常，建议添加try-catch
```

### 9.4.5 并发控制

- 同一PR的多个workflow只保留最新的
- 新的提交会自动取消之前正在运行的workflow
- 避免资源浪费和重复执行

### 9.4.6 缓存策略

| 缓存项 | 路径 | Key |
|-------|------|-----|
| Gradle缓存 | `~/.gradle/caches` | 基于gradle文件hash |
| Gradle Wrapper | `~/.gradle/wrapper` | 基于gradle文件hash |
| AVD缓存 | `~/.android/avd/*` | 基于API级别 |

### 9.4.7 产物保留

| 产物 | 保留天数 | 用途 |
|-----|---------|------|
| Debug APK | 7天 | 测试安装包 |
| Lint报告 | 14天 | 代码规范问题排查 |
| 测试报告 | 14天 | 测试失败分析 |

### 9.4.8 门禁豁免

以下情况可跳过部分门禁（不推荐，仅紧急情况使用）：

```yaml
# 在commit message中添加标记
git commit -m "fix: 紧急修复 [skip-ci]"     # 跳过所有CI
git commit -m "docs: 更新文档 [skip-test]"  # 跳过测试
```

> **警告**：豁免标记仅用于紧急情况，滥用将被记录并审查。

### 9.4.9 PR评论自动通知

门禁完成后会自动在PR中添加两种评论：

**1. 门禁状态评论**
```
## 🔒 门禁检查结果

| 检查项 | 状态 |
|-------|------|
| 构建 | ✅ success |
| Lint | ✅ success |
| 单元测试 | ✅ success |
| 安全检查 | ✅ success |

### ✅ 所有检查通过，可以合入
```

**2. AI审查评论**
```
### 🤖 AI代码审查报告
[presubmit/ai-reviewer + GLM-4.7 分析结果]
```

### 9.4.10 本地预检

在推送前建议本地执行：

```bash
# 完整预检（推荐）
./gradlew assembleDebug lint test

# 快速预检（仅构建和lint）
./gradlew assembleDebug lint

# 使用项目脚本
bash scripts/check.sh
```

### 9.4.11 配置文件位置

```
.github/
└── workflows/
    └── ci.yml          # 主CI配置（包含AI审查）
```

**配置文件路径**: `.github/workflows/ci.yml`

### 9.4.12 门禁失败排查方法

当GitHub Actions门禁失败时，可使用`gh` CLI工具快速定位问题。

#### 查看失败的workflow列表

```bash
# 查看最近5次失败的workflow
gh run list --status failed --limit 5

# 输出示例：
# completed  failure  feat: xxx  Android CI Gate  feat/xxx  pull_request  12345678  1m30s  2026-03-21
```

#### 查看指定运行的详细日志

```bash
# 查看指定run的完整日志
gh run view {RUN_ID} --log

# 查看日志末尾（快速定位错误）
gh run view {RUN_ID} --log 2>&1 | tail -200

# 搜索关键错误信息
gh run view {RUN_ID} --log 2>&1 | grep -A 10 "Caused by"
gh run view {RUN_ID} --log 2>&1 | grep -A 10 "BUILD FAILED"
gh run view {RUN_ID} --log 2>&1 | grep -A 10 "Could not find"
```

#### 常见错误类型与解决

| 错误关键词 | 可能原因 | 解决方案 |
|-----------|---------|---------|
| `Could not find` | 依赖找不到 | 检查仓库配置、包名版本 |
| `BUILD FAILED` | 编译错误 | 检查代码语法、类型匹配 |
| `RepositoriesMode` | 仓库配置冲突 | 统一在settings.gradle.kts配置 |
| `Permission denied` | 权限不足 | 检查文件权限、workflow配置 |
| `Timeout` | 超时 | 优化构建配置、增加超时时间 |

#### 完整排查流程

```bash
# Step 1: 查看失败的workflow
gh run list --status failed --limit 5

# Step 2: 获取最新的失败run ID
RUN_ID=$(gh run list --status failed --limit 1 --json databaseId --jq '.[0].databaseId')

# Step 3: 查看错误详情
gh run view $RUN_ID --log 2>&1 | grep -E "(FAILURE|Caused by|error:)" -A 5

# Step 4: 本地复现问题
./gradlew assembleDebug lint test

# Step 5: 修复后推送
git add . && git commit -m "fix: 修复xxx问题"
git push
```

#### Web界面查看

也可以直接访问GitHub Actions页面：
```
https://github.com/{OWNER}/{REPO}/actions
```

---

## 十、测试规范

### 10.1 测试目录结构

```
app/src/
├── test/                              # 单元测试（本地JVM）
│   └── java/com/novelapp/aiagent/
│       ├── viewmodel/                 # ViewModel测试
│       │   ├── HomeViewModelTest.kt
│       │   └── EditorViewModelTest.kt
│       ├── repository/                # Repository测试
│       │   ├── NovelRepositoryTest.kt
│       │   └── AIRepositoryTest.kt
│       ├── voice/                     # 语音模块测试
│       │   ├── VoiceCommandParserTest.kt
│       │   └── VoiceStateManagerTest.kt
│       ├── ai/                        # AI模块测试
│       │   └── ContextManagerTest.kt
│       ├── utils/                     # 工具类测试
│       │   └── ExtensionsTest.kt
│       └── harness/                   # Harness管控测试
│           └── CodeValidatorTest.kt
│
└── androidTest/                       # UI测试（设备/模拟器）
    └── java/com/novelapp/aiagent/
        ├── ui/                        # UI测试
        │   ├── HomeActivityTest.kt
        │   ├── EditorActivityTest.kt
        │   └── CreateNovelDialogTest.kt
        ├── data/                      # 数据层集成测试
        │   └── AppDatabaseTest.kt
        └── flow/                      # E2E流程测试
            └── NovelFlowTest.kt
```

### 10.2 单元测试规范

#### 10.2.1 测试命名规范

```kotlin
// 测试类命名：被测类名 + Test
// 示例：HomeViewModel -> HomeViewModelTest

// 测试方法命名：when_条件_then_预期结果
// 或使用中文描述：`测试_场景_预期结果`

class HomeViewModelTest {

    // 推荐：使用 Given-When-Then 格式
    @Test
    fun `when load novels success then update state to Success`() {
        // Given: 准备测试数据
        val novels = listOf(mockNovel())

        // When: 执行被测方法
        viewModel.loadNovels()

        // Then: 验证结果
        assertEquals(UiState.Success(novels), viewModel.uiState.value)
    }

    @Test
    fun `when create novel with empty name then show error`() = runTest {
        // Given
        val emptyName = ""

        // When
        val result = viewModel.createNovel(emptyName)

        // Then
        assertFalse(result.isSuccess)
        assertEquals("请输入小说名称", result.error)
    }

    @Test
    fun `given network error when load novels then show error state`() = runTest {
        // Given
        coEvery { repository.getNovels() } throws NetworkException()

        // When
        viewModel.loadNovels()

        // Then
        assertTrue(viewModel.uiState.value is UiState.Error)
    }
}
```

#### 10.2.2 ViewModel测试模板

```kotlin
// viewmodel/HomeViewModelTest.kt
package com.novelapp.aiagent.viewmodel

import app.cash.turbine.test
import com.novelapp.aiagent.data.repository.NovelRepository
import com.novelapp.aiagent.model.Novel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    // 使用TestDispatcher
    private val testDispatcher = StandardTestDispatcher()

    // Mock依赖
    private val repository: NovelRepository = mockk()

    // 被测对象
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when init then load novels`() = runTest {
        // Given
        val novels = listOf(
            Novel(id = "1", title = "Test Novel", createdAt = System.currentTimeMillis())
        )
        coEvery { repository.getNovels() } returns Result.success(novels)

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.getNovels() }
    }

    @Test
    fun `when load novels success then state is Success`() = runTest {
        // Given
        val novels = listOf(mockNovel())
        coEvery { repository.getNovels() } returns Result.success(novels)

        // When & Then
        viewModel.uiState.test {
            viewModel.loadNovels()
            testDispatcher.scheduler.advanceUntilIdle()

            // 验证加载中状态
            assertTrue(awaitItem() is UiState.Loading)

            // 验证成功状态
            val successState = awaitItem()
            assertTrue(successState is UiState.Success)
            assertEquals(novels, (successState as UiState.Success).data)
        }
    }

    @Test
    fun `when delete novel then call repository`() = runTest {
        // Given
        val novelId = "test-id"
        coEvery { repository.deleteNovel(novelId) } returns Result.success(Unit)

        // When
        viewModel.deleteNovel(novelId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.deleteNovel(novelId) }
    }

    private fun mockNovel() = Novel(
        id = "test-id",
        title = "Test Novel",
        genre = "玄幻",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
```

#### 10.2.3 Repository测试模板

```kotlin
// repository/NovelRepositoryTest.kt
package com.novelapp.aiagent.data.repository

import com.novelapp.aiagent.data.local.NovelDao
import com.novelapp.aiagent.data.local.NovelEntity
import com.novelapp.aiagent.model.Novel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NovelRepositoryTest {

    private val novelDao: NovelDao = mockk()
    private lateinit var repository: NovelRepositoryImpl

    @Before
    fun setup() {
        repository = NovelRepositoryImpl(novelDao)
    }

    @Test
    fun `when getNovels called then return mapped novels`() = runTest {
        // Given
        val entities = listOf(mockNovelEntity())
        coEvery { novelDao.getAllNovels() } returns entities

        // When
        val result = repository.getNovels()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `when createNovel with duplicate name then return failure`() = runTest {
        // Given
        val name = "重复名称"
        coEvery { novelDao.existsByName(name) } returns true

        // When
        val result = repository.createNovel(name, "玄幻")

        // Then
        assertTrue(result.isFailure)
        assertEquals("已存在同名小说", result.exceptionOrNull()?.message)
    }

    private fun mockNovelEntity() = NovelEntity(
        id = "test-id",
        title = "Test Novel",
        genre = "玄幻",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isDeleted = false
    )
}
```

### 10.3 UI测试规范

#### 10.3.1 Activity测试模板

```kotlin
// ui/HomeActivityTest.kt
package com.novelapp.aiagent.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novelapp.aiagent.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(TestModule::class) // 替换测试模块
@RunWith(AndroidJUnit4::class)
class HomeActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(HomeActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun when_activityLaunched_then_displayNovelList() {
        // 验证小说列表显示
        onView(withId(R.id.rv_novel_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_clickCreateButton_then_showCreateDialog() {
        // 点击新建按钮
        onView(withId(R.id.btn_create))
            .perform(click())

        // 验证新建弹窗显示
        onView(withText(R.string.create_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_clickNovelItem_then_navigateToEditor() {
        // 点击列表第一项
        onView(withId(R.id.rv_novel_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // 验证跳转到编辑页
        onView(withId(R.id.editor_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_swipeToDelete_then_showConfirmDialog() {
        // 滑动删除
        onView(withId(R.id.rv_novel_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // 点击删除按钮
        onView(withId(R.id.btn_delete))
            .perform(click())

        // 验证确认弹窗显示
        onView(withText(R.string.delete_title))
            .check(matches(isDisplayed()))
    }
}
```

#### 10.3.2 Fragment测试模板

```kotlin
// ui/EditorFragmentTest.kt
package com.novelapp.aiagent.ui

import androidx.fragment.app.testing.launchFragmentInHiltContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novelapp.aiagent.R
import com.novelapp.aiagent.ui.editor.EditorFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EditorFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun when_fragmentLaunched_then_displayEditorContent() {
        // 启动Fragment
        launchFragmentInHiltContainer<EditorFragment>()

        // 验证编辑器显示
        onView(withId(R.id.et_content))
            .check(matches(isDisplayed()))
    }

    @Test
    fun when_typeContent_then_updateText() {
        launchFragmentInHiltContainer<EditorFragment>()

        // 输入内容
        onView(withId(R.id.et_content))
            .perform(typeText("测试内容"))

        // 验证内容更新
        onView(withId(R.id.et_content))
            .check(matches(withText("测试内容")))
    }

    @Test
    fun when_clickAiGenerate_then_showGeneratingState() {
        launchFragmentInHiltContainer<EditorFragment>()

        // 点击AI生成按钮
        onView(withId(R.id.btn_ai_generate))
            .perform(click())

        // 验证生成中状态
        onView(withId(R.id.tv_ai_status))
            .check(matches(withText(R.string.ai_generating)))
    }
}
```

### 10.4 E2E流程测试

```kotlin
// flow/NovelFlowTest.kt
package com.novelapp.aiagent.flow

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novelapp.aiagent.R
import com.novelapp.aiagent.ui.home.HomeActivity
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NovelFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(HomeActivity::class.java)

    /**
     * 完整流程测试：新建小说 -> 编辑 -> 删除
     */
    @Test
    fun fullFlow_createEditDeleteNovel() {
        // Step 1: 新建小说
        onView(withId(R.id.btn_create))
            .perform(click())

        onView(withId(R.id.et_novel_name))
            .perform(typeText("E2E测试小说"))

        onView(withId(R.id.btn_confirm))
            .perform(click())

        // 验证跳转到编辑页
        onView(withId(R.id.et_content))
            .check(matches(isDisplayed()))

        // Step 2: 编辑内容
        onView(withId(R.id.et_content))
            .perform(typeText("这是E2E测试内容"))

        // 等待自动保存
        Thread.sleep(1000)

        // Step 3: 返回首页
        onView(withId(R.id.btn_back))
            .perform(click())

        // 验证小说出现在列表中
        onView(withText("E2E测试小说"))
            .check(matches(isDisplayed()))

        // Step 4: 删除小说
        onView(withId(R.id.btn_delete))
            .perform(click())

        onView(withText(R.string.delete_confirm))
            .perform(click())

        // 验证小说已删除
        onView(withText("E2E测试小说"))
            .check(doesNotExist())
    }

    /**
     * 语音指令流程测试
     */
    @Test
    fun voiceFlow_createNovelByVoice() {
        // 点击语音按钮
        onView(withId(R.id.btn_voice))
            .perform(click())

        // 验证语音状态
        onView(withId(R.id.tv_voice_status))
            .check(matches(withText(R.string.voice_listening)))

        // 模拟语音输入（需要Mock语音识别）
        // ...
    }
}
```

### 10.5 测试覆盖率要求

| 模块 | 最低覆盖率 | 优先级 |
|------|-----------|--------|
| ViewModel | 80% | P0 |
| Repository | 80% | P0 |
| 语音模块 | 70% | P1 |
| AI模块 | 70% | P1 |
| 工具类 | 90% | P1 |
| UI组件 | 50% | P2 |

```bash
# 运行测试并生成覆盖率报告
./gradlew testDebugUnitTestCoverage

# 查看覆盖率报告
open app/build/reports/coverage/test/debug/index.html
```

### 10.6 Mock和测试工具

#### 10.6.1 依赖配置

```kotlin
// build.gradle.kts 已配置的测试依赖
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("app.cash.turbine:turbine:1.0.0")

androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
```

#### 10.6.2 Mock使用示例

```kotlin
// MockK示例

// 1. 创建Mock对象
private val repository: NovelRepository = mockk()

// 2. 配置Mock行为
coEvery { repository.getNovels() } returns Result.success(emptyList())
coEvery { repository.createNovel(any(), any()) } returns Result.success(mockNovel())

// 3. 验证调用
coVerify { repository.getNovels() }
coVerify(exactly = 1) { repository.createNovel(any(), any()) }
coVerify(timeout = 1000) { repository.saveNovel(any()) }

// 4. 捕获参数
val slot = slot<String>()
coEvery { repository.deleteNovel(capture(slot)) } returns Result.success(Unit)
// 调用后: slot.captured 包含传入的参数

// 5. 抛出异常
coEvery { repository.getNovels() } throws NetworkException()
coEvery { repository.getNovels() } returns Result.failure(NetworkException())
```

#### 10.6.3 Turbine测试Flow

```kotlin
// 测试StateFlow
viewModel.uiState.test {
    // 初始状态
    assertEquals(UiState.Idle, awaitItem())

    // 触发加载
    viewModel.loadNovels()

    // 验证加载中
    assertTrue(awaitItem() is UiState.Loading)

    // 验证成功
    val success = awaitItem()
    assertTrue(success is UiState.Success)

    // 确保没有其他发射
    expectNoEvents()
}
```

### 10.7 测试最佳实践

1. **隔离性**：每个测试独立运行，不依赖其他测试
2. **可重复性**：多次运行结果一致
3. **快速性**：单元测试应在毫秒级完成
4. **可读性**：测试代码清晰表达测试意图
5. **完整性**：覆盖正常流程、边界条件、异常情况

```kotlin
// 完整测试示例
class CreateNovelUseCaseTest {

    private val repository: NovelRepository = mockk()
    private val validator: NovelValidator = mockk()
    private lateinit var useCase: CreateNovelUseCase

    @Before
    fun setup() {
        useCase = CreateNovelUseCase(repository, validator)
    }

    // 正常流程
    @Test
    fun `when valid input then create novel successfully`() = runTest {
        coEvery { validator.validateName("测试小说") } returns true
        coEvery { repository.createNovel(any(), any()) } returns Result.success(mockNovel())

        val result = useCase("测试小说", "玄幻")

        assertTrue(result.isSuccess)
    }

    // 边界条件
    @Test
    fun `when name is 50 chars then create successfully`() = runTest {
        val maxName = "a".repeat(50)
        coEvery { validator.validateName(maxName) } returns true
        coEvery { repository.createNovel(any(), any()) } returns Result.success(mockNovel())

        val result = useCase(maxName, "玄幻")

        assertTrue(result.isSuccess)
    }

    // 异常情况
    @Test
    fun `when name is empty then return validation error`() = runTest {
        coEvery { validator.validateName("") } returns false

        val result = useCase("", "玄幻")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
    }

    @Test
    fun `when repository fails then return error`() = runTest {
        coEvery { validator.validateName(any()) } returns true
        coEvery { repository.createNovel(any(), any()) } returns Result.failure(DatabaseException())

        val result = useCase("测试小说", "玄幻")

        assertTrue(result.isFailure)
    }
}
```

---

## 十一、快速参考

### 11.1 常用命令

```bash
# 构建项目
./gradlew build

# 运行Lint检查
./gradlew lint

# 运行单元测试
./gradlew test

# 运行UI测试
./gradlew connectedAndroidTest

# 生成测试覆盖率报告
./gradlew testDebugUnitTestCoverage

# 生成Release包
./gradlew assembleRelease

# ========== 一键验证命令 ==========

# 运行所有检查（lint + 单元测试）
./gradlew lint test

# 完整验证流程（推荐在提交前执行）
./gradlew lint test && echo "All checks passed! Ready to commit."

# 验证并提交（全部通过后）
./gradlew lint test && git add . && git commit -m "feat: your message"
```

### 11.2 目录速查

| 内容 | 路径 |
|------|------|
| 研发规范 | `/AGENTS.md` |
| 设计文档 | `/docs/design/` |
| 任务记录 | `/docs/task/task_records.json` |
| Harness管控 | `/app/src/main/java/.../harness/` |
| 基类封装 | `/app/src/main/java/.../base/` |
| 数据实体 | `/app/src/main/java/.../model/` |
| 数据库层 | `/app/src/main/java/.../data/local/` |
| 仓库层 | `/app/src/main/java/.../data/repository/` |
| 布局文件 | `/app/src/main/res/layout/` |
| 字符串资源 | `/app/src/main/res/values/strings.xml` |
| 单元测试 | `/app/src/test/java/` |
| UI测试 | `/app/src/androidTest/java/` |
| Lint规则 | `/lint.xml` |

---

**版本**：v1.5.2
**更新日期**：2026-03-23
**维护者**：AI小说安卓App研发团队

### 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.5.2 | 2026-03-23 | AI审查改用presubmit/ai-reviewer + GLM-4.7（API: open.bigmodel.cn） |
| v1.5.1 | 2026-03-21 | 新增9.4.12节门禁失败排查方法（gh CLI使用） |
| v1.5.0 | 2026-03-21 | 核心流程改为PR模式，新增AI代码审查（reviewdog + GLM Code Plan） |
| v1.4.0 | 2026-03-21 | 新增GitHub Actions门禁规范（9.4节），包含构建/Lint/测试/安全检查 |
| v1.3.0 | 2026-03-21 | 核心流程增加构建测试步骤（build → lint → test → push） |
| v1.2.0 | 2026-03-21 | 新增核心开发流程（9.3节），强制lint+test+push流程 |
| v1.1.0 | 2026-03-21 | 新增测试规范章节（单元测试、UI测试、E2E测试） |
| v1.0.0 | 2026-03-21 | 初始版本 |
