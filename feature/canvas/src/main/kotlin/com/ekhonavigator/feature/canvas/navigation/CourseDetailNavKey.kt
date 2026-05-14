package com.ekhonavigator.feature.canvas.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class CourseDetailNavKey(val courseId: String) : NavKey

fun Navigator.navigateToCourseDetail(courseId: String) {
    navigate(CourseDetailNavKey(courseId = courseId))
}
