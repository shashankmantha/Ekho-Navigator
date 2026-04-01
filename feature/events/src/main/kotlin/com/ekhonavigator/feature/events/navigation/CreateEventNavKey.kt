package com.ekhonavigator.feature.events.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class CreateEventNavKey(val initialEpochDay: Long? = null) : NavKey

fun Navigator.navigateToCreateEvent(initialEpochDay: Long? = null) {
    navigate(CreateEventNavKey(initialEpochDay))
}
