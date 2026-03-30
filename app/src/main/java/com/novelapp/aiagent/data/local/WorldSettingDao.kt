package com.novelapp.aiagent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 世界观设定数据访问对象
 */
@Dao
interface WorldSettingDao {

    @Query("SELECT * FROM world_settings WHERE novelId = :novelId ORDER BY category, key")
    suspend fun getWorldSettingsByNovelId(novelId: String): List<WorldSettingEntity>

    @Query("SELECT * FROM world_settings WHERE novelId = :novelId ORDER BY category, key")
    fun getWorldSettingsByNovelIdFlow(novelId: String): Flow<List<WorldSettingEntity>>

    @Query("SELECT * FROM world_settings WHERE id = :settingId")
    suspend fun getSettingById(settingId: String): WorldSettingEntity?

    @Query("SELECT * FROM world_settings WHERE novelId = :novelId AND category = :category")
    suspend fun getSettingsByCategory(novelId: String, category: String): List<WorldSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: WorldSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<WorldSettingEntity>)

    @Update
    suspend fun updateSetting(setting: WorldSettingEntity)

    @Query("DELETE FROM world_settings WHERE id = :settingId")
    suspend fun deleteById(settingId: String)

    @Query("DELETE FROM world_settings WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: String)

    @Query("SELECT COUNT(*) FROM world_settings WHERE novelId = :novelId")
    suspend fun getSettingCount(novelId: String): Int
}
