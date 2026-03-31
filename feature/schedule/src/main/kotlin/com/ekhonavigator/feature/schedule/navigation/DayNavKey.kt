package com.ekhonavigator.feature.schedule.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

/**
 * Navigation key for the day detail screen.
 * @param epochDay The day as [java.time.LocalDate.toEpochDay] for serialization.
 */
@Serializable
data class DayNavKey(val epochDay: Long) : NavKey

fun Navigator.navigateToDay(epochDay: Long) {
    navigate(DayNavKey(epochDay))
}
