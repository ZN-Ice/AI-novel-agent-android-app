package com.novelapp.aiagent.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * 录音管理器
 *
 * 职责：
 * - 开始/停止录音
 * - 监控录音状态
 * - 获取音量振幅
 *
 * @see docs/design/voice.md
 */
class VoiceRecorder(private val context: Context) {

    companion object {
        private const val TAG = "VoiceRecorder"
        private const val DEFAULT_SAMPLE_RATE = 44100
        private const val DEFAULT_BIT_RATE = 128000
        private const val DEFAULT_OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
        private const val DEFAULT_AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
    }

    // MediaRecorder实例
    private var mediaRecorder: MediaRecorder? = null

    // 输出文件
    private var outputFile: File? = null

    // 录音状态
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // 音量振幅
    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    // 录音时长（毫秒）
    private var recordingStartTime: Long = 0

    // 最大录音时长（毫秒）
    var maxDuration: Long = 60 * 1000L // 默认60秒

    /**
     * 录音状态
     */
    sealed class RecordingState {
        object Idle : RecordingState()
        object Recording : RecordingState()
        data class Error(val message: String) : RecordingState()

        val isRecording: Boolean
            get() = this is Recording
    }

    /**
     * 开始录音
     *
     * @param outputPath 输出文件路径（可选，不传则自动生成）
     * @return 是否成功开始录音
     */
    fun startRecording(outputPath: String? = null): Boolean {
        if (_recordingState.value.isRecording) {
            Timber.w("Already recording, ignore start request")
            return false
        }

        return try {
            // 准备输出文件
            outputFile = if (outputPath != null) {
                File(outputPath).apply {
                    parentFile?.mkdirs()
                }
            } else {
                createDefaultOutputFile()
            }

            // 创建MediaRecorder
            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(DEFAULT_OUTPUT_FORMAT)
                setAudioEncoder(DEFAULT_AUDIO_ENCODER)
                setAudioEncodingBitRate(DEFAULT_BIT_RATE)
                setAudioSamplingRate(DEFAULT_SAMPLE_RATE)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording
            _amplitude.value = 0

            Timber.i("Recording started: ${outputFile?.absolutePath}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            _recordingState.value = RecordingState.Error("录音启动失败: ${e.message}")
            releaseRecorder()
            false
        }
    }

    /**
     * 停止录音
     *
     * @return 录音文件，失败返回null
     */
    fun stopRecording(): File? {
        if (!_recordingState.value.isRecording) {
            Timber.w("Not recording, ignore stop request")
            return null
        }

        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val duration = System.currentTimeMillis() - recordingStartTime
            Timber.i("Recording stopped, duration: ${duration}ms")

            _recordingState.value = RecordingState.Idle
            _amplitude.value = 0

            outputFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop recording")
            _recordingState.value = RecordingState.Error("录音停止失败: ${e.message}")
            releaseRecorder()
            null
        }
    }

    /**
     * 取消录音（不保存文件）
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // 删除录音文件
            outputFile?.delete()
            outputFile = null

            Timber.i("Recording cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Error during cancel recording")
        } finally {
            _recordingState.value = RecordingState.Idle
            _amplitude.value = 0
        }
    }

    /**
     * 获取当前振幅（用于波形显示）
     *
     * @return 振幅值（0-32767）
     */
    fun updateAmplitude(): Int {
        if (!_recordingState.value.isRecording) {
            return 0
        }

        return try {
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            _amplitude.value = amplitude
            amplitude
        } catch (e: Exception) {
            Timber.e(e, "Failed to get amplitude")
            0
        }
    }

    /**
     * 获取录音时长
     *
     * @return 时长（毫秒）
     */
    fun getRecordingDuration(): Long {
        return if (_recordingState.value.isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0
        }
    }

    /**
     * 检查是否超过最大时长
     *
     * @return 是否超时
     */
    fun isDurationExceeded(): Boolean {
        return getRecordingDuration() > maxDuration
    }

    /**
     * 创建MediaRecorder实例
     */
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    /**
     * 创建默认输出文件
     */
    private fun createDefaultOutputFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "voice_$timestamp.m4a"
        val dir = File(context.cacheDir, "voice_recordings")
        dir.mkdirs()
        return File(dir, fileName)
    }

    /**
     * 释放MediaRecorder资源
     */
    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing recorder")
        }
        mediaRecorder = null
    }

    /**
     * 清理缓存文件
     */
    fun cleanCache() {
        val cacheDir = File(context.cacheDir, "voice_recordings")
        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
                    file.delete()
                }
            }
        }
    }
}
