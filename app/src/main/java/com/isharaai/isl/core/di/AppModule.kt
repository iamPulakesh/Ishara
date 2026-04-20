package com.isharaai.isl.core.di

import android.content.Context
import com.isharaai.isl.feature.chat.ChatRepository
import com.isharaai.isl.feature.video.SignRepository
import com.isharaai.isl.core.db.ChatDao
import com.isharaai.isl.core.db.SignDatabase
import com.isharaai.isl.feature.video.BengaliTTSManager
import com.isharaai.isl.feature.video.ISLVideoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


// Hilt Module for dependency injection
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideSignDatabase(@ApplicationContext ctx: Context): SignDatabase =
        SignDatabase.create(ctx)

    @Provides
    @Singleton
    fun provideSignRepository(db: SignDatabase): SignRepository =
        SignRepository(db.signDao())

    @Provides
    @Singleton
    fun provideChatDao(db: SignDatabase): ChatDao =
        db.chatDao()

    @Provides
    @Singleton
    fun provideChatRepository(chatDao: ChatDao): ChatRepository =
        ChatRepository(chatDao)

    @Provides
    @Singleton
    fun provideTTSManager(@ApplicationContext ctx: Context): BengaliTTSManager =
        BengaliTTSManager(ctx)

    @Provides
    @Singleton
    fun provideISLVideoPlayer(@ApplicationContext ctx: Context): ISLVideoPlayer =
        ISLVideoPlayer(ctx)
}
