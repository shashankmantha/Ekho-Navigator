package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CanvasPlannerRepository {

    /** Cached planner items whose plannable date falls within `[start, end)`. */
    fun observeItems(start: Instant, end: Instant): Flow<List<PlannerItem>>

    /**
     * Fetches planner items for `[start, end)` from Canvas and reconciles the cache:
     * upserts what came back, prunes anything in that window the response no longer contains.
     * Items outside the window are left alone.
     */
    suspend fun sync(start: Instant, end: Instant): Result<Unit>
}
