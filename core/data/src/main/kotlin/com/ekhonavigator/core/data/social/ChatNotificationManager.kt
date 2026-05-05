package com.ekhonavigator.core.data.social

import android.Manifest
import android.R
import android.annotation.SuppressLint
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class ChatNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @SuppressLint("MissingPermission")
    fun showMessageNotification(
        conversationId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        isGroup: Boolean,
        chatTitle: String,
        friendAvatarId: String = "",
    ) {
        if (!canPostNotifications()) return

        createNotificationChannel()

        val notificationTitle = if (isGroup) {
            chatTitle.ifBlank { "Group Chat" }
        } else {
            senderName.ifBlank { "New message" }
        }

        val notificationText = if (isGroup) {
            "${senderName.ifBlank { "Someone" }}: ${messageText.ifBlank { "Sent a message" }}"
        } else {
            messageText.ifBlank { "Sent a message" }
        }

        val expandedText = if (isGroup) {
            "${senderName.ifBlank { "Someone" }}\n${messageText.ifBlank { "Sent a message" }}"
        } else {
            messageText.ifBlank { "Sent a message" }
        }

        val launchIntent = Intent().apply {
            setClassName(context.packageName, "com.ekhonavigator.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("openChat", true)
            putExtra("conversationId", conversationId)
            putExtra("chatTitle", notificationTitle)
            putExtra("isGroup", isGroup)

            if (!isGroup) {
                putExtra("friendUserId", senderId)
                putExtra("friendDisplayName", senderName)
                putExtra("friendAvatarId", friendAvatarId)
            }
        }

        val notificationId = conversationId.hashCode().absoluteValue

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                notificationId,
                notification,
            )
        } catch (_: SecurityException) {
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Chat messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for new chat messages"
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "chat_messages"
    }
}