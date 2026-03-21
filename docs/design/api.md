# AI接口对接文档

> **文档用途**：定义AI小说创作接口的规范，作为AI模块开发的唯一接口依据。
>
> **维护规则**：本文档为固化文档，变更需人工审批并记录版本。

---

## 一、接口概览

### 1.1 基础信息

| 属性 | 值 |
|------|-----|
| 基础URL | `https://api.novelapp.ai/v1` |
| 协议 | HTTPS |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | Bearer Token |

### 1.2 请求头规范

```http
Content-Type: application/json
Authorization: Bearer <access_token>
X-Request-ID: <uuid>
X-Client-Version: 1.0.0
X-Platform: Android
```

---

## 二、接口定义

### 2.1 生成内容接口

**接口说明**：根据上下文生成小说内容

**请求信息**：

| 属性 | 值 |
|------|-----|
| URL | `POST /generate` |
| 超时 | 30秒 |

**请求参数**：

```json
{
    "novel_id": "string",           // 必填，小说ID
    "chapter_id": "string",         // 必填，章节ID
    "context": "string",            // 必填，上下文内容
    "instruction": "string",        // 可选，用户指令
    "max_tokens": 2000,             // 可选，最大生成长度，默认2000
    "temperature": 0.8,             // 可选，创造性参数，默认0.8，范围0-1
    "stream": false                 // 可选，是否流式返回，默认false
}
```

**响应参数**：

```json
{
    "code": 0,                      // 状态码，0表示成功
    "message": "success",           // 状态信息
    "data": {
        "request_id": "string",     // 请求ID，用于取消/续创
        "text": "string",           // 生成的文本内容
        "tokens": 1234,             // 实际token数
        "finish_reason": "stop"     // 结束原因：stop/length/error
    },
    "timestamp": "2026-03-21T10:30:00Z"
}
```

**示例请求**：

```kotlin
val request = AIRequest(
    novelId = "novel_001",
    chapterId = "chapter_003",
    context = "李明站在山顶，望着远方的云海...",
    instruction = "继续写一段突破场景",
    maxTokens = 2000,
    temperature = 0.8f
)

val response = aiClient.generate(request)
```

### 2.2 流式生成接口

**接口说明**：流式返回生成内容，适合长文本生成

**请求信息**：

| 属性 | 值 |
|------|-----|
| URL | `POST /generate/stream` |
| 超时 | 5分钟 |

**请求参数**：同2.1，`stream` 设为 `true`

**响应格式**：SSE (Server-Sent Events)

```
event: start
data: {"request_id": "req_123"}

event: token
data: {"text": "李明"}

event: token
data: {"text": "深吸"}

event: token
data: {"text": "一口气"}

event: done
data: {"tokens": 2000, "finish_reason": "stop"}

event: error
data: {"code": 500, "message": "内部错误"}
```

### 2.3 取消生成接口

**接口说明**：取消正在进行的生成任务

**请求信息**：

| 属性 | 值 |
|------|-----|
| URL | `POST /generate/cancel` |
| 超时 | 5秒 |

**请求参数**：

```json
{
    "request_id": "string"          // 必填，要取消的请求ID
}
```

**响应参数**：

```json
{
    "code": 0,
    "message": "cancelled",
    "data": {
        "request_id": "string",
        "cancelled_at": "2026-03-21T10:35:00Z",
        "generated_tokens": 500     // 已生成的token数
    }
}
```

### 2.4 断点续创接口

**接口说明**：从检查点继续生成

**请求信息**：

| 属性 | 值 |
|------|-----|
| URL | `POST /generate/resume` |
| 超时 | 30秒 |

**请求参数**：

```json
{
    "checkpoint_id": "string",      // 必填，检查点ID
    "max_tokens": 2000              // 可选，继续生成的长度
}
```

**响应参数**：同2.1

---

## 三、错误码定义

### 3.1 通用错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 0 | 成功 | - |
| 400 | 请求参数错误 | 检查参数格式 |
| 401 | 认证失败 | 检查Token有效性 |
| 403 | 权限不足 | 检查用户权限 |
| 404 | 资源不存在 | 检查ID是否正确 |
| 429 | 请求过于频繁 | 等待后重试 |
| 500 | 服务器内部错误 | 重试或联系支持 |
| 503 | 服务暂不可用 | 稍后重试 |
| 504 | 网关超时 | 重试或降级处理 |

