package com.ekhonavigator.feature.events.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
object CreateEventNavKey : NavKey

fun Navigator.navigateToCreateEvent() {
    navigate(CreateEventNavKey)
}
