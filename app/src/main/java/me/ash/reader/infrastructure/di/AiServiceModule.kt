package me.ash.reader.infrastructure.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ash.reader.domain.data.SyncLogger
import me.ash.reader.domain.service.ai.AiService
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiServiceModule {
    @Provides
    @Singleton
    fun provideAiService(
        okHttpClient: OkHttpClient,
        syncLogger: SyncLogger,
    ): AiService {
        return AiService(okHttpClient, syncLogger)
    }
}