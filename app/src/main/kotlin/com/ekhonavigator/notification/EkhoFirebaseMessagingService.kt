package com.ekhonavigator.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ekhonavigator.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EkhoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenForCurrentUser(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val conversationId = message.data["conversationId"].orEmpty()
        val isGroup = message.data["isGroup"] == "true"

        val senderName = message.data["senderName"]
            ?: message.data["title"]
            ?: "Someone"

        val messageText = message.data["body"]
            ?: message.data["messageText"]
            ?: "You received a new message"

        val rawChatTitle = message.data["chatTitle"]
            ?: message.data["groupTitle"]
            ?: ""

        val groupFallbackTitle = message.data["groupFallbackTitle"]
            ?: message.data["participantNames"]
            ?: ""

        val chatTitle = when {
            rawChatTitle.isNotBlank() -> rawChatTitle
            isGroup && groupFallbackTitle.isNotBlank() -> groupFallbackTitle
            isGroup -> "Group Chat"
            else -> message.data["title"] ?: "New message"
        }

        if (isGroup) {
            showGroupChatNotification(
                conversationId = conversationId,
                chatTitle = chatTitle.ifBlank { "Group Chat" },
                senderName = senderName,
                messageText = messageText,
            )
        } else {
            showDirectChatNotification(
                conversationId = conversationId,
                senderName = senderName,
                messageText = messageText,
            )
        }
    }

    private fun saveTokenForCurrentUser(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val tokenData = mapOf(
            "token" to token,
            "platform" to "android",
            "updatedAt" to System.currentTimeMillis(),
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("fcmTokens")
            .document(token)
            .set(tokenData)
    }

    private fun showDirectChatNotification(
        conversationId: String,
        senderName: String,
        messageText: String,
    ) {
        if (!canPostNotifications()) return

        createChatNotificationChannel()

        val intent = buildChatIntent(
            conversationId = conversationId,
            chatTitle = senderName,
            isGroup = false,
        )

        val pendingIntent = buildPendingIntent(intent)

        val notification = NotificationCompat.Builder(this, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(messageText),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        postNotification(notification)
    }

    private fun showGroupChatNotification(
        conversationId: String,
        chatTitle: String,
        senderName: String,
        messageText: String,
    ) {
        if (!canPostNotifications()) return

        createChatNotificationChannel()

        val intent = buildChatIntent(
            conversationId = conversationId,
            chatTitle = chatTitle,
            isGroup = true,
        )

        val pendingIntent = buildPendingIntent(intent)

        val notification = NotificationCompat.Builder(this, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(chatTitle)
            .setContentText("$senderName: $messageText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$senderName\n$messageText"),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        postNotification(notification)
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        notification: android.app.Notification,
    ) {
        if (!canPostNotifications()) return

        try {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification,
            )
        } catch (_: SecurityException) {
        }
    }

    private fun buildChatIntent(
        conversationId: String,
        chatTitle: String,
        isGroup: Boolean,
    ): Intent {
        return Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openChat", true)
            putExtra("conversationId", conversationId)
            putExtra("chatTitle", chatTitle)
            putExtra("isGroup", isGroup)
        }
    }

    private fun buildPendingIntent(
        intent: Intent,
    ): PendingIntent {
        return PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChatNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHAT_CHANNEL_ID,
            "Chat messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for new chat messages"
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHAT_CHANNEL_ID = "chat_messages"
    }
}