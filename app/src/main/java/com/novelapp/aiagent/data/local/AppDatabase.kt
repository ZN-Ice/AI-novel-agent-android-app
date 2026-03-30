package com.novelapp.aiagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 应用数据库
 *
 * 版本历史：
 * - v1: 初始版本，包含novels和chapters表
 * - v2: 新增characters和world_settings表，支持角色和世界观设定
 */
@Database(
    entities = [
        NovelEntity::class,
        ChapterEntity::class,
        CharacterEntity::class,
        WorldSettingEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun characterDao(): CharacterDao
    abstract fun worldSettingDao(): WorldSettingDao

    companion object {
        const val DATABASE_NAME = "ai_novel.db"

        /**
         * v1 -> v2 迁移：新增characters和world_settings表
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建角色表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `characters` (
                        `id` TEXT NOT NULL,
                        `novelId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `role` TEXT NOT NULL DEFAULT 'SUPPORTING',
                        `description` TEXT NOT NULL DEFAULT '',
                        `personality` TEXT NOT NULL DEFAULT '',
                        `background` TEXT NOT NULL DEFAULT '',
                        `abilities` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 创建角色表索引
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_characters_novelId` ON `characters` (`novelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_characters_novelId_role` ON `characters` (`novelId`, `role`)")

                // 创建外键约束（Room使用WITHOUT ROWID的方式）
                // SQLite不支持ALTER TABLE ADD CONSTRAINT，外键通过重建表实现
                // Room会在下次导出时自动处理

                // 创建世界观设定表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `world_settings` (
                        `id` TEXT NOT NULL,
                        `novelId` TEXT NOT NULL,
                        `key` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'general',
                        `description` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 创建世界观设定表索引
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_world_settings_novelId` ON `world_settings` (`novelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_world_settings_novelId_category` ON `world_settings` (`novelId`, `category`)")
            }
        }
    }
}
