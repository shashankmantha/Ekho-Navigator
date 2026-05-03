package com.ekhonavigator.core.canvas.di

import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import com.ekhonavigator.core.canvas.auth.DefaultCanvasTokenStore
import com.ekhonavigator.core.canvas.auth.NoCanvasAccountSource
import com.ekhonavigator.core.canvas.network.BearerInterceptor
import com.ekhonavigator.core.canvas.network.CanvasHeadersInterceptor
import com.ekhonavigator.core.canvas.network.CanvasOkHttp
import com.ekhonavigator.core.canvas.network.RateLimitObserverInterceptor
import com.ekhonavigator.core.canvas.network.canvasJson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CanvasModule {

    @Binds
    abstract fun bindCanvasTokenStore(impl: DefaultCanvasTokenStore): CanvasTokenStore

    @Binds
    abstract fun bindCanvasAccountSource(impl: NoCanvasAccountSource): CanvasAccountSource

    companion object {

        @Provides
        @Singleton
        fun provideCanvasJson(): Json = canvasJson

        @Provides
        @Singleton
        @CanvasOkHttp
        fun provideCanvasOkHttpClient(
            headers: CanvasHeadersInterceptor,
            bearer: BearerInterceptor,
            rateLimit: RateLimitObserverInterceptor,
        ): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(headers)
            .addInterceptor(bearer)
            .addInterceptor(rateLimit)
            .build()
    }
}
