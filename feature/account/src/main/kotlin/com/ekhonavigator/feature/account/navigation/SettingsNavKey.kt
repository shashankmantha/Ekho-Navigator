package com.ekhonavigator.feature.account.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator

@Serializable
object SettingsNavKey : NavKey

fun Navigator.navigateToSettings() {
    navigate(SettingsNavKey)
}
