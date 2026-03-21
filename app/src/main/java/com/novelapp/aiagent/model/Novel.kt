package com.novelapp.aiagent.model

/**
 * 小说实体
 *
 * @property id 小说唯一标识
 * @property title 小说标题
 * @property genre 小说类型（玄幻/都市/科幻等）
 * @property description 简介
 * @property coverUrl 封面图片URL
 * @property chapterCount 章节数量
 * @property wordCount 总字数
 * @property status 创作状态
 * @property createdAt 创建时间
 * @property updatedAt 最后更新时间
 * @property isDeleted 是否已删除（软删除）
 * @property deletedAt 删除时间
 */
data class Novel(
    val id: String,
    val title: String,
    val genre: String = "",
    val description: String = "",
    val coverUrl: String? = null,
    val chapterCount: Int = 0,
    val wordCount: Long = 0L,
    val status: NovelStatus = NovelStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) {
    /**
     * 是否已超过软删除期限（7天）
     */
    fun isExpired(): Boolean {
        if (deletedAt == null) return false
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - deletedAt > sevenDaysInMillis
    }

    /**
     * 格式化字数显示
     */
    fun getFormattedWordCount(): String {
        return when {
            wordCount >= 10000 -> String.format("%.1f万", wordCount / 10000.0)
            else -> wordCount.toString()
        }
    }

    /**
     * 格式化时间显示
     */
    fun getFormattedUpdateTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - updatedAt

        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(updatedAt))
        }
    }
}

/**
 * 小说状态
 */
enum class NovelStatus(val label: String) {
    DRAFT("草稿"),
    ONGOING("连载中"),
    COMPLETED("已完结"),
    SUSPENDED("已暂停")
}

/**
 * 小说类型
 */
enum class NovelGenre(val label: String) {
    XUANHUAN("玄幻"),
    QIHUAN("奇幻"),
    WUXIA("武侠"),
    XIANXIA("仙侠"),
    DUSHI("都市"),
    LISHI("历史"),
    JUNSHI("军事"),
    KEHUAN("科幻"),
    LINGYI("灵异"),
    OTHER("其他");

    companion object {
        fun fromLabel(label: String): NovelGenre {
            return entries.find { it.label == label } ?: OTHER
        }
    }
}
