package com.novelapp.aiagent.ai.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AI DTO序列化/反序列化测试
 *
 * TDD RED Phase: 测试kotlinx.serialization替代regex解析
 */
class AIDtoSerializationTest {

    private lateinit var json: Json

    @Before
    fun setup() {
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
    }

    // ==================== GLM Request DTO 测试 ====================

    @Test
    fun `when serialize GLM request then contains required fields`() {
        val request = GLMChatRequest(
            model = "GLM-4.7",
            messages = listOf(
                GLMMessage(role = "system", content = "你是小说创作助手"),
                GLMMessage(role = "user", content = "请续写以下内容")
            ),
            maxTokens = 2000,
            temperature = 0.8f,
            stream = false
        )

        val jsonString = json.encodeToString(request)

        assertTrue(jsonString.contains("\"model\":\"GLM-4.7\""))
        assertTrue(jsonString.contains("\"max_tokens\":2000"))
        assertTrue(jsonString.contains("\"temperature\":0.8"))
        assertTrue(jsonString.contains("\"stream\":false"))
        assertTrue(jsonString.contains("\"messages\""))
    }

    @Test
    fun `when serialize GLM request with stream=true then stream field is true`() {
        val request = GLMChatRequest(
            model = "GLM-5",
            messages = listOf(GLMMessage(role = "user", content = "test")),
            stream = true
        )

        val jsonString = json.encodeToString(request)

        assertTrue(jsonString.contains("\"stream\":true"))
    }

    @Test
    fun `when serialize GLM request with instruction then includes prompt field`() {
        val request = GLMChatRequest(
            model = "GLM-4.6",
            messages = listOf(GLMMessage(role = "user", content = "context")),
            prompt = "请重点描写战斗场景"
        )

        val jsonString = json.encodeToString(request)

        assertTrue(jsonString.contains("\"prompt\":\"请重点描写战斗场景\""))
    }

    // ==================== GLM Response DTO 测试 ====================

    @Test
    fun `when deserialize GLM response then parsed correctly`() {
        val responseJson = """
        {
            "id": "chatcmpl-123",
            "created": 1677652288,
            "model": "GLM-4.7",
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "这是AI生成的内容"
                },
                "finish_reason": "stop"
            }],
            "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 20,
                "total_tokens": 30
            }
        }
        """

        val response = json.decodeFromString<GLMChatResponse>(responseJson)

        assertEquals("chatcmpl-123", response.id)
        assertEquals("GLM-4.7", response.model)
        assertEquals(1, response.choices.size)

        val choice = response.choices[0]
        assertEquals(0, choice.index)
        assertEquals("这是AI生成的内容", choice.message.content)
        assertEquals("stop", choice.finishReason)

        assertEquals(10, response.usage.promptTokens)
        assertEquals(20, response.usage.completionTokens)
        assertEquals(30, response.usage.totalTokens)
    }

    @Test
    fun `when deserialize GLM response with length finish reason then parsed correctly`() {
        val responseJson = """
        {
            "id": "chatcmpl-456",
            "created": 1677652288,
            "model": "GLM-4.6",
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "内容被截断"
                },
                "finish_reason": "length"
            }],
            "usage": {
                "prompt_tokens": 100,
                "completion_tokens": 2000,
                "total_tokens": 2100
            }
        }
        """

        val response = json.decodeFromString<GLMChatResponse>(responseJson)

        assertEquals("length", response.choices[0].finishReason)
        assertEquals(2000, response.usage.completionTokens)
    }

    @Test
    fun `when deserialize GLM response with multiline content then newlines preserved`() {
        val responseJson = """
        {
            "id": "chatcmpl-789",
            "created": 1677652288,
            "model": "GLM-4.7",
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "第一段\n第二段\n第三段"
                },
                "finish_reason": "stop"
            }],
            "usage": {
                "prompt_tokens": 5,
                "completion_tokens": 15,
                "total_tokens": 20
            }
        }
        """

        val response = json.decodeFromString<GLMChatResponse>(responseJson)

        assertTrue(response.choices[0].message.content.contains("\n"))
        assertEquals(3, response.choices[0].message.content.split("\n").size)
    }

    // ==================== SSE Event 解析测试 ====================

    @Test
    fun `when parse SSE data event then extract content correctly`() {
        val sseLine = """data:{"id":"chatcmpl-1","choices":[{"delta":{"content":"你好"},"finish_reason":null}]}"""

        val event = SSEParser.parseSSELine(sseLine)

        assertNotNull(event)
        assertTrue(event is SSEEvent.Data)
        val data = event as SSEEvent.Data
        assertEquals("你好", data.content)
    }

    @Test
    fun `when parse SSE done event then return Done`() {
        val sseLine = "data:[DONE]"

        val event = SSEParser.parseSSELine(sseLine)

        assertNotNull(event)
        assertTrue(event is SSEEvent.Done)
    }

