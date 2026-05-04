package com.ekhonavigator.feature.canvas.navigation

import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.navigation.Navigator
import kotlinx.serialization.Serializable

/** Per-class detail screen — Phase 7.A2 target for taps from the My Courses grid
 *  and from event detail courseLabel chips when the family-key matches a Canvas
 *  course. Carries the courseId so the screen can resolve via repository lookup. */
@Serializable
data class CourseDetailNavKey(val courseId: String) : NavKey

fun Navigator.navigateToCourseDetail(courseId: String) {
    navigate(CourseDetailNavKey(courseId = courseId))
}
