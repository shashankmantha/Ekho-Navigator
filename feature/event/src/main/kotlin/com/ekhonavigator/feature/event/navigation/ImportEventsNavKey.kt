package com.ekhonavigator.feature.event.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data object ImportEventsNavKey : NavKey

fun Navigator.navigateToImportEvents() {
    navigate(ImportEventsNavKey)
}