### 3.2 业务错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 1001 | 小说不存在 | 检查novel_id |
| 1002 | 章节不存在 | 检查chapter_id |
| 1003 | 上下文超长 | 减少上下文长度 |
| 1004 | 生成任务不存在 | 检查request_id |
| 1005 | 检查点不存在 | 检查checkpoint_id |
| 1006 | 内容审核不通过 | 修改内容后重试 |
| 1007 | 配额不足 | 升级套餐或等待重置 |

### 3.3 错误响应格式

```json
{
    "code": 400,
    "message": "请求参数错误",
    "data": {
        "field": "max_tokens",
        "reason": "必须大于0"
    },
    "timestamp": "2026-03-21T10:30:00Z"
}
```

---

## 四、超时重试策略

### 4.1 超时配置

```kotlin
object TimeoutConfig {
    // 连接超时
    const val CONNECT_TIMEOUT = 10L       // 秒

    // 读取超时
    const val READ_TIMEOUT = 30L          // 秒

    // 写入超时
    const val WRITE_TIMEOUT = 30L         // 秒

    // 单次生成超时
    const val GENERATE_TIMEOUT = 30L      // 秒

    // 流式生成总超时
    const val STREAM_TIMEOUT = 300L       // 秒（5分钟）

    // 取消请求超时
    const val CANCEL_TIMEOUT = 5L         // 秒
}
```

### 4.2 重试策略

```kotlin
object RetryConfig {
    // 最大重试次数
    const val MAX_RETRY = 3

    // 重试间隔（毫秒）
    val RETRY_INTERVALS = listOf(1000L, 2000L, 4000L)  // 指数退避

    // 可重试的错误码
    val RETRYABLE_CODES = listOf(429, 500, 503, 504)

    // 判断是否需要重试
    fun shouldRetry(code: Int, retryCount: Int): Boolean {
        return code in RETRYABLE_CODES && retryCount < MAX_RETRY
    }
}
```

### 4.3 降级策略

```kotlin
sealed class FallbackStrategy {
    // 重试
    data class Retry(val maxRetries: Int) : FallbackStrategy()

    // 使用本地模板
    object LocalTemplate : FallbackStrategy()

    // 显示错误提示
    data class ShowError(val message: String) : FallbackStrategy()

    // 保存草稿
    data class SaveDraft(val draftId: String) : FallbackStrategy()
}

// 根据错误码选择降级策略
fun getFallbackStrategy(code: Int): FallbackStrategy {
    return when (code) {
        401 -> FallbackStrategy.ShowError("认证失败，请重新登录")
        429 -> FallbackStrategy.Retry(3)
        500 -> FallbackStrategy.Retry(3)
        504 -> FallbackStrategy.SaveDraft(UUID.randomUUID().toString())
        -1 -> FallbackStrategy.LocalTemplate  // 网络异常
        else -> FallbackStrategy.ShowError("发生错误：$code")
    }
}
```

---

## 五、上下文管理

### 5.1 上下文结构

```kotlin
data class NovelContext(
    val novelId: String,
    val title: String,                   // 小说标题
    val genre: String,                   // 类型
    val outline: String,                 // 大纲
    val characters: List<CharacterInfo>, // 角色设定
    val worldSettings: List<WorldSetting>, // 世界观设定
    val recentChapters: List<ChapterSummary>, // 最近章节摘要
    val currentChapter: ChapterContent   // 当前章节
)

data class CharacterInfo(
    val name: String,
    val role: String,                    // 主角/配角
    val description: String,
    val personality: String,
    val abilities: List<String>
)

data class ChapterSummary(
    val chapterId: String,
    val title: String,
    val summary: String,                 // 章节摘要（不超过500字）
    val keyEvents: List<String>          // 关键事件
)
```

### 5.2 上下文拼接规则

```kotlin
class ContextBuilder {

    companion object {
        // 最大上下文长度（tokens）
        const val MAX_CONTEXT_TOKENS = 4000

        // 优先级权重
        val PRIORITY_WEIGHTS = mapOf(
            "character" to 1.0f,    // 角色设定 - 最高优先级
            "outline" to 0.9f,      // 大纲
            "recent" to 0.8f,       // 最近章节
            "history" to 0.5f       // 历史章节
        )
    }

    fun build(context: NovelContext): String {
        val segments = mutableListOf<String>()

        // 1. 添加角色设定（最高优先级）
        val characterSection = buildCharacterSection(context.characters)
        segments.add(characterSection)

        // 2. 添加大纲
        segments.add("【大纲】\n${context.outline}")

        // 3. 添加最近章节摘要
        val recentSection = buildRecentSection(context.recentChapters)
        segments.add(recentSection)

        // 4. 添加当前章节
        segments.add("【当前内容】\n${context.currentChapter.content}")

        // 5. 检查总长度，必要时截断
        return truncateIfNeeded(segments)
    }

    private fun truncateIfNeeded(segments: List<String>): String {
        // 按优先级截断低优先级内容
        // 实现略...
    }
}
```

