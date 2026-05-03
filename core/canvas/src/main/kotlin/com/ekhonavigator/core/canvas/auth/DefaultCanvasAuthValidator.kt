package com.ekhonavigator.core.canvas.auth

import com.ekhonavigator.core.canvas.model.CanvasProfile
import com.ekhonavigator.core.canvas.network.CanvasOkHttp
import com.ekhonavigator.core.canvas.network.dto.CanvasUserDto
import com.ekhonavigator.core.canvas.network.dto.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

internal class DefaultCanvasAuthValidator @Inject constructor(
    @CanvasOkHttp private val okHttpClient: OkHttpClient,
    private val json: Json,
) : CanvasAuthValidator {

    override suspend fun validate(domain: String, token: String): Result<CanvasProfile> =
        withContext(Dispatchers.IO) {
            // Strip whitespace anywhere in the token. OkHttp's Authorization header
            // validator throws IllegalArgumentException on any \n / \r mid-value;
            // PATs are alphanumeric so removing whitespace can't corrupt them.
            val sanitizedToken = token.filterNot { it.isWhitespace() }
            val baseUrl = if (domain.contains("://")) domain else "https://$domain"
            val url = "$baseUrl/api/v1/users/self/profile".toHttpUrlOrNull()
                ?: return@withContext Result.failure(CanvasAuthError.InvalidDomain)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $sanitizedToken")
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.code == 401 -> Result.failure(CanvasAuthError.InvalidToken)
                        !response.isSuccessful -> Result.failure(CanvasAuthError.HttpError(response.code))
                        else -> {
                            val dto = json.decodeFromString<CanvasUserDto>(response.body.string())
                            Result.success(dto.toDomain())
                        }
                    }
                }
            } catch (e: IOException) {
                Result.failure(CanvasAuthError.Network(e))
            } catch (e: SerializationException) {
                Result.failure(CanvasAuthError.ParseError(e))
            }
        }
}
