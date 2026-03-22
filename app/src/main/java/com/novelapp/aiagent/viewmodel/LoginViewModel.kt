package com.novelapp.aiagent.viewmodel

import androidx.lifecycle.viewModelScope
import com.novelapp.aiagent.ai.config.*
import com.novelapp.aiagent.ai.config.ModelConfigManager
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
 * 登录流程：
 * 1. 选择模型方案（ModelProviderType）
 * 2. 选择模型类型（GLMModel）
 * 3. 输入API Key
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

    // ==================== 登录步骤状态 ====================

    // 当前步骤（1: 选择方案, 2: 选择模型, 3: 输入API Key）
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // 可用的提供商列表
    private val _availableProviders = MutableStateFlow<List<ProviderInfo>>(emptyList())
    val availableProviders: StateFlow<List<ProviderInfo>> = _availableProviders.asStateFlow()

    // 已选择的提供商
    private val _selectedProvider = MutableStateFlow<ModelProviderType?>(null)
    val selectedProvider: StateFlow<ModelProviderType?> = _selectedProvider.asStateFlow()

    // 可用的模型列表（基于选择的提供商）
    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels.asStateFlow()

    // 已选择的模型
    private val _selectedModel = MutableStateFlow<GLMModel?>(null)
    val selectedModel: StateFlow<GLMModel?> = _selectedModel.asStateFlow()

    // API Key输入
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // 自定义URL（可选）
    private val _customBaseUrl = MutableStateFlow("")
    val customBaseUrl: StateFlow<String> = _customBaseUrl.asStateFlow()

    // ==================== UI状态 ====================

    // 登录状态
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 表单验证状态
    private val _formValidation = MutableStateFlow<FormValidation>(FormValidation.Valid)
    val formValidation: StateFlow<FormValidation> = _formValidation.asStateFlow()

    init {
        loadAvailableProviders()
        checkExistingLogin()
    }

    /**
     * 加载可用的提供商
     */
    private fun loadAvailableProviders() {
        val providers = ModelProviderType.getSupportedProviders()
        _availableProviders.value = providers
        Timber.d("Loaded ${providers.size} providers")
    }

    /**
     * 检查是否已登录
     */
    private fun checkExistingLogin() {
        if (modelConfigManager.isLoggedIn()) {
            val config = modelConfigManager.getConfig()
            if (config != null) {
                _uiState.value = LoginUiState.AlreadyLoggedIn(config)
                Timber.i("User already logged in with ${config.getDisplayName()}")
            }
        }
    }

    // ==================== 步骤1: 选择提供商 ====================

    /**
     * 选择模型方案
     */
    fun selectProvider(provider: ModelProviderType) {
        _selectedProvider.value = provider
        _availableModels.value = provider.supportedModels
        _selectedModel.value = null // 重置模型选择

        Timber.i("Provider selected: ${provider.displayName}")
        moveToStep(2)
    }

    // ==================== 步骤2: 选择模型 ====================

    /**
     * 选择模型类型
     */
    fun selectModel(modelId: String) {
        val model = GLMModel.fromModelId(modelId)
        if (model != null) {
            _selectedModel.value = model
            Timber.i("Model selected: ${model.displayName}")
            moveToStep(3)
        } else {
            _formValidation.value = FormValidation.Invalid(listOf("无效的模型类型"))
        }
    }

    /**
     * 选择GLM模型
     */
    fun selectGLMModel(model: GLMModel) {
        _selectedModel.value = model
        Timber.i("GLM Model selected: ${model.displayName}")
        moveToStep(3)
    }

    // ==================== 步骤3: 输入API Key ====================

    /**
     * 更新API Key
     */
    fun updateApiKey(key: String) {
        _apiKey.value = key
        validateCurrentStep()
    }

    /**
     * 更新自定义URL
     */
    fun updateCustomBaseUrl(url: String) {
        _customBaseUrl.value = url
    }

    /**
     * 执行登录
     */
    fun login() {
        val provider = _selectedProvider.value
        val model = _selectedModel.value
        val key = _apiKey.value.trim()
        val customUrl = _customBaseUrl.value.trim().takeIf { it.isNotBlank() }

        // 验证
        if (provider == null) {
            _formValidation.value = FormValidation.Invalid(listOf("请选择模型方案"))
            return
        }

        if (provider == ModelProviderType.GLM_CODEPLAN && model == null) {
            _formValidation.value = FormValidation.Invalid(listOf("请选择模型类型"))
            return
        }

        if (key.isBlank()) {
            _formValidation.value = FormValidation.Invalid(listOf("请输入API密钥"))
            return
        }

        if (key.length < 10) {
            _formValidation.value = FormValidation.Invalid(listOf("API密钥格式不正确"))
            return
        }

        // 构建登录配置
        val loginConfig = LoginConfig(
            providerType = provider,
            glmModel = model,
            apiKey = key,
            customBaseUrl = customUrl
        )

        // 验证配置
        val validation = loginConfig.validate()
        if (validation.isError) {
            _formValidation.value = FormValidation.Invalid(
                listOf(validation.firstError ?: "验证失败")
            )
            return
        }

        // 执行登录
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            _formValidation.value = FormValidation.Valid

            try {
                val result = modelConfigManager.login(loginConfig)

                when (result) {
                    is LoginResult.Success -> {
                        _uiState.value = LoginUiState.Success(result.config)
                        Timber.i("Login successful")
                    }
                    is LoginResult.ValidationFailed -> {
                        _uiState.value = LoginUiState.Error(result.message)
                        _formValidation.value = FormValidation.Invalid(listOf(result.message))
                        Timber.w("Login validation failed: ${result.message}")
                    }
                    is LoginResult.SaveFailed -> {
                        _uiState.value = LoginUiState.Error(result.message)
                        Timber.e("Login save failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "登录失败")
                Timber.e(e, "Login error")
            }
        }
    }

    // ==================== 导航控制 ====================

    /**
     * 移动到指定步骤
     */
    fun moveToStep(step: Int) {
        if (step in 1..3) {
            _currentStep.value = step
            _formValidation.value = FormValidation.Valid
            Timber.d("Moved to step $step")
        }
    }

    /**
     * 返回上一步
     */
    fun goToPreviousStep() {
        val current = _currentStep.value
        if (current > 1) {
            _currentStep.value = current - 1
            _formValidation.value = FormValidation.Valid
        }
    }

    /**
     * 重置登录流程
     */
    fun reset() {
        _currentStep.value = 1
        _selectedProvider.value = null
        _selectedModel.value = null
        _apiKey.value = ""
        _customBaseUrl.value = ""
        _availableModels.value = emptyList()
        _uiState.value = LoginUiState.Idle
        _formValidation.value = FormValidation.Valid
        Timber.i("Login flow reset")
    }

    /**
     * 登出
     */
    fun logout() {
        modelConfigManager.logout()
        reset()
        Timber.i("User logged out")
    }

    // ==================== 验证 ====================

    /**
     * 验证当前步骤
     */
    private fun validateCurrentStep() {
        val errors = mutableListOf<String>()
        val step = _currentStep.value

        when (step) {
            1 -> {
                if (_selectedProvider.value == null) {
                    errors.add("请选择模型方案")
                }
            }
            2 -> {
                if (_selectedProvider.value == ModelProviderType.GLM_CODEPLAN && _selectedModel.value == null) {
                    errors.add("请选择模型类型")
                }
            }
            3 -> {
                val key = _apiKey.value.trim()
                if (key.isBlank()) {
                    errors.add("请输入API密钥")
                } else if (key.length < 10) {
                    errors.add("API密钥格式不正确")
                }

                val customUrl = _customBaseUrl.value.trim()
                if (customUrl.isNotBlank() && !customUrl.startsWith("https://")) {
                    errors.add("API地址必须使用HTTPS")
                }
            }
        }

        _formValidation.value = if (errors.isEmpty()) {
            FormValidation.Valid
        } else {
            FormValidation.Invalid(errors)
        }
    }

    /**
     * 检查是否可以进入下一步
     */
    fun canProceed(): Boolean {
        return when (_currentStep.value) {
            1 -> _selectedProvider.value != null
            2 -> {
                val provider = _selectedProvider.value
                if (provider == ModelProviderType.GLM_CODEPLAN) {
                    _selectedModel.value != null
                } else {
                    provider != null
                }
            }
            3 -> {
                val key = _apiKey.value.trim()
                key.isNotBlank() && key.length >= 10
            }
            else -> false
        }
    }
}

/**
 * 登录UI状态
 */
sealed class LoginUiState {
    /** 空闲状态 */
    object Idle : LoginUiState()

    /** 加载中 */
    object Loading : LoginUiState()

    /** 登录成功 */
    data class Success(val config: ModelConfig) : LoginUiState()

    /** 登录失败 */
    data class Error(val message: String) : LoginUiState()

    /** 已登录 */
    data class AlreadyLoggedIn(val config: ModelConfig) : LoginUiState()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isAlreadyLoggedIn: Boolean get() = this is AlreadyLoggedIn
}

/**
 * 表单验证状态
 */
sealed class FormValidation {
    /** 验证通过 */
    object Valid : FormValidation()

    /** 验证失败 */
    data class Invalid(val errors: List<String>) : FormValidation()

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid
    val firstError: String?
        get() = when (this) {
            is Invalid -> errors.firstOrNull() ?: "验证失败"
            is Valid -> null
        }
}
