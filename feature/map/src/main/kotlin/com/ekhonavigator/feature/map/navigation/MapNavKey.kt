package com.ekhonavigator.feature.map.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class MapNavKey(val focusPlaceId: String? = null) : NavKey
