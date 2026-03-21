package com.ekhonavigator.core.data.profile

data class UserProfile(
    val displayName: String = "",
    val email: String = "",
    val major: String = "",
    val description: String = "",
    val links: String = "",
    val majorVisible: Boolean = true,
    val descriptionVisible: Boolean = true,
    val linksVisible: Boolean = true,
)