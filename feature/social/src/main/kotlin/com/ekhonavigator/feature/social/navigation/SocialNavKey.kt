package com.ekhonavigator.feature.social.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.model.SharedLocation
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.social.ChatScreen
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.social.UserProfileScreen
import kotlinx.serialization.Serializable

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
    val friendAvatarId: String,
    val sharedLocation: SharedLocation? = null
) : NavKey

fun EntryProviderScope<NavKey>.socialEntry(navigator: Navigator, onNavigateToMap: () -> Unit) {
    entry<SocialNavKey> {
        SocialScreen(
            onProfileClick = { userId ->
                navigator.navigate(UserProfileNavKey(userId))
            },
            onMessageClick = { friendUserId, friendDisplayName, friendAvatarId ->
                navigator.navigate(
                    ChatNavKey(
                        friendUserId = friendUserId,
                        friendDisplayName = friendDisplayName,
                        friendAvatarId = friendAvatarId,
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
            friendAvatarId = key.friendAvatarId,
            sharedLocation = key.sharedLocation,
            onNavigateToMap = onNavigateToMap
        )
    }
}