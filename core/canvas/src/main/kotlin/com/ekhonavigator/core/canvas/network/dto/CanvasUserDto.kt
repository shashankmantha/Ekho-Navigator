package com.ekhonavigator.core.canvas.network.dto

import com.ekhonavigator.core.canvas.model.CanvasProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CanvasUserDto(
    val id: String,
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("primary_email") val primaryEmail: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

internal fun CanvasUserDto.toDomain(): CanvasProfile = CanvasProfile(
    id = id,
    name = name,
    shortName = shortName,
    primaryEmail = primaryEmail,
    avatarUrl = avatarUrl,
)