### 5.3 检查点机制

```kotlin
data class Checkpoint(
    val checkpointId: String,
    val requestId: String,
    val novelId: String,
    val chapterId: String,
    val generatedText: String,           // 已生成的文本
    val tokens: Int,                     // 已生成的token数
    val createdAt: Long,
    val expiresAt: Long                  // 过期时间（24小时）
)

// 检查点保存规则
// 1. 每生成500字保存一个检查点
// 2. 保留最近3个检查点
// 3. 检查点24小时后过期
// 4. 用户主动取消时保留检查点
```

---

## 六、安全规范

### 6.1 Token管理

```kotlin
// 禁止硬编码Token
// ❌ 错误示例
const val API_TOKEN = "sk-xxxxx"

// ✅ 正确示例
object TokenManager {
    private var token: String? = null

    fun setToken(newToken: String) {
        token = newToken
        // 使用EncryptedSharedPreferences安全存储
        saveToSecureStorage(newToken)
    }

    fun getToken(): String? {
        return token ?: loadFromSecureStorage()
    }

    fun clearToken() {
        token = null
        clearSecureStorage()
    }
}
```

### 6.2 请求签名

```kotlin
// 敏感请求需要签名
fun signRequest(params: Map<String, Any>, timestamp: Long): String {
    val sortedParams = params.toSortedMap()
    val signString = sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
    signString += "&timestamp=$timestamp&secret=$API_SECRET"

    return MessageDigest.getInstance("SHA-256")
        .digest(signString.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
```

### 6.3 数据加密

```kotlin
// 敏感数据传输加密
class SecureTransport {

    // 使用HTTPS传输
    // 敏感字段额外加密
    fun encryptField(data: String): String {
        // AES加密实现
    }

    fun decryptField(encrypted: String): String {
        // AES解密实现
    }
}
```

---

## 七、SDK使用示例

### 7.1 初始化

```kotlin
// Application中初始化
class NovelApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AIClient.init(
            context = this,
            baseUrl = BuildConfig.API_BASE_URL,
            connectTimeout = TimeoutConfig.CONNECT_TIMEOUT,
            readTimeout = TimeoutConfig.READ_TIMEOUT
        )
    }
}
```

### 7.2 生成内容

```kotlin
class EditorViewModel(
    private val aiRepository: AIRepository
) : BaseViewModel() {

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState

    fun generateContent(context: NovelContext, instruction: String?) {
        viewModelScope.launch {
            _generateState.value = GenerateState.Loading

            val request = AIRequest(
                novelId = context.novelId,
                chapterId = context.currentChapter.chapterId,
                context = contextBuilder.build(context),
                instruction = instruction,
                maxTokens = 2000,
                temperature = 0.8f
            )

            when (val result = aiRepository.generate(request)) {
                is Result.Success -> {
                    _generateState.value = GenerateState.Success(result.data)
                }
                is Result.Error -> {
                    _generateState.value = GenerateState.Error(result.exception)
                }
            }
        }
    }

    fun cancelGeneration(requestId: String) {
        viewModelScope.launch {
            aiRepository.cancel(requestId)
            _generateState.value = GenerateState.Cancelled
        }
    }
}

sealed class GenerateState {
    object Idle : GenerateState()
    object Loading : GenerateState()
    data class Success(val response: AIResponse) : GenerateState()
    data class Error(val error: Throwable) : GenerateState()
    object Cancelled : GenerateState()
}
```

### 7.3 流式生成

```kotlin
fun generateStream(
    request: AIRequest,
    onToken: (String) -> Unit,
    onComplete: (AIResponse) -> Unit,
    onError: (Throwable) -> Unit
) {
    viewModelScope.launch {
        aiRepository.generateStream(request)
            .catch { e -> onError(e) }
            .collect { event ->
                when (event) {
                    is StreamEvent.Token -> onToken(event.text)
                    is StreamEvent.Done -> onComplete(event.response)
                    is StreamEvent.Error -> onError(event.error)
                }
            }
    }
}
```

---

## 八、版本记录

| 版本 | 日期 | 变更内容 | 审批人 |
|------|------|----------|--------|
| v1.0.0 | 2026-03-21 | 初始化API接口文档 | - |

---

**维护者**：AI小说安卓App研发团队
**相关文档**：[AGENTS.md](../AGENTS.md) | [ui.md](./ui.md) | [voice.md](./voice.md)
