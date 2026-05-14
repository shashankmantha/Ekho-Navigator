package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerKind
import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType

internal const val CANVAS_PLANNER_ITEM_SOURCE = "canvas_planner_item"

// Returns null for plannable kinds we surface elsewhere (announcements →
// bell) or hide by default (calendar_events are usually instructor noise).
// description stays empty — the planner endpoint doesn't include the body;
// it gets backfilled from the per-course assignments endpoint after sync.
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
