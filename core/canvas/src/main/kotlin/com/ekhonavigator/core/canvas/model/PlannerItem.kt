package com.ekhonavigator.core.canvas.model

import java.time.Instant

data class PlannerItem(
    /** Composite "$kind_$plannableId" — globally unique across plannable types. */
    val id: String,
    val kind: PlannerKind,
    val courseId: String?,
    val title: String,
    val contextName: String?,
    val contextImage: String?,
    val plannableDate: Instant,
    val dueAt: Instant?,
    val pointsPossible: Double?,
    val htmlUrl: String,
    val newActivity: Boolean,
    val submission: PlannerSubmissionStatus,
)

data class PlannerSubmissionStatus(
    val submitted: Boolean,
    val late: Boolean,
    val missing: Boolean,
    val graded: Boolean,
    val needsGrading: Boolean,
    val hasFeedback: Boolean,
    val excused: Boolean,
) {
    companion object {
        val EMPTY = PlannerSubmissionStatus(
            submitted = false,
            late = false,
            missing = false,
            graded = false,
            needsGrading = false,
            hasFeedback = false,
            excused = false,
        )
    }
}

enum class PlannerKind {
    ASSIGNMENT,
    QUIZ,
    DISCUSSION,
    ANNOUNCEMENT,
    CALENDAR_EVENT,
    PLANNER_NOTE,
    WIKI_PAGE,
    UNKNOWN;

    companion object {
        fun fromCanvasType(plannableType: String): PlannerKind = when (plannableType) {
            "assignment" -> ASSIGNMENT
            "quiz" -> QUIZ
            "discussion_topic" -> DISCUSSION
            "announcement" -> ANNOUNCEMENT
            "calendar_event" -> CALENDAR_EVENT
            "planner_note" -> PLANNER_NOTE
            "wiki_page" -> WIKI_PAGE
            else -> UNKNOWN
        }
    }
}
