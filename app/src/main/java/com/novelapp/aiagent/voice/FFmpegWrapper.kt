package com.novelapp.aiagent.voice

import android.content.Context
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.FFmpegKitConfig
import com.antonkarpenko.ffmpegkit.FFprobeKit
import com.novelapp.aiagent.model.AIResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

/**
 * ffmpeg封装类
 *
 * 职责：
 * - 音频格式转换
 * - 音频降噪处理
 * - 音频压缩
 * - 获取音频信息
 *
 * @see docs/design/voice.md
 */
class FFmpegWrapper(private val context: Context) {

    companion object {
        private const val TAG = "FFmpegWrapper"

        // 支持的输入格式
        val SUPPORTED_INPUT_FORMATS = listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")

        // 输出格式（用于语音识别）
        const val OUTPUT_FORMAT = "pcm"
        const val OUTPUT_SAMPLE_RATE = 16000
        const val OUTPUT_CHANNELS = 1
    }

    /**
     * 音频信息
     */
    data class AudioInfo(
        val duration: Double,      // 时长（秒）
        val sampleRate: Int,       // 采样率
        val channels: Int,         // 声道数
        val bitRate: Int,          // 比特率
        val format: String         // 格式
    )

    /**
     * 将音频转换为PCM格式（用于语音识别）
     *
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @return 转换结果
     */
    suspend fun convertToPcm(
        inputPath: String,
        outputPath: String
    ): AIResult<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val command = buildString {
                append("-i \"$inputPath\"")
                append(" -f s16le")                    // 16位小端PCM
                append(" -acodec pcm_s16le")           // PCM编码
                append(" -ar $OUTPUT_SAMPLE_RATE")     // 16kHz采样率
                append(" -ac $OUTPUT_CHANNELS")        // 单声道
                append(" -y")                          // 覆盖输出
                append(" \"$outputPath\"")
            }

            Timber.d("Converting audio: $command")

            FFmpegKit.executeAsync(command, { session ->
                val returnCode = session.returnCode
                when {
                    returnCode.isValueSuccess -> {
                        Timber.i("Audio conversion successful: $outputPath")
                        continuation.resume(AIResult.success(outputPath))
                    }
                    returnCode.isValueCancel -> {
                        Timber.w("Audio conversion cancelled")
                        continuation.resume(AIResult.failure("转换已取消"))
                    }
                    else -> {
                        val output = session.output
                        Timber.e("Audio conversion failed: $output")
                        continuation.resume(AIResult.failure("转码失败: ${getErrorMessage(returnCode.value)}"))
                    }
                }
            })

            continuation.invokeOnCancellation {
                FFmpegKit.cancel()
            }
        }
    }

    /**
     * 音频降噪处理
     *
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @return 处理结果
     */
    suspend fun denoise(
        inputPath: String,
        outputPath: String
    ): AIResult<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            // 使用高通和低通滤波器降噪
            val command = buildString {
                append("-i \"$inputPath\"")
                append(" -af \"highpass=f=200,lowpass=f=3000\"")  // 保留人声频率范围
                append(" -ar $OUTPUT_SAMPLE_RATE")
                append(" -y")
                append(" \"$outputPath\"")
            }

            Timber.d("Denoising audio: $command")

            FFmpegKit.executeAsync(command, { session ->
                val returnCode = session.returnCode
                when {
                    returnCode.isValueSuccess -> {
                        Timber.i("Audio denoise successful")
                        continuation.resume(AIResult.success(outputPath))
                    }
                    else -> {
                        val output = session.output
                        Timber.e("Audio denoise failed: $output")
                        continuation.resume(AIResult.failure("降噪失败"))
                    }
                }
            })

            continuation.invokeOnCancellation {
                FFmpegKit.cancel()
            }
        }
    }

    /**
     * 音频压缩
     *
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param bitrate 目标比特率（默认64k）
     * @return 压缩结果
     */
    suspend fun compress(
        inputPath: String,
        outputPath: String,
        bitrate: String = "64k"
    ): AIResult<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val command = buildString {
                append("-i \"$inputPath\"")
                append(" -b:a $bitrate")
                append(" -y")
                append(" \"$outputPath\"")
            }

            Timber.d("Compressing audio: $command")

            FFmpegKit.executeAsync(command, { session ->
                val returnCode = session.returnCode
                when {
                    returnCode.isValueSuccess -> {
                        Timber.i("Audio compression successful")
                        continuation.resume(AIResult.success(outputPath))
                    }
                    else -> {
                        val output = session.output
                        Timber.e("Audio compression failed: $output")
                        continuation.resume(AIResult.failure("压缩失败"))
                    }
                }
            })

            continuation.invokeOnCancellation {
                FFmpegKit.cancel()
            }
        }
    }

    /**
     * 获取音频信息
     *
     * @param inputPath 输入文件路径
     * @return 音频信息
     */
    suspend fun getAudioInfo(inputPath: String): AIResult<AudioInfo> = withContext(Dispatchers.IO) {
        try {
            val mediaInformation = FFprobeKit.getMediaInformation(inputPath).mediaInformation

            if (mediaInformation == null) {
                return@withContext AIResult.failure("无法获取音频信息")
            }

            val duration = mediaInformation.duration?.toDoubleOrNull() ?: 0.0
            val stream = mediaInformation.streams?.firstOrNull()

            val audioInfo = AudioInfo(
                duration = duration,
                sampleRate = stream?.sampleRate?.toIntOrNull() ?: 0,
                channels = stream?.channels?.toIntOrNull() ?: 0,
                bitRate = mediaInformation.bitrate?.toIntOrNull() ?: 0,
                format = mediaInformation.format ?: "unknown"
            )

            Timber.d("Audio info: $audioInfo")
            AIResult.success(audioInfo)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get audio info")
            AIResult.failure("获取音频信息失败: ${e.message}")
        }
    }

    /**
     * 裁剪音频
     *
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param startTime 开始时间（秒）
     * @param duration 持续时间（秒）
     * @return 裁剪结果
     */
    suspend fun trim(
        inputPath: String,
        outputPath: String,
        startTime: Double,
        duration: Double
    ): AIResult<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val command = buildString {
                append("-i \"$inputPath\"")
                append(" -ss $startTime")
                append(" -t $duration")
                append(" -c copy")
                append(" -y")
                append(" \"$outputPath\"")
            }

            Timber.d("Trimming audio: $command")

            FFmpegKit.executeAsync(command, { session ->
                val returnCode = session.returnCode
                when {
                    returnCode.isValueSuccess -> {
                        Timber.i("Audio trim successful")
                        continuation.resume(AIResult.success(outputPath))
                    }
                    else -> {
                        continuation.resume(AIResult.failure("裁剪失败"))
                    }
                }
            })

            continuation.invokeOnCancellation {
                FFmpegKit.cancel()
            }
        }
    }

    /**
     * 取消所有执行中的任务
     */
    fun cancelAll() {
        FFmpegKit.cancel()
        Timber.i("All FFmpeg tasks cancelled")
    }

    /**
     * 获取ffmpeg版本信息
     */
    fun getVersion(): String {
        return FFmpegKitConfig.getVersion()
    }

    /**
     * 获取错误消息
     */
    private fun getErrorMessage(returnCode: Int): String {
        return when (returnCode) {
            1 -> "通用错误"
            2 -> "无效参数"
            3 -> "文件不存在"
            4 -> "权限被拒绝"
            5 -> "内存不足"
            else -> "未知错误 ($returnCode)"
        }
    }
}
