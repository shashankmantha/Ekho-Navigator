package com.ekhonavigator.core.data.social

data class FriendUser(
    val uid: String = "",
    val displayName: String = "",
    val avatarId: String = "avatar_default",
    val major: String = "",
    val showOnlineStatus: Boolean = true,
    val online: Boolean = false,
    val lastChanged: Long = 0L,
)
