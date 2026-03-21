package com.novelapp.aiagent.model

import org.junit.Assert.*
import org.junit.Test

/**
 * VoiceCommand模型单元测试
 */
class VoiceCommandTest {

    @Test
    fun `VoiceRecognitionResult success has correct properties`() {
        val result = VoiceRecognitionResult.success("新建小说", 0.95f)

        assertTrue(result.isSuccess)
        assertEquals("新建小说", result.text)
        assertEquals(0.95f, result.confidence, 0.01f)
        assertTrue(result.isFinal)
        assertNull(result.errorCode)
    }

    @Test
    fun `VoiceRecognitionResult error has correct properties`() {
        val result = VoiceRecognitionResult.error(1001, "网络错误")

        assertFalse(result.isSuccess)
        assertEquals("", result.text)
        assertEquals(1001, result.errorCode)
        assertEquals("网络错误", result.errorMessage)
    }

    @Test
    fun `VoiceState Idle is not listening or processing`() {
        val state = VoiceState.Idle

        assertFalse(state.isListening)
        assertFalse(state.isProcessing)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun `VoiceState Listening is listening`() {
        val state = VoiceState.Listening

        assertTrue(state.isListening)
        assertFalse(state.isProcessing)
    }

    @Test
    fun `VoiceState Processing is processing`() {
        val state = VoiceState.Processing

        assertFalse(state.isListening)
        assertTrue(state.isProcessing)
    }

    @Test
    fun `VoiceState Success is success`() {
        val state = VoiceState.Success(VoiceCommandType.CREATE_NOVEL, "新建小说")

        assertTrue(state.isSuccess)
        assertEquals(VoiceCommandType.CREATE_NOVEL, state.command)
        assertEquals("新建小说", state.text)
    }

    @Test
    fun `VoiceState Error is error`() {
        val state = VoiceState.Error("识别失败")

        assertTrue(state.isError)
        assertEquals("识别失败", state.message)
    }

    @Test
    fun `VoiceCommandType fromAction returns correct type`() {
        assertEquals(
            VoiceCommandType.CREATE_NOVEL,
            VoiceCommandType.fromAction("ACTION_CREATE_NOVEL")
        )
        assertEquals(
            VoiceCommandType.DELETE_NOVEL,
            VoiceCommandType.fromAction("ACTION_DELETE_NOVEL")
        )
        assertNull(VoiceCommandType.fromAction("UNKNOWN_ACTION"))
    }
}
