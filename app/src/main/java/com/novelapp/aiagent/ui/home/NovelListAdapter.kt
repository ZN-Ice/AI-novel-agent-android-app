package com.novelapp.aiagent.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.novelapp.aiagent.R
import com.novelapp.aiagent.databinding.NovelListItemBinding
import com.novelapp.aiagent.model.Novel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 小说列表适配器
 *
 * 命名规范遵循 AGENTS.md 2.1节
 *
 * @see AGENTS.md 2.1节 组件命名规范
 */
class NovelListAdapter(
    private val onItemClick: (Novel) -> Unit,
    private val onDeleteClick: (Novel) -> Unit
) : ListAdapter<Novel, NovelListAdapter.NovelViewHolder>(NovelDiffCallback()) {

    companion object {
        private const val TAG = "NovelListAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelViewHolder {
        val binding = NovelListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NovelViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 小说列表项ViewHolder
     */
    class NovelViewHolder(
        private val binding: NovelListItemBinding,
        private val onItemClick: (Novel) -> Unit,
        private val onDeleteClick: (Novel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun bind(novel: Novel) {
            // 设置标题
            binding.tvNovelTitle.text = novel.title

            // 设置类型
            binding.tvNovelGenre.text = novel.genre.ifEmpty {
                itemView.context.getString(R.string.create_genre_xuanhuan)
            }

            // 设置统计信息
            val stats = itemView.context.getString(
                R.string.home_stats,
                novel.chapterCount,
                formatWordCount(novel.wordCount)
            )
            binding.tvNovelStats.text = stats

            // 设置最近编辑时间
            binding.tvNovelTime.text = itemView.context.getString(
                R.string.home_recent_edit,
                formatTime(novel.updatedAt)
            )

            // 设置点击事件
            binding.root.setOnClickListener {
                onItemClick(novel)
            }

            // 设置删除按钮点击事件
            binding.ivDelete.setOnClickListener {
                onDeleteClick(novel)
            }
        }

        /**
         * 格式化字数显示
         */
        private fun formatWordCount(count: Long): String {
            return when {
                count >= 10000 -> String.format("%.1f万", count / 10000.0)
                else -> count.toString()
            }
        }

        /**
         * 格式化时间显示
         */
        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "刚刚"
                diff < 3600_000 -> "${diff / 60_000}分钟前"
                diff < 86400_000 -> "${diff / 3600_000}小时前"
                diff < 604800_000 -> "${diff / 86400_000}天前"
                else -> dateFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * DiffUtil回调
     */
    class NovelDiffCallback : DiffUtil.ItemCallback<Novel>() {
        override fun areItemsTheSame(oldItem: Novel, newItem: Novel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Novel, newItem: Novel): Boolean {
            return oldItem == newItem
        }
    }
}
