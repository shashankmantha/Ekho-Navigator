package com.ekhonavigator.core.canvas.network

import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

internal class BearerInterceptor @Inject constructor(
    private val accountSource: CanvasAccountSource,
    private val tokenStore: CanvasTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accountSource.currentOrNull()?.let(tokenStore::get)
            ?: return chain.proceed(chain.request())
        val authed = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authed)
    }
}
