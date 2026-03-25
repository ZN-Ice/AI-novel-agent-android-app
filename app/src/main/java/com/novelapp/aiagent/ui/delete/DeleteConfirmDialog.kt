package com.novelapp.aiagent.ui.delete

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.novelapp.aiagent.R
import com.novelapp.aiagent.databinding.DialogDeleteConfirmBinding
import com.novelapp.aiagent.model.Novel

/**
 * 删除确认弹窗
 *
 * 命名规范遵循 AGENTS.md 2.1节
 * 流程规范遵循 AGENTS.md 5.2节
 *
 * @see AGENTS.md 5.2节 删除小说流程
 */
class DeleteConfirmDialog : DialogFragment() {

    companion object {
        const val TAG = "DeleteConfirmDialog"

        private const val ARG_NOVEL_ID = "novel_id"
        private const val ARG_NOVEL_TITLE = "novel_title"

        fun newInstance(novel: Novel): DeleteConfirmDialog {
            return DeleteConfirmDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOVEL_ID, novel.id)
                    putString(ARG_NOVEL_TITLE, novel.title)
                }
            }
        }
    }

    private var _binding: DialogDeleteConfirmBinding? = null
    private val binding get() = _binding!!

    // 小说信息
    private val novelId: String? by lazy { arguments?.getString(ARG_NOVEL_ID) }
    private val novelTitle: String? by lazy { arguments?.getString(ARG_NOVEL_TITLE) }

    // 回调接口
    private var onConfirmListener: ((novelId: String) -> Unit)? = null

    fun setOnConfirmListener(listener: (String) -> Unit) {
        onConfirmListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDeleteConfirmBinding.inflate(LayoutInflater.from(requireContext()))

        // 设置删除消息
        setupMessage()

        // 设置按钮点击事件
        setupClickListeners()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .create()
    }

    /**
     * 设置删除消息
     */
    private fun setupMessage() {
        val title = novelTitle ?: ""
        binding.tvDeleteMessage.text = getString(R.string.delete_message, title)
    }

    /**
     * 设置按钮点击事件
     */
    private fun setupClickListeners() {
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // 确认删除按钮
        binding.btnConfirm.setOnClickListener {
            novelId?.let { id ->
                onConfirmListener?.invoke(id)
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
