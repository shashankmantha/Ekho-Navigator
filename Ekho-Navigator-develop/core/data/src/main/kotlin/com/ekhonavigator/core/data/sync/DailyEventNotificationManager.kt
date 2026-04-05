package com.ekhonavigator.core.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ekhonavigator.core.database.dao.CalendarEventDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyEventNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarEventDao: CalendarEventDao,
) {
    suspend fun notifyTodaysEventsIfNeeded(now: Instant = Instant.now()) {
        if (!canPostNotifications()) return

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        if (wasAlreadyNotified(today, KEY_LAST_NOTIFIED_DATE)) return

        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        val todaysEvents = calendarEventDao
            .getEventsByDateRange(startOfDay, endOfDay)
            .filter { it.endTime.isAfter(now) }

        if (todaysEvents.isEmpty()) return

        postNotification(
            notificationId = NOTIFICATION_ID,
            title = if (todaysEvents.size == 1) {
                "1 event happening today"
            } else {
                "${todaysEvents.size} events happening today"
            },
            contentText = todaysEvents.first().title,
            events = todaysEvents,
        )
        markNotified(today, KEY_LAST_NOTIFIED_DATE)
    }

    suspend fun notifyBookmarkedEventsIfNeeded(now: Instant = Instant.now()) {
        if (!canPostNotifications()) return

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        if (wasAlreadyNotified(today, KEY_LAST_BOOKMARKED_NOTIFIED_DATE)) return

        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        val bookmarkedEvents = calendarEventDao
            .getBookmarkedEventsByDateRange(startOfDay, endOfDay)
            .filter { it.endTime.isAfter(now) }

        if (bookmarkedEvents.isEmpty()) return

        postNotification(
            notificationId = BOOKMARKED_NOTIFICATION_ID,
            title = if (bookmarkedEvents.size == 1) {
                "1 bookmarked event today"
            } else {
                "${bookmarkedEvents.size} bookmarked events today"
            },
            contentText = bookmarkedEvents.first().title,
            events = bookmarkedEvents,
        )
        markNotified(today, KEY_LAST_BOOKMARKED_NOTIFIED_DATE)
    }

    suspend fun notifyEventBookmarked(
        eventId: String,
        now: Instant = Instant.now(),
    ) {
        if (!canPostNotifications()) return

        val event = calendarEventDao.getEventById(eventId) ?: return
        if (!event.isBookmarked || !event.endTime.isAfter(now)) return

        postNotification(
            notificationId = BOOKMARK_ACTION_NOTIFICATION_ID,
            title = "Event bookmarked",
            contentText = event.title,
            events = listOf(event),
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun postNotification(
        notificationId: Int,
        title: String,
        contentText: String,
        events: List<com.ekhonavigator.core.database.model.CalendarEventEntity>,
    ) {
        if (!canPostNotifications()) return

        createNotificationChannel()

        val zoneId = ZoneId.systemDefault()
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val style = NotificationCompat.InboxStyle()
        events.take(MAX_LINES).forEach { event ->
            val startText = event.startTime.atZone(zoneId).format(timeFormatter)
            style.addLine("$startText • ${event.title}")
        }
        if (events.size > MAX_LINES) {
            style.setSummaryText("+${events.size - MAX_LINES} more")
        }

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun wasAlreadyNotified(date: LocalDate, key: String): Boolean =
        prefs().getString(key, null) == date.toString()

    private fun markNotified(date: LocalDate, key: String) {
        prefs().edit().putString(key, date.toString()).apply()
    }

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily events",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications for events happening today"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val PREFS_NAME = "daily_event_notifications"
        private const val KEY_LAST_NOTIFIED_DATE = "last_notified_date"
        private const val KEY_LAST_BOOKMARKED_NOTIFIED_DATE = "last_bookmarked_notified_date"
        private const val CHANNEL_ID = "daily_events"
        private const val NOTIFICATION_ID = 1001
        private const val BOOKMARKED_NOTIFICATION_ID = 1002
        private const val BOOKMARK_ACTION_NOTIFICATION_ID = 1003
        private const val MAX_LINES = 4
    }
}
