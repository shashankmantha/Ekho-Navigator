package com.ekhonavigator.feature.calendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.calendar.CalendarScreen
import com.ekhonavigator.feature.event.navigation.navigateToEvent

@Serializable
object CalendarNavKey : NavKey

fun EntryProviderScope<NavKey>.calendarEntry(navigator: Navigator) {
    entry<CalendarNavKey> {
        CalendarScreen(
            onEventClick = navigator::navigateToEvent,
        )
    }
}
