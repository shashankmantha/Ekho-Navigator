package com.ekhonavigator.feature.canvas.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
object MyCoursesNavKey : NavKey

fun Navigator.navigateToMyCourses() {
    navigate(MyCoursesNavKey)
}
