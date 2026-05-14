package com.ekhonavigator.feature.calendar.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

// Empty sourceTypes = use ViewModel defaults. Empty categories = show all.
@Serializable
data class DayNavKey(
    val epochDay: Long,
    val sourceTypes: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
) : NavKey

fun Navigator.navigateToDay(
    epochDay: Long,
    sourceTypes: Set<EventSourceType> = emptySet(),
    categories: Set<EventCategory> = emptySet(),
) {
    navigate(
        DayNavKey(
            epochDay = epochDay,
            sourceTypes = sourceTypes.map { it.name },
            categories = categories.map { it.name },
        ),
    )
}
