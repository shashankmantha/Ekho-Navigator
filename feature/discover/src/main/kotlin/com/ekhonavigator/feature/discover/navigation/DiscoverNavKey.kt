package com.ekhonavigator.feature.discover.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.feature.discover.DiscoverTab
import kotlinx.serialization.Serializable

@Serializable
data class DiscoverNavKey(
    val focusPlaceId: String? = null,
    val initialTab: DiscoverTab = DiscoverTab.COURSES,
) : NavKey
