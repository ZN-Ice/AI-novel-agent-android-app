package com.novelapp.aiagent.viewmodel

import androidx.lifecycle.viewModelScope
import com.novelapp.aiagent.ai.config.LoginConfig
import com.novelapp.aiagent.ai.config.ModelConfig
import com.novelapp.aiagent.ai.config.ModelConfigManager
import com.novelapp.aiagent.ai.config.ModelType
import com.novelapp.aiagent.ai.config.ModelTypeInfo
import com.novelapp.aiagent.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 登录ViewModel
 *
 * 职责：
 * - 管理登录流程
 * - 处理模型选择
 * - 验证API Key
 * - 保存配置
 *
 * @see AGENTS.md AI接口模块规范
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val modelConfigManager: ModelConfigManager
) : BaseViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    // 登录状态
    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    // 可选的模型类型列表
    private val _availableModels = MutableStateFlow<List<ModelTypeInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelTypeInfo>> = _availableModels.asStateFlow()

    // 当前选择的模型类型
    private val _selectedModelType = MutableStateFlow<ModelType?>(null)
    val selectedModelType: StateFlow<ModelType?> = _selectedModelType.asStateFlow()

    // API Key输入
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // 自定义URL（可选）
    private val _customBaseUrl = MutableStateFlow("")
    val customBaseUrl: StateFlow<String> = _customBaseUrl.asStateFlow()

    // 是否显示高级选项
    private val _showAdvancedOptions = MutableStateFlow(false)
    val showAdvancedOptions: StateFlow<Boolean> = _showAdvancedOptions.asStateFlow()

    // 表单验证状态
    private val _formValidation = MutableStateFlow<FormValidation>(FormValidation.Valid)
    val formValidation: StateFlow<FormValidation> = _formValidation.asStateFlow()

    init {
        loadAvailableModels()
        checkLoginStatus()
    }

    /**
     * 加载可用的模型类型
     */
    private fun loadAvailableModels() {
        _availableModels.value = ModelType.getSupportedModels()

        // 默认选择第一个模型
        if (_availableModels.value.isNotEmpty() && _selectedModelType.value == null) {
            _selectedModelType.value = ModelType.GLM_CODEPLAN
        }
    }

    /**
     * 检查登录状态
     */
    private fun checkLoginStatus() {
        if (modelConfigManager.isLoggedIn()) {
            _loginState.value = LoginUiState.AlreadyLoggedIn(
                modelConfigManager.getConfig()!!
            )
        }
    }

    /**
     * 选择模型类型
     */
    fun selectModelType(modelType: ModelType) {
        _selectedModelType.value = modelType
        clearValidationError()

        // 更新默认URL
        if (_customBaseUrl.value.isBlank()) {
            _customBaseUrl.value = modelType.defaultBaseUrl
        }
    }

    /**
     * 更新API Key
     */
    fun updateApiKey(key: String) {
        _apiKey.value = key
        clearValidationError()
    }

    /**
     * 更新自定义URL
     */
    fun updateCustomBaseUrl(url: String) {
        _customBaseUrl.value = url
        clearValidationError()
    }

    /**
     * 切换高级选项显示
     */
    fun toggleAdvancedOptions() {
        _showAdvancedOptions.value = !_showAdvancedOptions.value
    }

    /**
     * 登录
     */
    fun login() {
        val modelType = _selectedModelType.value
        val apiKey = _apiKey.value.trim()

        // 验证
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            showLoading()

            try {
                val loginConfig = LoginConfig(
                    modelType = modelType!!,
                    apiKey = apiKey,
                    customBaseUrl = _customBaseUrl.value.trim().ifBlank { null }
                )

                val result = modelConfigManager.login(loginConfig)

                hideLoading()

                when (result) {
                    is com.novelapp.aiagent.ai.config.LoginResult.Success -> {
                        Timber.i("Login successful: ${result.config.modelType.displayName}")
                        _loginState.value = LoginUiState.Success(result.config)
                        showToast("登录成功")
                    }
                    is com.novelapp.aiagent.ai.config.LoginResult.ValidationFailed -> {
                        Timber.w("Login validation failed: ${result.message}")
                        _loginState.value = LoginUiState.Error(result.message)
                        showError(result.message)
                    }
                    is com.novelapp.aiagent.ai.config.LoginResult.SaveFailed -> {
                        Timber.e("Login save failed: ${result.message}")
                        _loginState.value = LoginUiState.Error(result.message)
                        showError(result.message)
                    }
                }
            } catch (e: Exception) {
                hideLoading()
                Timber.e(e, "Login error")
                _loginState.value = LoginUiState.Error(e.message ?: "登录失败")
                showError("登录失败: ${e.message}")
            }
        }
    }

    /**
     * 登出
     */
    fun logout() {
        modelConfigManager.logout()
        _loginState.value = LoginUiState.Idle
        _apiKey.value = ""
        _customBaseUrl.value = ""
        clearValidationError()
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _loginState.value = LoginUiState.Idle
        clearValidationError()
    }

    /**
     * 验证表单
     */
    private fun validateForm(): Boolean {
        val errors = mutableListOf<String>()

        // 验证模型选择
        if (_selectedModelType.value == null) {
            errors.add("请选择模型类型")
        }

        // 验证API Key
        val apiKey = _apiKey.value.trim()
        when {
            apiKey.isBlank() -> errors.add("请输入API密钥")
            apiKey.length < 10 -> errors.add("API密钥格式不正确")
        }

        // 验证自定义URL（如果填写）
        val customUrl = _customBaseUrl.value.trim()
        if (customUrl.isNotBlank() && !customUrl.startsWith("https://")) {
            errors.add("自定义URL必须使用HTTPS")
        }

        if (errors.isNotEmpty()) {
            _formValidation.value = FormValidation.Invalid(errors)
            return false
        }

        _formValidation.value = FormValidation.Valid
        return true
    }

    /**
     * 清除验证错误
     */
    private fun clearValidationError() {
        if (_formValidation.value is FormValidation.Invalid) {
            _formValidation.value = FormValidation.Valid
        }
    }

    /**
     * 获取当前模型类型的提示信息
     */
    fun getModelHint(): String {
        return _selectedModelType.value?.let { type ->
            """
            ${type.displayName}
            ${type.description}

            特性：${type.features.joinToString("、") { it.displayName }}
            """.trimIndent()
        } ?: "请选择一个模型类型"
    }

    /**
     * 获取API Key输入提示
     */
    fun getApiKeyHint(): String {
        return _selectedModelType.value?.let {
            "请输入${it.displayName}的API密钥"
        } ?: "请先选择模型类型"
    }

    /**
     * 是否可以登录
     */
    fun canLogin(): Boolean {
        return _selectedModelType.value != null &&
                _apiKey.value.trim().isNotBlank() &&
                _loginState.value !is LoginUiState.Loading
    }
}

/**
 * 登录UI状态
 */
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val config: ModelConfig) : LoginUiState()
    data class AlreadyLoggedIn(val config: ModelConfig) : LoginUiState()
    data class Error(val message: String) : LoginUiState()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isAlreadyLoggedIn: Boolean get() = this is AlreadyLoggedIn
}

/**
 * 表单验证状态
 */
sealed class FormValidation {
    object Valid : FormValidation()
    data class Invalid(val errors: List<String>) : FormValidation() {
        val firstError: String get() = errors.firstOrNull() ?: "验证失败"
    }

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid
}
