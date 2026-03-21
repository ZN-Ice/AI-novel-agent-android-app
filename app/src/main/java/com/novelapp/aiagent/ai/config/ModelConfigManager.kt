package com.novelapp.aiagent.ai.config

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模型配置管理器
 *
 * 职责：
 * - 安全存储API密钥
 * - 管理模型配置
 * - 提供配置变更通知
 *
 * 使用EncryptedSharedPreferences安全存储敏感信息
 */
@Singleton
class ModelConfigManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ModelConfigManager"
        private const val PREFS_FILE_NAME = "ai_model_config"
        private const val KEY_MODEL_CONFIG = "model_config"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    // 加密的SharedPreferences
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 当前配置
    private val _currentConfig = MutableStateFlow<ModelConfig?>(null)
    val currentConfig: StateFlow<ModelConfig?> = _currentConfig.asStateFlow()

    // 登录状态
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        loadConfig()
    }

    /**
     * 加载已保存的配置
     */
    private fun loadConfig() {
        try {
            val isLogged = encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
            _isLoggedIn.value = isLogged

            if (isLogged) {
                val configJson = encryptedPrefs.getString(KEY_MODEL_CONFIG, null)
                if (configJson != null) {
                    val config = json.decodeFromString<ModelConfig>(configJson)
                    _currentConfig.value = config
                    Timber.i("Model config loaded: ${config.modelType.displayName}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load model config")
            _isLoggedIn.value = false
            _currentConfig.value = null
        }
    }

    /**
     * 登录并保存配置
     *
     * @param loginConfig 登录配置
     * @return 是否成功
     */
    fun login(loginConfig: LoginConfig): LoginResult {
        // 验证配置
        val validation = loginConfig.validate()
        if (validation.isError) {
            return LoginResult.ValidationFailed(validation.firstError ?: "验证失败")
        }

        // 转换为完整配置
        val modelConfig = loginConfig.toModelConfig()

        // 验证配置有效性
        if (!modelConfig.isValid()) {
            return LoginResult.ValidationFailed("配置无效")
        }

        return try {
            // 保存配置
            val configJson = json.encodeToString(modelConfig)

            encryptedPrefs.edit()
                .putString(KEY_MODEL_CONFIG, configJson)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply()

            _currentConfig.value = modelConfig
            _isLoggedIn.value = true

            Timber.i("Login successful: ${modelConfig.modelType.displayName}")
            LoginResult.Success(modelConfig)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save model config")
            LoginResult.SaveFailed(e.message ?: "保存配置失败")
        }
    }

    /**
     * 更新配置
     *
     * @param newConfig 新配置
     * @return 是否成功
     */
    fun updateConfig(newConfig: ModelConfig): Boolean {
        if (!newConfig.isValid()) {
            Timber.w("Invalid config")
            return false
        }

        return try {
            val configJson = json.encodeToString(newConfig)

            encryptedPrefs.edit()
                .putString(KEY_MODEL_CONFIG, configJson)
                .apply()

            _currentConfig.value = newConfig
            Timber.i("Config updated: ${newConfig.modelType.displayName}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to update config")
            false
        }
    }

    /**
     * 登出（清除配置）
     */
    fun logout() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_MODEL_CONFIG)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply()

            _currentConfig.value = null
            _isLoggedIn.value = false

            Timber.i("Logged out")
        } catch (e: Exception) {
            Timber.e(e, "Failed to logout")
        }
    }

    /**
     * 获取当前配置（如果未登录返回null）
     */
    fun getConfig(): ModelConfig? {
        return _currentConfig.value
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return _isLoggedIn.value
    }

    /**
     * 获取API密钥（如果已登录）
     */
    fun getApiKey(): String? {
        return _currentConfig.value?.apiKey
    }

    /**
     * 获取基础URL（如果已登录）
     */
    fun getBaseUrl(): String? {
        return _currentConfig.value?.baseUrl
    }

    /**
     * 获取模型名称（如果已登录）
     */
    fun getModelName(): String? {
        return _currentConfig.value?.modelName
    }

    /**
     * 获取模型类型（如果已登录）
     */
    fun getModelType(): ModelType? {
        return _currentConfig.value?.modelType
    }

    /**
     * 检查是否支持某特性
     */
    fun supportsFeature(feature: ModelFeature): Boolean {
        return _currentConfig.value?.modelType?.features?.contains(feature) == true
    }
}

/**
 * 登录结果
 */
sealed class LoginResult {
    data class Success(val config: ModelConfig) : LoginResult()
    data class ValidationFailed(val message: String) : LoginResult()
    data class SaveFailed(val message: String) : LoginResult()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is !Success
    val errorMessage: String?
        get() = when (this) {
            is ValidationFailed -> message
            is SaveFailed -> message
            else -> null
        }
}
