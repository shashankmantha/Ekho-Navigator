package com.ekhonavigator.feature.social.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.social.UserProfileScreen

@Serializable
object SocialNavKey : NavKey

@Serializable
data class UserProfileNavKey(
    val userId: String,
) : NavKey

fun EntryProviderScope<NavKey>.socialEntry(navigator: Navigator) {
    entry<SocialNavKey> {
        SocialScreen(
            onProfileClick = { userId ->
                navigator.navigate(UserProfileNavKey(userId))
            },
            onMessageClick = { uid, displayName, avatarId ->
                // TODO: Implement messaging navigation
            }
        )
    }

    entry<UserProfileNavKey> { key ->
        UserProfileScreen(
            userId = key.userId,
        )
    }
}
