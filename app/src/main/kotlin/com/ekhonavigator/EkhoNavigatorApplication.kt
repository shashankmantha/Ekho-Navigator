package com.ekhonavigator

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ekhonavigator.core.data.sync.SyncInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EkhoNavigatorApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        val feedUrl = "https://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"

        // Schedule recurring sync every 2 hours
        SyncInitializer.enqueuePeriodicSync(context = this, feedUrl = feedUrl)

        // Fire an immediate sync on every app launch so data is fresh right away
        SyncInitializer.requestImmediateSync(context = this, feedUrl = feedUrl)
    }
}
