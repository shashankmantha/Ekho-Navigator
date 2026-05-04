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
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.repository.PresenceRepository
import com.ekhonavigator.core.designsystem.theme.EkhoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class NotificationChatRequest(
    val friendUserId: String,
    val friendDisplayName: String,
    val friendAvatarId: String,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var presenceRepository: PresenceRepository

    @Inject
    lateinit var customEventRepository: CustomEventRepository

    @Inject
    lateinit var calendarRepository: CalendarRepository

    private var notificationChatRequest by mutableStateOf<NotificationChatRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)

        enableEdgeToEdge()

        // Track presence globally while the user is signed in
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    EkhoNavigatorApp(
                        onSignIn = {
                            customEventRepository.startSync(MainScope())
                            MainScope().launch {
                                calendarRepository.restoreBookmarks()
                            }
                        },
                        onSignOut = {
                            MainScope().launch {
                                customEventRepository.onSignOut()
                                calendarRepository.onSignOut()
                            }
                        },
                        notificationChatRequest = notificationChatRequest,
                        onNotificationChatRequestHandled = {
                            notificationChatRequest = null
                        },
                    )
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
        android.util.Log.d(
            "NOTIFICATION_NAV",
            "openChat=${intent?.getBooleanExtra("openChat", false)}, " +
                    "friendUserId=${intent?.getStringExtra("friendUserId")}, " +
                    "friendDisplayName=${intent?.getStringExtra("friendDisplayName")}",
        )

        if (intent?.getBooleanExtra("openChat", false) != true) return

        val friendUserId = intent.getStringExtra("friendUserId") ?: return
        val friendDisplayName = intent.getStringExtra("friendDisplayName") ?: "Chat"
        val friendAvatarId = intent.getStringExtra("friendAvatarId") ?: ""

        notificationChatRequest = NotificationChatRequest(
            friendUserId = friendUserId,
            friendDisplayName = friendDisplayName,
            friendAvatarId = friendAvatarId,
        )
    }
}