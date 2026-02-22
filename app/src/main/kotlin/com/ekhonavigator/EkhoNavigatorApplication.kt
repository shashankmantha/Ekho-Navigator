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
        // TODO: Replace with actual CSUCI 25Live feed URL
        SyncInitializer.enqueuePeriodicSync(
            context = this,
            feedUrl = "https://25livepub.collegenet.com/calendars/CSUCI_EVENTS.ics",
        )
    }
}
