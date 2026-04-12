package com.ekhonavigator.core.data.profile

data class UserProfile(
    val displayName: String = "",
    val displayNameLower: String = "",
    val email: String = "",
    val emailLower: String = "",
    val major: String = "",
    val majorLower: String = "",
    val description: String = "",
    val links: String = "",
    val majorVisible: Boolean = true,
    val descriptionVisible: Boolean = true,
    val linksVisible: Boolean = true,
    val avatarId: String = "avatar_default",
    val searchable: Boolean = true,
    val showOnlineStatus: Boolean = true,
)