package com.ekhonavigator.feature.social.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.social.ChatScreen
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.social.UserProfileScreen

@Serializable
object SocialNavKey : NavKey

@Serializable
data class UserProfileNavKey(
    val userId: String,
) : NavKey

@Serializable
data class ChatNavKey(
    val friendUserId: String,
    val friendDisplayName: String,
) : NavKey

fun EntryProviderScope<NavKey>.socialEntry(navigator: Navigator) {
    entry<SocialNavKey> {
        SocialScreen(
            onProfileClick = { userId ->
                navigator.navigate(UserProfileNavKey(userId))
            },
            onMessageClick = { friendUserId, friendDisplayName ->
                navigator.navigate(
                    ChatNavKey(
                        friendUserId = friendUserId,
                        friendDisplayName = friendDisplayName,
                    )
                )
            },
        )
    }

    entry<UserProfileNavKey> { key ->
        UserProfileScreen(
            userId = key.userId,
        )
    }

    entry<ChatNavKey> { key ->
        ChatScreen(
            friendUserId = key.friendUserId,
            friendDisplayName = key.friendDisplayName,
        )
    }
}