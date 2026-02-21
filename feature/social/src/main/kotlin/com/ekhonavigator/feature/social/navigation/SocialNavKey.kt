package com.ekhonavigator.feature.social.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.event.navigation.navigateToEvent

@Serializable
object SocialNavKey : NavKey

fun EntryProviderScope<NavKey>.socialEntry(navigator: Navigator) {
    entry<SocialNavKey> {
        SocialScreen(
            onEventClick = navigator::navigateToEvent,
        )
    }
}
