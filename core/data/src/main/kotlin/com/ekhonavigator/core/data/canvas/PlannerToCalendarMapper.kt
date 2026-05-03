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
 * for plannable kinds we surface elsewhere (announcements / discussions go to the
 * notifications bell) or not at all (wiki pages, planner notes — for now).
 *
 * Description, location, and categories are intentionally empty: detail UIs query
 * the planner table directly for the rich Canvas-specific metadata.
 */
internal fun CanvasPlannerItemEntity.toCalendarEventOrNull(): CalendarEventEntity? {
    val (mappedType, instant) = when (PlannerKind.fromCanvasType(plannableType)) {
        PlannerKind.ASSIGNMENT, PlannerKind.QUIZ -> EventType.ASSIGNMENT to (dueAt ?: plannableDate)
        PlannerKind.CALENDAR_EVENT -> EventType.EVENT to plannableDate
        else -> return null
    }

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
        eventType = "",
        placeId = null,
        externalSourceId = id,
        externalSourceType = CANVAS_PLANNER_ITEM_SOURCE,
        dueAt = dueAt,
        type = mappedType,
    )
}
