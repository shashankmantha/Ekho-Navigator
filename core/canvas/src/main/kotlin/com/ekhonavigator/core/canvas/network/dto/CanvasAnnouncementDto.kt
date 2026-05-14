package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Announcements are discussion_topic records with is_announcement=true.
// contextCode arrives as "course_<id>"; the mapper strips the prefix.
@Serializable
data class CanvasAnnouncementDto(
    val id: String,
    val title: String,
    val message: String? = null,
    @SerialName("posted_at") val postedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("context_code") val contextCode: String,
    val author: CanvasAnnouncementAuthorDto? = null,
)

@Serializable
data class CanvasAnnouncementAuthorDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_image_url") val avatarUrl: String? = null,
)
