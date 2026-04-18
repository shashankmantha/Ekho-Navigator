package com.ekhonavigator.feature.discover.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DiscoverNavKey(val initialLocationFilter: String? = null) : NavKey
