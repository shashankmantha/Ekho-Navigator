package com.ekhonavigator.core.canvas.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

internal class CanvasHeadersInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", ACCEPT_STRING_IDS)
            .header("User-Agent", USER_AGENT)
            .build()
        return chain.proceed(request)
    }

    companion object {
        // Canvas returns 64-bit IDs as integers by default; this header forces
        // string serialization so deserializers can use String safely without precision loss.
        private const val ACCEPT_STRING_IDS = "application/json+canvas-string-ids"

        // Identifies our pilot traffic to Instructure for fair-use accountability.
        private const val USER_AGENT = "EkhoNavigator/0.x (csuci-pilot)"
    }
}
