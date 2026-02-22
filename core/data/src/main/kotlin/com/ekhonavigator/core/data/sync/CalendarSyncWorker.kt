package com.ekhonavigator.core.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.util.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that syncs the iCal feed into Room.
 * Scheduled by [SyncInitializer] via WorkManager.
 */
@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val calendarRepository: CalendarRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val feedUrl = inputData.getString(KEY_FEED_URL)
        if (feedUrl == null) {
            Log.e(TAG, "No feed URL in input data")
            return Result.failure()
        }

        Log.d(TAG, "Starting sync from: $feedUrl")

        return when (val result = calendarRepository.sync(feedUrl)) {
            is SyncResult.Success -> {
                Log.d(TAG, "Sync success: ${result.eventsUpdated} events")
                Result.success()
            }
            is SyncResult.Error -> {
                Log.e(TAG, "Sync error: ${result.message}", result.cause)
                if (runAttemptCount < MAX_RETRIES) Result.retry()
                else Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "CalendarSync"
        const val KEY_FEED_URL = "feed_url"
        const val UNIQUE_WORK_NAME = "CalendarSyncWork"
        private const val MAX_RETRIES = 3
    }
}
