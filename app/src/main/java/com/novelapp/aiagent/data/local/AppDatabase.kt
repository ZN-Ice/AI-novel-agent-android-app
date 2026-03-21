package com.novelapp.aiagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 应用数据库
 *
 * 版本历史：
 * - v1: 初始版本，包含novels和chapters表
 */
@Database(
    entities = [
        NovelEntity::class,
        ChapterEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        const val DATABASE_NAME = "ai_novel.db"
    }
}
