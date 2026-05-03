package com.ekhonavigator.core.canvas.network

import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasApiProvider @Inject constructor(
    @CanvasOkHttp private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val accountSource: CanvasAccountSource,
) : CanvasApiProvider {

    @Volatile
    private var cached: Pair<String, CanvasApi>? = null

    override fun current(): CanvasApi? {
        val account = accountSource.currentOrNull() ?: return null
        cached?.let { (domain, api) -> if (domain == account.domain) return api }
        return buildApi(account.domain).also { cached = account.domain to it }
    }

    private fun buildApi(domain: String): CanvasApi = Retrofit.Builder()
        .baseUrl("https://$domain/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()
        .create(CanvasApi::class.java)

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
