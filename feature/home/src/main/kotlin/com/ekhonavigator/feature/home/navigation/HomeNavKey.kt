package com.ekhonavigator.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.home.HomeScreen
import com.ekhonavigator.feature.event.navigation.navigateToEvent

@Serializable
object HomeNavKey : NavKey

fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> {
        HomeScreen(
            onEventClick = navigator::navigateToEvent,
        )
    }
}
