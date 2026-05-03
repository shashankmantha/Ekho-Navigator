package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CanvasPlannerRepository {

    /** Cached planner items whose plannable date falls within `[start, end)`. */
    fun observeItems(start: Instant, end: Instant): Flow<List<PlannerItem>>

    /**
     * All cached planner items, regardless of date. Used by render-side filters
     * that need a global eventId → courseId lookup without re-keying per range.
     * Bounded in volume by the sync window's pruning policy.
     */
    fun observeAllItems(): Flow<List<PlannerItem>>

    /**
     * Fetches planner items for `[start, end)` from Canvas and reconciles the cache:
     * upserts what came back, prunes anything in that window the response no longer contains.
     * Items outside the window are left alone.
     */
    suspend fun sync(start: Instant, end: Instant): Result<Unit>

    /**
     * Wipes the planner-items cache AND the calendar_events rows that were bridged
     * from those planner items. Call this on PAT disconnect / sign-out — without
     * the second wipe, "Canvas events" linger on the calendar even after the user
     * appears to have disconnected.
     */
    suspend fun clearAll()
}
