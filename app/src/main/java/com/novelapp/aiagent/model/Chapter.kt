package com.novelapp.aiagent.model

/**
 * 章节实体
 *
 * @property id 章节唯一标识
 * @property novelId 所属小说ID
 * @property title 章节标题
 * @property content 章节内容
 * @property wordCount 字数
 * @property chapterNumber 章节序号
 * @property status 章节状态
 * @property createdAt 创建时间
 * @property updatedAt 最后更新时间
 * @property isPublished 是否已发布
 */
data class Chapter(
    val id: String,
    val novelId: String,
    val title: String,
    val content: String = "",
    val wordCount: Int = 0,
    val chapterNumber: Int = 1,
    val status: ChapterStatus = ChapterStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = false
) {
    /**
     * 计算字数
     */
    fun calculateWordCount(): Int {
        // 中文按字符数，英文按单词数
        val chineseChars = content.count { it.code in 0x4E00..0x9FFF }
        val englishWords = content.split(Regex("\\s+")).count { it.isNotEmpty() && it.all { it.code !in 0x4E00..0x9FFF } }
        return chineseChars + englishWords
    }

    /**
     * 格式化字数显示
     */
    fun getFormattedWordCount(): String {
        return when {
            wordCount >= 10000 -> String.format("%.1f万", wordCount / 10000.0)
            else -> "${wordCount}字"
        }
    }

    /**
     * 获取内容摘要
     * @param maxLength 最大长度
     */
    fun getSummary(maxLength: Int = 100): String {
        if (content.length <= maxLength) return content
        return content.take(maxLength) + "..."
    }
}

/**
 * 章节状态
 */
enum class ChapterStatus(val label: String) {
    DRAFT("草稿"),
    WRITING("写作中"),
    AI_GENERATING("AI生成中"),
    COMPLETED("已完成"),
    PUBLISHED("已发布")
}

/**
 * 章节摘要（用于上下文拼接）
 */
data class ChapterSummary(
    val chapterId: String,
    val title: String,
    val summary: String,
    val keyEvents: List<String> = emptyList()
)

/**
 * 章节列表项（用于UI显示）
 */
data class ChapterListItem(
    val id: String,
    val title: String,
    val wordCount: Int,
    val chapterNumber: Int,
    val status: ChapterStatus,
    val updatedAt: Long,
    val isSelected: Boolean = false
) {
    fun getFormattedWordCount(): String = "${wordCount}字"

    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(updatedAt))
    }
}
