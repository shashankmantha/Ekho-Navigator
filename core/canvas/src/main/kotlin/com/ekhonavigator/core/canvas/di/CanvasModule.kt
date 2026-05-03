package com.ekhonavigator.core.canvas.di

import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.auth.CanvasAuthValidator
import com.ekhonavigator.core.canvas.auth.CanvasInstitutionStore
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import com.ekhonavigator.core.canvas.auth.DefaultCanvasAccountSource
import com.ekhonavigator.core.canvas.auth.DefaultCanvasAuthValidator
import com.ekhonavigator.core.canvas.auth.DefaultCanvasInstitutionStore
import com.ekhonavigator.core.canvas.auth.DefaultCanvasTokenStore
import com.ekhonavigator.core.canvas.network.BearerInterceptor
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.CanvasHeadersInterceptor
import com.ekhonavigator.core.canvas.network.CanvasOkHttp
import com.ekhonavigator.core.canvas.network.DefaultCanvasApiProvider
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
internal abstract class CanvasModule {

    @Binds
    internal abstract fun bindCanvasTokenStore(impl: DefaultCanvasTokenStore): CanvasTokenStore

    @Binds
    internal abstract fun bindCanvasInstitutionStore(impl: DefaultCanvasInstitutionStore): CanvasInstitutionStore

    @Binds
    internal abstract fun bindCanvasAccountSource(impl: DefaultCanvasAccountSource): CanvasAccountSource

    @Binds
    internal abstract fun bindCanvasAuthValidator(impl: DefaultCanvasAuthValidator): CanvasAuthValidator

    @Binds
    internal abstract fun bindCanvasApiProvider(impl: DefaultCanvasApiProvider): CanvasApiProvider

    companion object {

        @Provides
        @Singleton
        internal fun provideCanvasJson(): Json = canvasJson

        @Provides
        @Singleton
        @CanvasOkHttp
        internal fun provideCanvasOkHttpClient(
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
