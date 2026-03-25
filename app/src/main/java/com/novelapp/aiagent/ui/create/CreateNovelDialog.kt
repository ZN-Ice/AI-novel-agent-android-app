package com.novelapp.aiagent.ui.create

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.novelapp.aiagent.R
import com.novelapp.aiagent.databinding.DialogCreateNovelBinding

/**
 * 新建小说弹窗
 *
 * 命名规范遵循 AGENTS.md 2.1节
 * 流程规范遵循 AGENTS.md 5.1节
 *
 * @see AGENTS.md 5.1节 新建小说流程
 */
class CreateNovelDialog : DialogFragment() {

    companion object {
        const val TAG = "CreateNovelDialog"

        fun newInstance(): CreateNovelDialog {
            return CreateNovelDialog()
        }
    }

    private var _binding: DialogCreateNovelBinding? = null
    private val binding get() = _binding!!

    // 回调接口
    private var onCreateListener: ((title: String, genre: String) -> Unit)? = null

    // 选中的类型
    private var selectedGenre: String = ""

    fun setOnCreateListener(listener: (String, String) -> Unit) {
        onCreateListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateNovelBinding.inflate(LayoutInflater.from(requireContext()))

        // 设置类型选择监听
        setupGenreSelection()

        // 设置输入监听
        setupInputListener()

        // 设置按钮点击事件
        setupClickListeners()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(true)
            .create()
    }

    /**
     * 设置类型选择监听
     */
    private fun setupGenreSelection() {
        // 默认选中玄幻
        binding.rbXuanhuan.isChecked = true
        selectedGenre = getString(R.string.create_genre_xuanhuan)

        // 监听类型选择变化
        val genreButtons = listOf(
            binding.rbXuanhuan to R.string.create_genre_xuanhuan,
            binding.rbDushi to R.string.create_genre_dushi,
            binding.rbKehuan to R.string.create_genre_kehuan,
            binding.rbWuxia to R.string.create_genre_wuxia,
            binding.rbQihuan to R.string.create_genre_qihuan,
            binding.rbLishi to R.string.create_genre_lishi
        )

        genreButtons.forEach { (button, stringRes) ->
            button.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedGenre = getString(stringRes)
                }
            }
        }
    }

    /**
     * 设置输入监听
     */
    private fun setupInputListener() {
        binding.etNovelName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 验证输入
                validateInput()
            }
        })
    }

    /**
     * 验证输入
     */
    private fun validateInput() {
        val name = binding.etNovelName.text.toString().trim()

        // 验证名称是否为空
        if (name.isEmpty()) {
            binding.etNovelName.error = getString(R.string.error_name_empty)
            binding.btnConfirm.isEnabled = false
            return
        }

        // 验证名称长度
        if (name.length > 50) {
            binding.etNovelName.error = getString(R.string.error_name_too_long)
            binding.btnConfirm.isEnabled = false
            return
        }

        // 验证通过
        binding.etNovelName.error = null
        binding.btnConfirm.isEnabled = true
    }

    /**
     * 设置按钮点击事件
     */
    private fun setupClickListeners() {
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // 确认按钮
        binding.btnConfirm.setOnClickListener {
            val name = binding.etNovelName.text.toString().trim()

            if (name.isNotEmpty() && name.length <= 50) {
                onCreateListener?.invoke(name, selectedGenre)
                dismiss()
            }
        }
    }

    /**
     * 设置语音输入的名称
     */
    fun setNovelName(name: String) {
        binding.etNovelName.setText(name)
    }

    /**
     * 显示语音提示
     */
    fun showVoiceHint() {
        binding.tvVoiceHint.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
