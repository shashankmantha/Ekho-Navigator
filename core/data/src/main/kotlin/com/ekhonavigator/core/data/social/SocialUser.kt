package com.ekhonavigator.core.data.social

data class SocialUser(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val major: String = "",
    val description: String = "",
    val links: String = "",
    val avatarId: String = "avatar_default",
    val majorVisible: Boolean = false,
    val descriptionVisible: Boolean = false,
    val linksVisible: Boolean = false,
    val showOnlineStatus: Boolean = true,
)
