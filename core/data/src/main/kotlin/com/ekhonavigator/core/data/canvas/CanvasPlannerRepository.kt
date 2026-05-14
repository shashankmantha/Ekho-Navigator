package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CanvasPlannerRepository {

    fun observeItems(start: Instant, end: Instant): Flow<List<PlannerItem>>

    fun observeAllItems(): Flow<List<PlannerItem>>

    fun observeById(id: String): Flow<PlannerItem?>

    // Upserts what came back, prunes items in the window the response no longer
    // contains. Items outside the window are left alone.
    suspend fun sync(start: Instant, end: Instant): Result<Unit>

    // Also wipes the bridged calendar_events rows — otherwise Canvas pills
    // linger on the calendar after disconnect.
    suspend fun clearAll()
}
