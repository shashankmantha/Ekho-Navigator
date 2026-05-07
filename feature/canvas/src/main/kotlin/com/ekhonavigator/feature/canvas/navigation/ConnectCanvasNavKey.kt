package com.ekhonavigator.feature.canvas.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
object ConnectCanvasNavKey : NavKey

fun Navigator.navigateToConnectCanvas() {
    navigate(ConnectCanvasNavKey)
}
