package com.novelapp.aiagent.di

import android.content.Context
import androidx.room.Room
import com.novelapp.aiagent.data.local.AppDatabase
import com.novelapp.aiagent.data.local.ChapterDao
import com.novelapp.aiagent.data.local.NovelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块
 *
 * 提供Room数据库和相关DAO的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // 允许主线程查询（仅用于简单查询，生产环境应移除）
            // .allowMainThreadQueries()
            // 数据库迁移策略
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideNovelDao(database: AppDatabase): NovelDao {
        return database.novelDao()
    }

    @Provides
    @Singleton
    fun provideChapterDao(database: AppDatabase): ChapterDao {
        return database.chapterDao()
    }
}
