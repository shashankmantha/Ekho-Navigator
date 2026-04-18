package com.ekhonavigator.feature.schedule.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleNavKey(val initialLocationFilter: String? = null) : NavKey