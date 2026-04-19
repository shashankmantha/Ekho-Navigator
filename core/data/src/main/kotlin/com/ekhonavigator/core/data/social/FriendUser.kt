package com.ekhonavigator.core.data.social

import com.ekhonavigator.core.model.OnlineStatus

data class FriendUser(
    val uid: String = "",
    val displayName: String = "",
    val avatarId: String = "avatar_default",
    val major: String = "",
    val showOnlineStatus: Boolean = true,
    val online: Boolean = false,
    val onlineStatus: OnlineStatus = OnlineStatus.ONLINE,
    val lastChanged: Long = 0L,
)
