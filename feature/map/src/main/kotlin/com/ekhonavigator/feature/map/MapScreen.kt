package com.ekhonavigator.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

/*
This will be the main map view with nearby buildings or directions to classes
We could also implement the onEventClick to show clickable links to events at
particular locations

@TODO Create repository for this feature in core
@TODO Create ViewModel for this screen in this folder
@TODO Replace with actual screen
*/
@Composable
fun MapScreen(onEventClick: (String) -> Unit,
                   modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Map feature coming soon!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
