package com.novelapp.aiagent.di

import android.content.Context
import androidx.room.Room
import com.novelapp.aiagent.data.local.AppDatabase
import com.novelapp.aiagent.data.local.ChapterDao
import com.novelapp.aiagent.data.local.CharacterDao
import com.novelapp.aiagent.data.local.NovelDao
import com.novelapp.aiagent.data.local.WorldSettingDao
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
            .addMigrations(AppDatabase.MIGRATION_1_2)
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

    @Provides
    @Singleton
    fun provideCharacterDao(database: AppDatabase): CharacterDao {
        return database.characterDao()
    }

    @Provides
    @Singleton
    fun provideWorldSettingDao(database: AppDatabase): WorldSettingDao {
        return database.worldSettingDao()
    }
}
