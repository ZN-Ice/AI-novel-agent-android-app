package com.novelapp.aiagent.voice

import com.novelapp.aiagent.model.VoiceCommandEntity
import com.novelapp.aiagent.model.VoiceCommandType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VoiceCommandCacheTest {

    private lateinit var cache: VoiceCommandCache

    private fun createCommand(
        id: String = "cmd-${System.nanoTime()}",
        type: VoiceCommandType = VoiceCommandType.SAVE,
        text: String = "保存",
        timestamp: Long = System.currentTimeMillis()
    ): VoiceCommandEntity {
        return VoiceCommandEntity(
            id = id,
            commandType = type,
            recognizedText = text,
            confidence = 0.9f,
            timestamp = timestamp
        )
    }

    @Before
    fun setUp() {
        cache = VoiceCommandCache()
    }

    // ==================== put / get 基础测试 ====================

    @Test
    fun `put and get by id`() {
        val command = createCommand(id = "test-1")
        cache.put(command)

        val result = cache.get("test-1")
        assertNotNull(result)
        assertEquals("test-1", result!!.id)
        assertEquals(VoiceCommandType.SAVE, result.commandType)
    }

    @Test
    fun `get non-existent id returns null`() {
        assertNull(cache.get("non-existent"))
    }

    @Test
    fun `put returns the same command`() {
        val command = createCommand(id = "test-2")
        val returned = cache.put(command)

        assertSame(command, returned)
    }

    @Test
    fun `put overwrites existing id`() {
        val cmd1 = createCommand(id = "dup", type = VoiceCommandType.SAVE, text = "保存")
        cache.put(cmd1)

        val cmd2 = createCommand(id = "dup", type = VoiceCommandType.UNDO, text = "撤销")
        cache.put(cmd2)

        val result = cache.get("dup")!!
        assertEquals(VoiceCommandType.UNDO, result.commandType)
        assertEquals("撤销", result.recognizedText)
    }

    // ==================== remove 测试 ====================

    @Test
    fun `remove existing command returns it`() {
        val command = createCommand(id = "rm-1")
        cache.put(command)

        val removed = cache.remove("rm-1")
        assertNotNull(removed)
        assertEquals("rm-1", removed!!.id)
        assertNull(cache.get("rm-1"))
    }

    @Test
    fun `remove non-existent returns null`() {
        assertNull(cache.remove("non-existent"))
    }

    // ==================== clear 测试 ====================

    @Test
    fun `clear removes all entries`() {
        repeat(5) { cache.put(createCommand(id = "cmd-$it")) }
        assertEquals(5, cache.size())

        cache.clear()
        assertEquals(0, cache.size())
    }

    // ==================== getAll 测试 ====================

    @Test
    fun `getAll returns all cached commands`() {
        val commands = (0..4).map { createCommand(id = "all-$it") }
        commands.forEach { cache.put(it) }

        val all = cache.getAll()
        assertEquals(5, all.size)
        commands.forEach { cmd ->
            assertTrue(all.any { it.id == cmd.id })
        }
    }

    @Test
    fun `getAll on empty cache returns empty list`() {
        assertTrue(cache.getAll().isEmpty())
    }

    // ==================== getByType 测试 ====================

    @Test
    fun `getByType returns matching commands`() {
        cache.put(createCommand(id = "t1", type = VoiceCommandType.SAVE))
        cache.put(createCommand(id = "t2", type = VoiceCommandType.SAVE))
        cache.put(createCommand(id = "t3", type = VoiceCommandType.UNDO))

        val saves = cache.getByType(VoiceCommandType.SAVE)
        assertEquals(2, saves.size)
        assertTrue(saves.all { it.commandType == VoiceCommandType.SAVE })
    }

    @Test
    fun `getByType with no matches returns empty list`() {
        cache.put(createCommand(type = VoiceCommandType.SAVE))
        assertTrue(cache.getByType(VoiceCommandType.HELP).isEmpty())
    }

    // ==================== getByScene 测试 ====================

    @Test
    fun `getByScene HOME filters correctly`() {
        cache.put(createCommand(id = "h1", type = VoiceCommandType.CREATE_NOVEL))
        cache.put(createCommand(id = "h2", type = VoiceCommandType.SAVE))
        cache.put(createCommand(id = "h3", type = VoiceCommandType.HELP))

        val home = cache.getByScene(VoiceScene.HOME)
        assertTrue(home.any { it.commandType == VoiceCommandType.CREATE_NOVEL })
        assertTrue(home.any { it.commandType == VoiceCommandType.HELP })
        assertFalse(home.any { it.commandType == VoiceCommandType.SAVE })
    }

    @Test
    fun `getByScene EDITOR filters correctly`() {
        cache.put(createCommand(id = "e1", type = VoiceCommandType.START_WRITING))
        cache.put(createCommand(id = "e2", type = VoiceCommandType.UNDO))
        cache.put(createCommand(id = "e3", type = VoiceCommandType.CREATE_NOVEL))

        val editor = cache.getByScene(VoiceScene.EDITOR)
        assertTrue(editor.any { it.commandType == VoiceCommandType.START_WRITING })
        assertTrue(editor.any { it.commandType == VoiceCommandType.UNDO })
        assertFalse(editor.any { it.commandType == VoiceCommandType.CREATE_NOVEL })
    }

    @Test
    fun `getByScene DELETE_DIALOG filters correctly`() {
        cache.put(createCommand(id = "d1", type = VoiceCommandType.CONFIRM_DELETE))
        cache.put(createCommand(id = "d2", type = VoiceCommandType.CANCEL_DELETE))
        cache.put(createCommand(id = "d3", type = VoiceCommandType.SAVE))

        val dialog = cache.getByScene(VoiceScene.DELETE_DIALOG)
        assertEquals(2, dialog.size)
        assertTrue(dialog.all {
            it.commandType == VoiceCommandType.CONFIRM_DELETE ||
                it.commandType == VoiceCommandType.CANCEL_DELETE
        })
    }

    // ==================== getRecent 测试 ====================

    @Test
    fun `getRecent returns last N commands`() {
        repeat(15) { cache.put(createCommand(id = "r-$it")) }

        val recent5 = cache.getRecent(5)
        assertEquals(5, recent5.size)
    }

    @Test
    fun `getRecent with count larger than size returns all`() {
        repeat(3) { cache.put(createCommand(id = "few-$it")) }

        val recent = cache.getRecent(10)
        assertEquals(3, recent.size)
    }

    @Test
    fun `getRecent default count is 10`() {
        repeat(20) { cache.put(createCommand(id = "def-$it")) }

        val recent = cache.getRecent()
        assertEquals(10, recent.size)
    }

    // ==================== size / contains 测试 ====================

    @Test
    fun `size reflects cached count`() {
        assertEquals(0, cache.size())
        cache.put(createCommand(id = "s1"))
        assertEquals(1, cache.size())
        cache.put(createCommand(id = "s2"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `contains checks by id`() {
        cache.put(createCommand(id = "c1"))
        assertTrue(cache.contains("c1"))
        assertFalse(cache.contains("c2"))
    }

    // ==================== trimToSize 测试 ====================

    @Test
    fun `cache trims to maxSize on overflow`() {
        cache.configure(VoiceCommandCache.CacheConfig(maxSize = 3, expiryMs = Long.MAX_VALUE))

        val commands = (0..4).map { createCommand(id = "trim-$it") }
        commands.forEach { cache.put(it) }

        assertEquals(3, cache.size())
        // oldest entries should have been evicted
        assertNull(cache.get("trim-0"))
        assertNull(cache.get("trim-1"))
        assertNotNull(cache.get("trim-2"))
        assertNotNull(cache.get("trim-3"))
        assertNotNull(cache.get("trim-4"))
    }

    // ==================== 过期清理测试 ====================

    @Test
    fun `expired commands are not returned by get`() {
        cache.configure(VoiceCommandCache.CacheConfig(maxSize = 50, expiryMs = 1000L))

        val expired = createCommand(id = "exp-1", timestamp = System.currentTimeMillis() - 5000)
        cache.put(expired)

        assertNull(cache.get("exp-1"))
    }

    @Test
    fun `non-expired commands are returned by get`() {
        cache.configure(VoiceCommandCache.CacheConfig(maxSize = 50, expiryMs = 60000L))

        val fresh = createCommand(id = "fresh-1", timestamp = System.currentTimeMillis())
        cache.put(fresh)

        assertNotNull(cache.get("fresh-1"))
    }

    @Test
    fun `getAll evicts expired entries`() {
        cache.configure(VoiceCommandCache.CacheConfig(maxSize = 50, expiryMs = 1000L))

        cache.put(createCommand(id = "old", timestamp = System.currentTimeMillis() - 5000))
        cache.put(createCommand(id = "new", timestamp = System.currentTimeMillis()))

        val all = cache.getAll()
        assertEquals(1, all.size)
        assertEquals("new", all[0].id)
    }

    @Test
    fun `getByType evicts expired entries`() {
        cache.configure(VoiceCommandCache.CacheConfig(maxSize = 50, expiryMs = 1000L))

        cache.put(createCommand(id = "ot", type = VoiceCommandType.SAVE, timestamp = System.currentTimeMillis() - 5000))
        cache.put(createCommand(id = "nt", type = VoiceCommandType.SAVE, timestamp = System.currentTimeMillis()))

        val saves = cache.getByType(VoiceCommandType.SAVE)
        assertEquals(1, saves.size)
        assertEquals("nt", saves[0].id)
    }

    // ==================== configure 测试 ====================

    @Test
    fun `configure updates maxSize and expiry`() {
        cache.configure(VoiceCommandCache.CacheConfig(maxSize = 5, expiryMs = 10000L))

        // Fill beyond new maxSize
        repeat(7) { cache.put(createCommand(id = "cfg-$it")) }
        assertEquals(5, cache.size())
    }

    // ==================== LRU 行为测试 ====================

    @Test
    fun `accessOrder LRU - get promotes entry`() {
        cache.configure(VoiceCommandCache.CacheConfig(maxSize = 3, expiryMs = Long.MAX_VALUE))

        cache.put(createCommand(id = "a", type = VoiceCommandType.SAVE))
        cache.put(createCommand(id = "b", type = VoiceCommandType.UNDO))
        cache.put(createCommand(id = "c", type = VoiceCommandType.HELP))

        // Access "a" to promote it (it's now most recently used)
        cache.get("a")

        // Adding a new entry should evict the least recently used ("b")
        cache.put(createCommand(id = "d", type = VoiceCommandType.PAUSE))

        assertNull(cache.get("b")) // "b" should be evicted
        assertNotNull(cache.get("a")) // "a" should still be present
    }
}
