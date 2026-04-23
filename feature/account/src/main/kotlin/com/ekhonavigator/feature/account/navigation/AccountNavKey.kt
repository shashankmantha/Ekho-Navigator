package com.ekhonavigator.feature.account.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.ekhonavigator.core.navigation.Navigator

@Serializable
object AccountNavKey : NavKey

fun Navigator.navigateToAccount() {
    navigate(AccountNavKey)
}
