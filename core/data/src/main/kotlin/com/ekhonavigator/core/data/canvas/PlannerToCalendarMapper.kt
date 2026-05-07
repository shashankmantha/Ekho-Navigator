package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerKind
import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType

/** Source-type discriminator for calendar_events rows mirrored from canvas_planner_items. */
internal const val CANVAS_PLANNER_ITEM_SOURCE = "canvas_planner_item"

/**
 * Projects a Canvas planner item onto the unified calendar surface, or returns null
 * for plannable kinds users don't want on a calendar (calendar_events are typically
 * instructor-authored office-hours / nice-to-know items — they live in the planner
 * table for a future opt-in toggle but stay off the calendar by default), or kinds
 * we surface elsewhere (announcements / discussions go to the notifications bell).
 *
 * `description` stays empty here — Canvas's planner endpoint doesn't include the
 * assignment body. Phase 7.A2 backfills `description` from the per-course
 * assignments endpoint after sync, so EventScreen will get richer text without
 * any change to this mapper. `location` has no Canvas equivalent at all, and
 * `categories` is empty by design — Canvas doesn't carry our category taxonomy.
 */
internal fun CanvasPlannerItemEntity.toCalendarEventOrNull(): CalendarEventEntity? {
    val kind = PlannerKind.fromCanvasType(plannableType)
    val instant = when (kind) {
        PlannerKind.ASSIGNMENT, PlannerKind.QUIZ -> dueAt ?: plannableDate
        else -> return null
    }
    val mappedType = EventType.ASSIGNMENT

    return CalendarEventEntity(
        uid = id,
        title = title,
        description = "",
        location = "",
        startTime = instant,
        endTime = instant,
        categories = emptyList(),
        url = htmlUrl,
        status = "CONFIRMED",
        isBookmarked = false,
        lastSyncedAt = lastSyncedAt,
        source = EventSource.CANVAS,
        ownerUid = null,
        ownerDisplayName = contextName.orEmpty(),
        pendingSync = false,
        eventName = title,
        organization = contextName.orEmpty(),
        // Surface the plannable kind ("Assignment" / "Quiz") as the eventType
        // ribbon on EventScreen — was empty before, leaving the detail screen
        // saying nothing about what kind of Canvas thing the user is looking at.
        eventType = kind.displayName(),
        placeId = null,
        externalSourceId = id,
        externalSourceType = CANVAS_PLANNER_ITEM_SOURCE,
        dueAt = dueAt,
        type = mappedType,
    )
}

private fun PlannerKind.displayName(): String = when (this) {
    PlannerKind.ASSIGNMENT -> "Assignment"
    PlannerKind.QUIZ -> "Quiz"
    PlannerKind.DISCUSSION -> "Discussion"
    PlannerKind.ANNOUNCEMENT -> "Announcement"
    PlannerKind.CALENDAR_EVENT -> "Calendar Event"
    PlannerKind.PLANNER_NOTE -> "Note"
    PlannerKind.WIKI_PAGE -> "Page"
    PlannerKind.UNKNOWN -> ""
}
