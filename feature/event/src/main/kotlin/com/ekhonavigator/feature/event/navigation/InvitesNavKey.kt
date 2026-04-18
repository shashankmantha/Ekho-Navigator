package com.ekhonavigator.feature.event.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data object InvitesNavKey : NavKey

fun Navigator.navigateToInvites() {
    navigate(InvitesNavKey)
}
