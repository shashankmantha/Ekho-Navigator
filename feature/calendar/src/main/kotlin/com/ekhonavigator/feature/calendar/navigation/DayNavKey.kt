package com.ekhonavigator.feature.calendar.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSourceType
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

/**
 * Navigation key for the day detail screen.
 * @param epochDay The day as [java.time.LocalDate.toEpochDay] for serialization.
 * @param sourceTypes Active source-type filter names from the parent schedule screen.
 *   Empty list means "use ViewModel defaults".
 * @param categories Active category filter names from the parent schedule screen.
 *   Empty list means "no category filter" (show all).
 */
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
