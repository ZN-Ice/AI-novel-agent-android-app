package com.novelapp.aiagent.voice

import com.novelapp.aiagent.model.VoiceCommandEntity
import com.novelapp.aiagent.model.VoiceCommandType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音指令缓存
 *
 * 职责：
 * - 缓存最近执行的语音指令
 * - 支持按类型和场景查询历史指令
 * - 提供指令去重和过期清理
 *
 * @see docs/design/voice.md
 */
@Singleton
class VoiceCommandCache @Inject constructor() {

    companion object {
        private const val TAG = "VoiceCommandCache"
        const val DEFAULT_MAX_SIZE = 50
        const val DEFAULT_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val cache = LinkedHashMap<String, VoiceCommandEntity>(DEFAULT_MAX_SIZE, 0.75f, true)
    private var maxSize: Int = DEFAULT_MAX_SIZE
    private var expiryMs: Long = DEFAULT_EXPIRY_MS

    /**
     * 缓存配置
     */
    data class CacheConfig(
        val maxSize: Int = DEFAULT_MAX_SIZE,
        val expiryMs: Long = DEFAULT_EXPIRY_MS
    )

    /**
     * 更新缓存配置
     */
    fun configure(config: CacheConfig) {
        maxSize = config.maxSize
        expiryMs = config.expiryMs
        Timber.d("Cache configured: maxSize=$maxSize, expiryMs=$expiryMs")
    }

    /**
     * 添加指令到缓存
     */
    @Synchronized
    fun put(command: VoiceCommandEntity): VoiceCommandEntity {
        evictExpired()
        trimToSize()

        cache[command.id] = command
        Timber.d("Cached command: ${command.commandType}, id=${command.id}")

        return command
    }

    /**
     * 根据ID获取缓存指令
     */
    @Synchronized
    fun get(id: String): VoiceCommandEntity? {
        val command = cache[id]
        if (command != null && isExpired(command)) {
            cache.remove(id)
            return null
        }
        return command
    }

    /**
     * 移除缓存指令
     */
    @Synchronized
    fun remove(id: String): VoiceCommandEntity? {
        return cache.remove(id)
    }

    /**
     * 清空所有缓存
     */
    @Synchronized
    fun clear() {
        val size = cache.size
        cache.clear()
        Timber.d("Cache cleared, removed $size entries")
    }

    /**
     * 获取所有缓存指令（排除已过期的）
     */
    @Synchronized
    fun getAll(): List<VoiceCommandEntity> {
        evictExpired()
        return cache.values.toList()
    }

    /**
     * 按指令类型查询
     */
    @Synchronized
    fun getByType(type: VoiceCommandType): List<VoiceCommandEntity> {
        evictExpired()
        return cache.values.filter { it.commandType == type }
    }

    /**
     * 按场景查询可用指令
     */
    @Synchronized
    fun getByScene(scene: VoiceScene): List<VoiceCommandEntity> {
        val sceneTypes = VoiceCommandParser.getHintsForScene(scene)
            .map { it.command }
            .toSet()

        evictExpired()
        return cache.values.filter { it.commandType in sceneTypes }
    }

    /**
     * 获取最近的指令
     */
    @Synchronized
    fun getRecent(count: Int = 10): List<VoiceCommandEntity> {
        evictExpired()
        return cache.values.toList().takeLast(count)
    }

    /**
     * 获取缓存大小
     */
    @Synchronized
    fun size(): Int = cache.size

    /**
     * 检查是否包含指定ID的指令
     */
    @Synchronized
    fun contains(id: String): Boolean = cache.containsKey(id)

    /**
     * 检查指令是否已过期
     */
    private fun isExpired(command: VoiceCommandEntity): Boolean {
        return System.currentTimeMillis() - command.timestamp > expiryMs
    }

    /**
     * 清除过期指令
     */
    private fun evictExpired() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { now - it.value.timestamp > expiryMs }
            .map { it.key }

        expiredKeys.forEach { cache.remove(it) }

        if (expiredKeys.isNotEmpty()) {
            Timber.d("Evicted ${expiredKeys.size} expired commands")
        }
    }

    /**
     * 当缓存超过最大容量时移除最旧的条目
     */
    private fun trimToSize() {
        while (cache.size >= maxSize) {
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
            Timber.d("Trimmed oldest command: $oldestKey")
        }
    }
}
