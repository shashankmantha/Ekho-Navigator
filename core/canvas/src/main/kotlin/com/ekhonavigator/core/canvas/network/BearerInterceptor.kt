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
        // A request that already carries Authorization (e.g. token-validation
        // calls during connect) wins over the stored account's token.
        if (chain.request().header("Authorization") != null) {
            return chain.proceed(chain.request())
        }
        val token = accountSource.currentOrNull()?.let(tokenStore::get)
            ?: return chain.proceed(chain.request())
        // Defensive sanitization in case a legacy entry in the encrypted store
        // contains whitespace from a copy-paste artifact. OkHttp's Authorization
        // header validator throws on any \n / \r — would crash every API call.
        val sanitized = token.filterNot { it.isWhitespace() }
        val authed = chain.request().newBuilder()
            .header("Authorization", "Bearer $sanitized")
            .build()
        return chain.proceed(authed)
    }
}
