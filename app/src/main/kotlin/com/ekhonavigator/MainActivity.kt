package com.ekhonavigator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.profile.ProfileRepository
import com.ekhonavigator.core.data.repository.PresenceRepository
import com.ekhonavigator.core.designsystem.theme.EkhoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class NotificationChatRequest(
    val conversationId: String? = null,
    val friendUserId: String = "",
    val friendDisplayName: String = "",
    val friendAvatarId: String = "",
    val chatTitle: String = "",
    val isGroup: Boolean = false,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var presenceRepository: PresenceRepository

    private var notificationChatRequest by mutableStateOf<NotificationChatRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)

        enableEdgeToEdge()

        MainScope().launch {
            authRepository.userFlow().collectLatest { uid ->
                if (uid != null) {
                    val profile = profileRepository.getProfile(uid)
                    val showOnlineStatus = profile?.showOnlineStatus ?: true
                    presenceRepository.startPresence(uid, showOnlineStatus)
                } else {
                    presenceRepository.stopPresence()
                }
            }
        }

        setContent {
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { }

            LaunchedEffect(Unit) {
                val needsNotificationPermission =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED

                if (needsNotificationPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            EkhoTheme {
                AssignmentDecoratorProvider {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        EkhoNavigatorApp(
                            notificationChatRequest = notificationChatRequest,
                            onNotificationChatRequestHandled = {
                                notificationChatRequest = null
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("openChat", false) != true) return

        val conversationId = intent.getStringExtra("conversationId").orEmpty()
        val isGroup = intent.getBooleanExtra("isGroup", false)

        val friendUserId = intent.getStringExtra("friendUserId").orEmpty()
        val friendDisplayName = intent.getStringExtra("friendDisplayName").orEmpty()
        val friendAvatarId = intent.getStringExtra("friendAvatarId").orEmpty()

        val chatTitle = intent.getStringExtra("chatTitle").orEmpty()

        android.util.Log.d(
            "NOTIFICATION_NAV",
            "openChat=true, " +
                    "conversationId=$conversationId, " +
                    "isGroup=$isGroup, " +
                    "friendUserId=$friendUserId, " +
                    "friendDisplayName=$friendDisplayName, " +
                    "chatTitle=$chatTitle",
        )

        notificationChatRequest = if (isGroup) {
            if (conversationId.isBlank()) return

            NotificationChatRequest(
                conversationId = conversationId,
                chatTitle = chatTitle.ifBlank { "Group Chat" },
                isGroup = true,
            )
        } else {
            if (conversationId.isBlank() && friendUserId.isBlank()) return

            NotificationChatRequest(
                conversationId = conversationId.ifBlank { null },
                friendUserId = friendUserId,
                friendDisplayName = friendDisplayName.ifBlank { chatTitle.ifBlank { "Chat" } },
                friendAvatarId = friendAvatarId,
                chatTitle = chatTitle.ifBlank { friendDisplayName.ifBlank { "Chat" } },
                isGroup = false,
            )
        }
    }
}