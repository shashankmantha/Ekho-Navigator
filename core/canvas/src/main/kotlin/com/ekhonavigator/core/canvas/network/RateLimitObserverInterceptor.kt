package com.ekhonavigator.core.canvas.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canvas exposes per-token throttling via response headers. Quota numbers
 * aren't documented; we observe X-Request-Cost (always present) and
 * X-Rate-Limit-Remaining (present when quota is being consumed) and surface
 * each response as a sample so downstream code can adapt cadence or alert.
 */
@Singleton
class RateLimitObserverInterceptor @Inject constructor() : Interceptor {

    private val _samples = MutableSharedFlow<CanvasRateLimitSample>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val samples: SharedFlow<CanvasRateLimitSample> = _samples

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        _samples.tryEmit(
            CanvasRateLimitSample(
                requestCost = response.header(HEADER_REQUEST_COST)?.toDoubleOrNull(),
                remaining = response.header(HEADER_REMAINING)?.toDoubleOrNull(),
                urlPath = response.request.url.encodedPath,
                statusCode = response.code,
            ),
        )
        return response
    }

    companion object {
        private const val HEADER_REQUEST_COST = "X-Request-Cost"
        private const val HEADER_REMAINING = "X-Rate-Limit-Remaining"
    }
}

data class CanvasRateLimitSample(
    val requestCost: Double?,
    val remaining: Double?,
    val urlPath: String,
    val statusCode: Int,
)
