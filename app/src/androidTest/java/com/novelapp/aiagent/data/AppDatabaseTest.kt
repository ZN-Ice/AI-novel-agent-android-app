package com.novelapp.aiagent.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.novelapp.aiagent.data.local.AppDatabase
import com.novelapp.aiagent.data.local.ChapterEntity
import com.novelapp.aiagent.data.local.NovelEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room数据库集成测试
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== 小说DAO测试 ====================

    @Test
    fun insertNovel_and_getNovelById() = runTest {
        val novel = NovelEntity(
            id = "test-id",
            title = "测试小说",
            genre = "玄幻"
        )

        database.novelDao().insertNovel(novel)
        val result = database.novelDao().getNovelById("test-id")

        assertNotNull(result)
        assertEquals("测试小说", result?.title)
        assertEquals("玄幻", result?.genre)
    }

    @Test
    fun getAllNovels_returnsOnlyNotDeleted() = runTest {
        // 插入两条小说
        database.novelDao().insertNovel(
            NovelEntity(id = "1", title = "小说1")
        )
        database.novelDao().insertNovel(
            NovelEntity(id = "2", title = "小说2", isDeleted = true)
        )

        val novels = database.novelDao().getAllNovels().first()

        assertEquals(1, novels.size)
        assertEquals("小说1", novels[0].title)
    }

    @Test
    fun softDelete_and_restore() = runTest {
        val novel = NovelEntity(id = "test-id", title = "测试小说")
        database.novelDao().insertNovel(novel)

        // 软删除
        database.novelDao().softDelete("test-id")
        val deleted = database.novelDao().getNovelById("test-id")
        assertTrue(deleted?.isDeleted == true)

        // 恢复
        database.novelDao().restore("test-id")
        val restored = database.novelDao().getNovelById("test-id")
        assertFalse(restored?.isDeleted == true)
    }

    @Test
    fun existsByTitle_returnsCorrectCount() = runTest {
        database.novelDao().insertNovel(
            NovelEntity(id = "1", title = "已存在小说")
        )

        val exists = database.novelDao().existsByTitle("已存在小说")
        val notExists = database.novelDao().existsByTitle("不存在的小说")

        assertEquals(1, exists)
        assertEquals(0, notExists)
    }

    @Test
    fun searchNovels_returnsMatchingResults() = runTest {
        database.novelDao().insertNovel(NovelEntity(id = "1", title = "玄幻大作"))
        database.novelDao().insertNovel(NovelEntity(id = "2", title = "都市传说"))
        database.novelDao().insertNovel(NovelEntity(id = "3", title = "科幻世界"))

        val results = database.novelDao().searchNovels("玄幻").first()

        assertEquals(1, results.size)
        assertEquals("玄幻大作", results[0].title)
    }

    // ==================== 章节DAO测试 ====================

    @Test
    fun insertChapter_and_getChapterById() = runTest {
        // 先创建小说
        database.novelDao().insertNovel(NovelEntity(id = "novel-1", title = "测试小说"))

        val chapter = ChapterEntity(
            id = "chapter-1",
            novelId = "novel-1",
            title = "第一章",
            chapterNumber = 1
        )

        database.chapterDao().insertChapter(chapter)
        val result = database.chapterDao().getChapterById("chapter-1")

        assertNotNull(result)
        assertEquals("第一章", result?.title)
        assertEquals(1, result?.chapterNumber)
    }

    @Test
    fun getChaptersByNovelId_returnsOrderedByNumber() = runTest {
        database.novelDao().insertNovel(NovelEntity(id = "novel-1", title = "测试小说"))

        // 乱序插入
        database.chapterDao().insertChapter(
            ChapterEntity(id = "3", novelId = "novel-1", title = "第三章", chapterNumber = 3)
        )
        database.chapterDao().insertChapter(
            ChapterEntity(id = "1", novelId = "novel-1", title = "第一章", chapterNumber = 1)
        )
        database.chapterDao().insertChapter(
            ChapterEntity(id = "2", novelId = "novel-1", title = "第二章", chapterNumber = 2)
        )

        val chapters = database.chapterDao().getChaptersByNovelId("novel-1").first()

        assertEquals(3, chapters.size)
        assertEquals(listOf(1, 2, 3), chapters.map { it.chapterNumber })
    }

    @Test
    fun updateChapterContent_updatesWordCount() = runTest {
        database.novelDao().insertNovel(NovelEntity(id = "novel-1", title = "测试小说"))
        database.chapterDao().insertChapter(
            ChapterEntity(id = "chapter-1", novelId = "novel-1", title = "第一章")
        )

        val content = "这是一段测试内容"
        database.chapterDao().updateContent("chapter-1", content, content.length)

        val updated = database.chapterDao().getChapterById("chapter-1")
        assertEquals(content, updated?.content)
        assertEquals(content.length, updated?.wordCount)
    }

    @Test
    fun getRecentChapters_returnsLastN() = runTest {
        database.novelDao().insertNovel(NovelEntity(id = "novel-1", title = "测试小说"))

        for (i in 1..5) {
            database.chapterDao().insertChapter(
                ChapterEntity(id = "chapter-$i", novelId = "novel-1", title = "第${i}章", chapterNumber = i)
            )
        }

        val recent = database.chapterDao().getRecentChapters("novel-1", limit = 3)

        assertEquals(3, recent.size)
        assertEquals(listOf(5, 4, 3), recent.map { it.chapterNumber })
    }

    @Test
    fun getTotalWordCount_returnsCorrectSum() = runTest {
        database.novelDao().insertNovel(NovelEntity(id = "novel-1", title = "测试小说"))
        database.chapterDao().insertChapter(
            ChapterEntity(id = "1", novelId = "novel-1", title = "第一章", wordCount = 1000)
        )
        database.chapterDao().insertChapter(
            ChapterEntity(id = "2", novelId = "novel-1", title = "第二章", wordCount = 2000)
        )

        val total = database.chapterDao().getTotalWordCount("novel-1")

        assertEquals(3000L, total)
    }

    @Test
    fun deleteByNovelId_cascadesDelete() = runTest {
        database.novelDao().insertNovel(NovelEntity(id = "novel-1", title = "测试小说"))
        database.chapterDao().insertChapter(
            ChapterEntity(id = "chapter-1", novelId = "novel-1", title = "第一章")
        )

        database.chapterDao().deleteByNovelId("novel-1")

        val chapters = database.chapterDao().getChaptersByNovelId("novel-1").first()
        assertTrue(chapters.isEmpty())
    }
}
