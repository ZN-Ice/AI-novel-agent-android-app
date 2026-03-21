package com.novelapp.aiagent.model

/**
 * 角色实体
 *
 * @property id 角色唯一标识
 * @property novelId 所属小说ID
 * @property name 角色名称
 * @property role 角色定位（主角/配角/反派等）
 * @property description 外貌描述
 * @property personality 性格特点
 * @property background 背景故事
 * @property abilities 能力/技能
 * @property relationships 人物关系
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
data class Character(
    val id: String,
    val novelId: String,
    val name: String,
    val role: CharacterRole = CharacterRole.SUPPORTING,
    val description: String = "",
    val personality: String = "",
    val background: String = "",
    val abilities: List<String> = emptyList(),
    val relationships: List<CharacterRelationship> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 角色定位
 */
enum class CharacterRole(val label: String) {
    PROTAGONIST("主角"),
    DEUTERAGONIST("第二主角"),
    SUPPORTING("配角"),
    ANTAGONIST("反派"),
    MINOR("龙套");

    companion object {
        fun fromLabel(label: String): CharacterRole {
            return entries.find { it.label == label } ?: SUPPORTING
        }
    }
}

/**
 * 人物关系
 */
data class CharacterRelationship(
    val targetCharacterId: String,
    val targetCharacterName: String,
    val relationship: String,
    val description: String = ""
)

/**
 * 角色简要信息（用于上下文拼接）
 */
data class CharacterInfo(
    val name: String,
    val role: String,
    val description: String,
    val personality: String,
    val abilities: List<String>
) {
    /**
     * 转换为上下文格式
     */
    fun toContextString(): String {
        val sb = StringBuilder()
        sb.appendLine("【$name】($role)")
        if (description.isNotBlank()) {
            sb.appendLine("外貌：$description")
        }
        if (personality.isNotBlank()) {
            sb.appendLine("性格：$personality")
        }
        if (abilities.isNotEmpty()) {
            sb.appendLine("能力：${abilities.joinToString("、")}")
        }
        return sb.toString()
    }
}

/**
 * 角色列表项（用于UI显示）
 */
data class CharacterListItem(
    val id: String,
    val name: String,
    val role: CharacterRole,
    val description: String
)
