package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape returned by `GET /api/v1/announcements?context_codes[]=course_<id>`.
 *
 * Canvas announcements are technically `discussion_topic` records with
 * `is_announcement=true`, so this DTO mirrors the discussion-topic shape but
 * stays scoped to the fields the Announcements section actually renders.
 *
 * `contextCode` arrives as `course_<id>` — we strip the prefix at mapping
 * time so the entity carries a plain `courseId` like every other table.
 */
@Serializable
data class CanvasAnnouncementDto(
    val id: String,
    val title: String,
    /** HTML body — sanitized + rendered by EventScreen / detail expansion via
     *  the existing `HtmlDescription` helper. */
    val message: String? = null,
    @SerialName("posted_at") val postedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("context_code") val contextCode: String,
    /** Author info — Canvas nests this under `author` on the discussion-topic
     *  payload. Only the display name surfaces in the UI; the rest stays
     *  available for future use. */
    val author: CanvasAnnouncementAuthorDto? = null,
)

@Serializable
data class CanvasAnnouncementAuthorDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_image_url") val avatarUrl: String? = null,
)
