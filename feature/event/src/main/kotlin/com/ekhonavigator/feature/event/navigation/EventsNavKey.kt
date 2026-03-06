package com.ekhonavigator.feature.event.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.event.EventScreen
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy

@Serializable
data class EventNavKey(val id: String) : NavKey

fun Navigator.navigateToEvent(
    eventId: String,
) {
    navigate(EventNavKey(eventId))
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.eventEntry(navigator: Navigator) {
    entry<EventNavKey>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { key ->
        val id = key.id
        EventScreen(
            showBackButton = true,
            onBackClick = { navigator.goBack() },
            onEventClick = navigator::navigateToEvent,
        )
    }
}
