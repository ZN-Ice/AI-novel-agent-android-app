package com.novelapp.aiagent.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * 角色数据库实体
 *
 * 存储小说中的角色设定，用于AI上下文构建
 */
@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["novelId"]),
        Index(value = ["novelId", "role"])
    ]
)
@TypeConverters(Converters::class)
data class CharacterEntity(
    @PrimaryKey
    val id: String,
    val novelId: String,
    val name: String,
    val role: String = "SUPPORTING",
    val description: String = "",
    val personality: String = "",
    val background: String = "",
    val abilities: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
