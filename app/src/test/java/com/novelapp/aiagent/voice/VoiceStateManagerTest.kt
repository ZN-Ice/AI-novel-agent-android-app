package com.novelapp.aiagent.voice

import com.novelapp.aiagent.model.VoiceCommandType
import com.novelapp.aiagent.model.VoiceState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * VoiceStateManager单元测试
 */
class VoiceStateManagerTest {

    private lateinit var stateManager: VoiceStateManager

    @Before
    fun setup() {
        stateManager = VoiceStateManager()
    }

    // ==================== 状态转换测试 ====================

    @Test
    fun `initial state is Idle`() {
        assertEquals(VoiceState.Idle, stateManager.state.value)
    }

    @Test
    fun `updateState to Listening generates correct feedback`() {
        stateManager.updateState(VoiceState.Listening)

        assertEquals(VoiceState.Listening, stateManager.state.value)
        val feedback = stateManager.feedback.value
        assertNotNull(feedback)
        assertEquals("正在聆听…", feedback?.message)
        assertEquals(VoiceStateManager.FeedbackType.INFO, feedback?.type)
    }

    @Test
    fun `updateState to Processing generates correct feedback`() {
        stateManager.updateState(VoiceState.Processing)

        assertEquals(VoiceState.Processing, stateManager.state.value)
        val feedback = stateManager.feedback.value
        assertNotNull(feedback)
        assertEquals("正在处理…", feedback?.message)
    }

    @Test
    fun `updateState to Success generates correct feedback`() {
        val successState = VoiceState.Success(
            VoiceCommandType.CREATE_NOVEL,
            "新建小说"
        )

        stateManager.updateState(successState)

        assertEquals(successState, stateManager.state.value)
        val feedback = stateManager.feedback.value
        assertNotNull(feedback)
        assertTrue(feedback?.message?.contains("识别成功") == true)
        assertEquals(VoiceStateManager.FeedbackType.SUCCESS, feedback?.type)
    }

    @Test
    fun `updateState to Error generates correct feedback`() {
        val errorState = VoiceState.Error("识别失败")

        stateManager.updateState(errorState)

        assertEquals(errorState, stateManager.state.value)
        val feedback = stateManager.feedback.value
        assertNotNull(feedback)
        assertEquals("识别失败", feedback?.message)
        assertEquals(VoiceStateManager.FeedbackType.ERROR, feedback?.type)
    }

    @Test
    fun `updateState to PermissionDenied generates correct feedback`() {
        stateManager.updateState(VoiceState.PermissionDenied)

        assertEquals(VoiceState.PermissionDenied, stateManager.state.value)
        val feedback = stateManager.feedback.value
        assertNotNull(feedback)
        assertTrue(feedback?.message?.contains("权限") == true)
    }

    // ==================== 反馈管理测试 ====================

    @Test
    fun `showFeedback updates feedback state`() {
        stateManager.showFeedback("测试消息", VoiceStateManager.FeedbackType.INFO)

        val feedback = stateManager.feedback.value
        assertNotNull(feedback)
        assertEquals("测试消息", feedback?.message)
        assertEquals(VoiceStateManager.FeedbackType.INFO, feedback?.type)
    }

    @Test
    fun `clearFeedback sets feedback to null`() {
        stateManager.showFeedback("测试消息", VoiceStateManager.FeedbackType.INFO)
        stateManager.clearFeedback()

        assertNull(stateManager.feedback.value)
    }

    // ==================== 重置测试 ====================

    @Test
    fun `reset clears all state`() {
        stateManager.updateState(VoiceState.Listening)
        stateManager.updateRecognizedText("测试文本")

        stateManager.reset()

        assertEquals(VoiceState.Idle, stateManager.state.value)
        assertNull(stateManager.feedback.value)
        assertEquals("", stateManager.lastRecognizedText.value)
    }

    // ==================== 状态检查测试 ====================

    @Test
    fun `canStartRecording returns true for Idle state`() {
        stateManager.updateState(VoiceState.Idle)

        assertTrue(stateManager.canStartRecording())
    }

    @Test
    fun `canStartRecording returns true for Error state`() {
        stateManager.updateState(VoiceState.Error("错误"))

        assertTrue(stateManager.canStartRecording())
    }

    @Test
    fun `canStartRecording returns false for Listening state`() {
        stateManager.updateState(VoiceState.Listening)

        assertFalse(stateManager.canStartRecording())
    }

    @Test
    fun `isProcessing returns true for Listening state`() {
        stateManager.updateState(VoiceState.Listening)

        assertTrue(stateManager.isProcessing())
    }

    @Test
    fun `isProcessing returns true for Processing state`() {
        stateManager.updateState(VoiceState.Processing)

        assertTrue(stateManager.isProcessing())
    }

    @Test
    fun `isProcessing returns false for Idle state`() {
        stateManager.updateState(VoiceState.Idle)

        assertFalse(stateManager.isProcessing())
    }

    // ==================== 状态提示测试 ====================

    @Test
    fun `getStateHint returns correct hint for Idle`() {
        stateManager.updateState(VoiceState.Idle)

        val hint = stateManager.getStateHint()

        assertTrue(hint.contains("点击麦克风"))
    }

    @Test
    fun `getStateHint returns correct hint for Listening`() {
        stateManager.updateState(VoiceState.Listening)

        val hint = stateManager.getStateHint()

        assertTrue(hint.contains("请说出"))
    }

    @Test
    fun `getStateHint returns correct hint for Error`() {
        stateManager.updateState(VoiceState.Error("测试错误"))

        val hint = stateManager.getStateHint()

        assertTrue(hint.contains("识别失败"))
        assertTrue(hint.contains("测试错误"))
    }

    // ==================== 反馈播报测试 ====================

    @Test
    fun `SUCCESS feedback should speak`() {
        val feedback = VoiceStateManager.VoiceFeedback("成功", VoiceStateManager.FeedbackType.SUCCESS)

        assertTrue(feedback.shouldSpeak())
    }

    @Test
    fun `ERROR feedback should speak`() {
        val feedback = VoiceStateManager.VoiceFeedback("错误", VoiceStateManager.FeedbackType.ERROR)

        assertTrue(feedback.shouldSpeak())
    }

    @Test
    fun `INFO feedback should not speak`() {
        val feedback = VoiceStateManager.VoiceFeedback("信息", VoiceStateManager.FeedbackType.INFO)

        assertFalse(feedback.shouldSpeak())
    }

    @Test
    fun `HINT feedback should not speak`() {
        val feedback = VoiceStateManager.VoiceFeedback("提示", VoiceStateManager.FeedbackType.HINT)

        assertFalse(feedback.shouldSpeak())
    }
}
