package com.novelapp.aiagent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 角色数据访问对象
 */
@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters WHERE novelId = :novelId ORDER BY role, name")
    suspend fun getCharactersByNovelId(novelId: String): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE novelId = :novelId ORDER BY role, name")
    fun getCharactersByNovelIdFlow(novelId: String): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :characterId")
    suspend fun getCharacterById(characterId: String): CharacterEntity?

    @Query("SELECT * FROM characters WHERE novelId = :novelId AND role = :role")
    suspend fun getCharactersByRole(novelId: String, role: String): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<CharacterEntity>)

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :characterId")
    suspend fun deleteById(characterId: String)

    @Query("DELETE FROM characters WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: String)

    @Query("SELECT COUNT(*) FROM characters WHERE novelId = :novelId")
    suspend fun getCharacterCount(novelId: String): Int
}
