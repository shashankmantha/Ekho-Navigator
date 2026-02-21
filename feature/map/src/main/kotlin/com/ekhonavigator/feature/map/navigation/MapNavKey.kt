package com.ekhonavigator.feature.map.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.map.MapScreen
import com.ekhonavigator.feature.event.navigation.navigateToEvent

@Serializable
object MapNavKey : NavKey

fun EntryProviderScope<NavKey>.mapEntry(navigator: Navigator) {
    entry<MapNavKey> {
        MapScreen(
            onEventClick = navigator::navigateToEvent,
        )
    }
}
