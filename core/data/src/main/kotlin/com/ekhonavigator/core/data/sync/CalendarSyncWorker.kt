package com.ekhonavigator.core.data.sync

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
    private val dailyEventNotificationManager: DailyEventNotificationManager,
) : CoroutineWorker(appContext, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val feedUrl = inputData.getString(KEY_FEED_URL) ?: return Result.failure()

        return when (val result = calendarRepository.sync(feedUrl)) {
            is SyncResult.Success -> {
                calendarRepository.restoreBookmarks()
                if (hasPostNotificationsPermission()) {
                    runCatching {
                        dailyEventNotificationManager.notifyTodaysEventsIfNeeded()
                        dailyEventNotificationManager.notifyBookmarkedEventsIfNeeded()
                    }
                }
                Result.success()
            }
            is SyncResult.Error -> {
                if (runAttemptCount < MAX_RETRIES) Result.retry()
                else Result.failure()
            }
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val KEY_FEED_URL = "feed_url"
        const val UNIQUE_WORK_NAME = "CalendarSyncWork"
        private const val MAX_RETRIES = 3
    }
}
