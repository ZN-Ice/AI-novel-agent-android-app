package com.novelapp.aiagent.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.novelapp.aiagent.model.Novel
import com.novelapp.aiagent.model.NovelStatus

/**
 * 小说数据库实体
 */
@Entity(
    tableName = "novels",
    indices = [
        Index(value = ["title"], unique = true),
        Index(value = ["updatedAt"]),
        Index(value = ["isDeleted"])
    ]
)
data class NovelEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val genre: String = "",
    val description: String = "",
    val coverUrl: String? = null,
    val chapterCount: Int = 0,
    val wordCount: Long = 0L,
    val status: String = NovelStatus.DRAFT.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) {
    /**
     * 转换为领域模型
     */
    fun toModel(): Novel {
        return Novel(
            id = id,
            title = title,
            genre = genre,
            description = description,
            coverUrl = coverUrl,
            chapterCount = chapterCount,
            wordCount = wordCount,
            status = NovelStatus.valueOf(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            deletedAt = deletedAt
        )
    }

    companion object {
        /**
         * 从领域模型创建实体
         */
        fun fromModel(model: Novel): NovelEntity {
            return NovelEntity(
                id = model.id,
                title = model.title,
                genre = model.genre,
                description = model.description,
                coverUrl = model.coverUrl,
                chapterCount = model.chapterCount,
                wordCount = model.wordCount,
                status = model.status.name,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                isDeleted = model.isDeleted,
                deletedAt = model.deletedAt
            )
        }
    }
}
