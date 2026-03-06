package com.ekhonavigator.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

/*
Main user account area. This will show login/sign-out and other optional user info

@TODO Create repository for this feature in core
@TODO Create ViewModel for this screen in this folder
@TODO Replace with actual screen
*/

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "User account coming soon!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }

}