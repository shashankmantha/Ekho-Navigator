package com.ekhonavigator.feature.social

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

/*
This will be the main social view with user friends
will integrate some kind of event click callback here

@TODO Create repository for this feature in core
@TODO Create ViewModel for this screen in this folder
@TODO Replace with actual screen
*/
@Composable
fun SocialScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Social feature coming soon!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
