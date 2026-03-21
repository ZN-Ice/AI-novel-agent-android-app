package com.novelapp.aiagent.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.novelapp.aiagent.model.Chapter
import com.novelapp.aiagent.model.ChapterStatus

/**
 * 章节数据库实体
 */
@Entity(
    tableName = "chapters",
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
        Index(value = ["chapterNumber"]),
        Index(value = ["updatedAt"])
    ]
)
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val novelId: String,
    val title: String,
    val content: String = "",
    val wordCount: Int = 0,
    val chapterNumber: Int = 1,
    val status: String = ChapterStatus.DRAFT.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = false
) {
    /**
     * 转换为领域模型
     */
    fun toModel(): Chapter {
        return Chapter(
            id = id,
            novelId = novelId,
            title = title,
            content = content,
            wordCount = wordCount,
            chapterNumber = chapterNumber,
            status = ChapterStatus.valueOf(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPublished = isPublished
        )
    }

    companion object {
        /**
         * 从领域模型创建实体
         */
        fun fromModel(model: Chapter): ChapterEntity {
            return ChapterEntity(
                id = model.id,
                novelId = model.novelId,
                title = model.title,
                content = model.content,
                wordCount = model.wordCount,
                chapterNumber = model.chapterNumber,
                status = model.status.name,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                isPublished = model.isPublished
            )
        }
    }
}
