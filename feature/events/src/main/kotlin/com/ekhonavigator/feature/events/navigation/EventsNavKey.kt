package com.ekhonavigator.feature.events.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.events.EventsScreen
import com.ekhonavigator.feature.event.navigation.navigateToEvent

@Serializable
object EventsNavKey : NavKey

fun EntryProviderScope<NavKey>.eventsEntry(navigator: Navigator) {
    entry<EventsNavKey> {
        EventsScreen(
            onEventClick = navigator::navigateToEvent,
        )
    }
}
