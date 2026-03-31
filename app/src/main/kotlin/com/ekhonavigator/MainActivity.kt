package com.ekhonavigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.designsystem.theme.EkhoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var customEventRepository: CustomEventRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EkhoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    EkhoNavigatorApp(
                        onSignIn = {
                            customEventRepository.startSync(MainScope())
                        },
                        onSignOut = {
                            MainScope().launch {
                                customEventRepository.onSignOut()
                            }
                        },
                    )
                }
            }
        }
    }
}