    @Test
    fun `when parse SSE empty line then return null`() {
        val sseLine = ""

        val event = SSEParser.parseSSELine(sseLine)

        assertNull(event)
    }

    @Test
    fun `when parse SSE comment line then return null`() {
        val sseLine = ": this is a comment"

        val event = SSEParser.parseSSELine(sseLine)

        assertNull(event)
    }

    @Test
    fun `when parse SSE event with finish_reason stop then contains finish reason`() {
        val sseLine = """data:{"id":"chatcmpl-2","choices":[{"delta":{"content":"结尾"},"finish_reason":"stop"}]}"""

        val event = SSEParser.parseSSELine(sseLine)

        assertNotNull(event)
        assertTrue(event is SSEEvent.Data)
        val data = event as SSEEvent.Data
        assertEquals("stop", data.finishReason)
    }

    @Test
    fun `when parse multiple SSE lines then accumulate content correctly`() {
        val lines = listOf(
            """data:{"id":"chatcmpl-3","choices":[{"delta":{"content":"第一段"},"finish_reason":null}]}""",
            """data:{"id":"chatcmpl-3","choices":[{"delta":{"content":"第二段"},"finish_reason":null}]}""",
            """data:{"id":"chatcmpl-3","choices":[{"delta":{"content":"第三段"},"finish_reason":"stop"}]}""",
            "data:[DONE]"
        )

        val contents = mutableListOf<String>()
        var done = false

        for (line in lines) {
            val event = SSEParser.parseSSELine(line)
            when (event) {
                is SSEEvent.Data -> contents.add(event.content)
                is SSEEvent.Done -> done = true
                null -> {}
            }
        }

        assertEquals(3, contents.size)
        assertEquals("第一段第二段第三段", contents.joinToString(""))
        assertTrue(done)
    }

    // ==================== Internal API DTO 测试 ====================

    @Test
    fun `when serialize internal AIRequest then format correctly`() {
        val request = InternalAIRequest(
            novelId = "novel-123",
            chapterId = "chapter-456",
            context = "这是上下文内容",
            instruction = "请续写",
            maxTokens = 2000,
            temperature = 0.8f
        )

        val jsonString = json.encodeToString(request)

        assertTrue(jsonString.contains("\"novel_id\":\"novel-123\""))
        assertTrue(jsonString.contains("\"chapter_id\":\"chapter-456\""))
        assertTrue(jsonString.contains("\"max_tokens\":2000"))
    }

    @Test
    fun `when deserialize internal AIResponse then parse correctly`() {
        val responseJson = """
        {
            "code": 0,
            "message": "success",
            "data": {
                "request_id": "req-001",
                "text": "生成的小说内容",
                "tokens": 150,
                "finish_reason": "stop"
            }
        }
        """

        val response = json.decodeFromString<InternalAIResponse>(responseJson)

        assertEquals(0, response.code)
        assertEquals("success", response.message)
        val data = response.data
        assertNotNull(data)
        assertEquals("req-001", data!!.requestId)
        assertEquals("生成的小说内容", data.text)
        assertEquals(150, data.tokens)
        assertEquals("stop", data.finishReason)
    }

    @Test
    fun `when deserialize error response then data is null`() {
        val responseJson = """
        {
            "code": 500,
            "message": "服务器内部错误",
            "data": null
        }
        """

        val response = json.decodeFromString<InternalAIResponse>(responseJson)

        assertEquals(500, response.code)
        assertEquals("服务器内部错误", response.message)
        assertNull(response.data)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `when serialize request with special characters then escaped correctly`() {
        val request = GLMChatRequest(
            model = "GLM-4.7",
            messages = listOf(
                GLMMessage(
                    role = "user",
                    content = "包含\"引号\"和\n换行符"
                )
            )
        )

        val jsonString = json.encodeToString(request)

        // Should not crash and should be valid JSON
        val parsed = json.decodeFromString<GLMChatRequest>(jsonString)
        assertEquals("包含\"引号\"和\n换行符", parsed.messages[0].content)
    }

    @Test
    fun `when deserialize response with unknown fields then ignore them`() {
        val responseJson = """
        {
            "id": "chatcmpl-x",
            "created": 1677652288,
            "model": "GLM-4.7",
            "unknown_field": "should be ignored",
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "test"
                },
                "finish_reason": "stop",
                "extra_field": 123
            }],
            "usage": {
                "prompt_tokens": 1,
                "completion_tokens": 1,
                "total_tokens": 2
            }
        }
        """

        // Should not throw
        val response = json.decodeFromString<GLMChatResponse>(responseJson)
        assertEquals("test", response.choices[0].message.content)
    }
}
