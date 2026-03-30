package com.novelapp.aiagent.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 世界观设定数据库实体
 *
 * 存储小说的世界观设定，用于AI上下文构建
 */
@Entity(
    tableName = "world_settings",
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
        Index(value = ["novelId", "category"])
    ]
)
data class WorldSettingEntity(
    @PrimaryKey
    val id: String,
    val novelId: String,
    val key: String,
    val value: String,
    val category: String = "general",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
