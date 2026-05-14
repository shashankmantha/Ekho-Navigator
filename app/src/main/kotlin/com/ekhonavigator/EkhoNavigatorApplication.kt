package com.ekhonavigator

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ekhonavigator.core.data.auth.AuthLifecycleObserver
import com.ekhonavigator.core.data.social.ChatNotificationObserver
import com.ekhonavigator.core.data.sync.SyncInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EkhoNavigatorApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var chatNotificationObserver: ChatNotificationObserver

    @Inject
    lateinit var authLifecycleObserver: AuthLifecycleObserver

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        chatNotificationObserver.start()

        // Reactive per-user setup/teardown on Firebase auth state changes.
        authLifecycleObserver.start()

        val feedUrl = "https://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"

        SyncInitializer.enqueuePeriodicSync(
            context = this,
            feedUrl = feedUrl,
        )

        // Immediate sync on launch — periodic schedule's first fire is too far out.
        SyncInitializer.requestImmediateSync(
            context = this,
            feedUrl = feedUrl,
        )
    }
}