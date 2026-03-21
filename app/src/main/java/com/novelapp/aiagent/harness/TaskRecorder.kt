package com.novelapp.aiagent.harness

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 任务记录器
 *
 * 职责：
 * - 记录研发任务
 * - 任务状态跟踪
 * - 任务历史查询
 * - 自动写入docs/task/task_records.json
 */
@Singleton
class TaskRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // 任务记录文件
    private val taskFile: File by lazy {
        // 优先使用项目docs目录，如果不存在则使用应用缓存目录
        val projectDocsDir = File(context.getExternalFilesDir(null), "docs/task")
        if (projectDocsDir.exists() || projectDocsDir.mkdirs()) {
            File(projectDocsDir, "task_records.json")
        } else {
            File(context.cacheDir, "task_records.json")
        }
    }

    /**
     * 创建新任务
     * @param type 任务类型
     * @param description 任务描述
     * @param module 所属模块
     * @param executor 执行者
     * @return 任务ID
     */
    suspend fun createTask(
        type: TaskType,
        description: String,
        module: String,
        executor: TaskExecutor = TaskExecutor.CLAUDE_CODE
    ): Result<TaskRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val taskId = generateTaskId()
                val now = dateFormat.format(Date())

                val task = TaskRecord(
                    taskId = taskId,
                    taskType = type.label,
                    taskDesc = description,
                    module = module,
                    executor = executor.label,
                    status = TaskStatus.PENDING.label,
                    createTime = now,
                    finishTime = null,
                    checkResult = null,
                    remark = null
                )

                // 读取现有记录
                val records = readRecords().toMutableList()
                records.add(task)

                // 写入文件
                writeRecords(records)

                Result.success(task)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 新状态
     * @param checkResult 校验结果
     * @param remark 备注
     */
    suspend fun updateTaskStatus(
        taskId: String,
        status: TaskStatus,
        checkResult: String? = null,
        remark: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val records = readRecords().toMutableList()
                val index = records.indexOfFirst { it.taskId == taskId }

                if (index == -1) {
                    return@withContext Result.failure(Exception("任务不存在: $taskId"))
                }

                val oldTask = records[index]
                val updatedTask = oldTask.copy(
                    status = status.label,
                    finishTime = if (status.isFinished) dateFormat.format(Date()) else oldTask.finishTime,
                    checkResult = checkResult ?: oldTask.checkResult,
                    remark = remark ?: oldTask.remark
                )

                records[index] = updatedTask
                writeRecords(records)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 获取所有任务记录
     */
    suspend fun getAllTasks(): Result<List<TaskRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(readRecords())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 根据状态获取任务
     */
    suspend fun getTasksByStatus(status: TaskStatus): Result<List<TaskRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                val records = readRecords().filter { it.status == status.label }
                Result.success(records)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 获取今日任务统计
     */
    suspend fun getTodayStats(): Result<TaskStats> {
        return withContext(Dispatchers.IO) {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val records = readRecords().filter { it.createTime.startsWith(today) }

                val stats = TaskStats(
                    total = records.size,
                    pending = records.count { it.status == TaskStatus.PENDING.label },
                    inProgress = records.count { it.status == TaskStatus.IN_PROGRESS.label },
                    completed = records.count { it.status == TaskStatus.COMPLETED.label },
                    rejected = records.count { it.status == TaskStatus.REJECTED.label }
                )

                Result.success(stats)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 生成任务ID
     * 格式：T + 日期 + 3位序号
     */
    private suspend fun generateTaskId(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val records = readRecords()
        val todayTasks = records.count { it.taskId.startsWith("T$dateStr") }
        val sequence = (todayTasks + 1).toString().padStart(3, '0')
        return "T$dateStr$sequence"
    }

    /**
     * 读取记录文件
     */
    private fun readRecords(): List<TaskRecord> {
        if (!taskFile.exists()) {
            return emptyList()
        }

        return try {
            val content = taskFile.readText()
            if (content.isBlank()) {
                emptyList()
            } else {
                json.decodeFromString<List<TaskRecord>>(content)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 写入记录文件
     */
    private fun writeRecords(records: List<TaskRecord>) {
        taskFile.parentFile?.mkdirs()
        val content = json.encodeToString(records)
        taskFile.writeText(content)
    }
}

/**
 * 任务记录
 */
@Serializable
data class TaskRecord(
    val taskId: String,
    val taskType: String,
    val taskDesc: String,
    val module: String,
    val executor: String,
    val status: String,
    val createTime: String,
    val finishTime: String?,
    val checkResult: String?,
    val remark: String?
)

/**
 * 任务类型
 */
enum class TaskType(val label: String) {
    FEATURE("功能开发"),
    BUG_FIX("BUG修复"),
    OPTIMIZATION("迭代优化"),
    REFACTOR("代码重构"),
    DOCUMENTATION("文档更新"),
    TEST("测试相关")
}

/**
 * 任务状态
 */
enum class TaskStatus(val label: String) {
    PENDING("待执行"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    REJECTED("驳回");

    val isFinished: Boolean
        get() = this == COMPLETED || this == REJECTED
}

/**
 * 任务执行者
 */
enum class TaskExecutor(val label: String) {
    CLAUDE_CODE("Claude Code"),
    MANUAL("人工")
}

/**
 * 任务统计
 */
data class TaskStats(
    val total: Int,
    val pending: Int,
    val inProgress: Int,
    val completed: Int,
    val rejected: Int
)
