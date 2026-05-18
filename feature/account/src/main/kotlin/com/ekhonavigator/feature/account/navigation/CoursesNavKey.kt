package com.ekhonavigator.feature.account.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
object CoursesNavKey : NavKey

fun Navigator.navigateToCourses() {
    navigate(CoursesNavKey)
}
