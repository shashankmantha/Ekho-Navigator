package com.ekhonavigator.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Convenience object for scheduling calendar sync work.
 */
object SyncInitializer {

    /**
     * Schedules periodic sync every [intervalHours] hours.
     * Uses [ExistingPeriodicWorkPolicy.KEEP] so restarting the app
     * doesn't reset the timer.
     */
    fun enqueuePeriodicSync(context: Context, feedUrl: String, intervalHours: Long = 2) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(CalendarSyncWorker.KEY_FEED_URL, feedUrl)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
            repeatInterval = intervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CalendarSyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork,
        )
    }

    /**
     * Triggers a one-time immediate sync. Use for pull-to-refresh.
     */
    fun requestImmediateSync(context: Context, feedUrl: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(CalendarSyncWorker.KEY_FEED_URL, feedUrl)
            .build()

        val oneTimeWork = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWork)
    }
}
