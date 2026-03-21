package com.novelapp.aiagent.di

import android.content.Context
import com.novelapp.aiagent.ai.AIRepository
import com.novelapp.aiagent.ai.config.ModelConfigManager
import com.novelapp.aiagent.ai.providers.AIProvider
import com.novelapp.aiagent.ai.providers.AIProviderFactory
import com.novelapp.aiagent.ai.providers.GLMProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * AI模块依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideModelConfigManager(
        @ApplicationContext context: Context
    ): ModelConfigManager {
        return ModelConfigManager(context)
    }

    @Provides
    @Singleton
    fun provideAIRepository(
        modelConfigManager: ModelConfigManager
    ): AIRepository {
        return AIRepository(modelConfigManager)
    }

    // 注册AI Providers
    @Provides
    @IntoSet
    fun provideGLMProvider(): AIProvider {
        return GLMProvider()
    }

    @Provides
    @Singleton
    fun provideAIProviderFactory(
        providers: Set<@JvmSuppressWildcards AIProvider>
    ): AIProviderFactory {
        // AIProviderFactory使用object单例，这里确保providers已注册
        providers.forEach { provider ->
            AIProviderFactory.registerProvider(provider)
        }
        return AIProviderFactory
    }
}
