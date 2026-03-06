package com.ekhonavigator.feature.event

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

/*
This is a single event only, not to be confused with the EventsScreen

The back arrow will return the user to their previous page

A big decision to make is whether or not we merge campus events with user events/meetings
That could help simplify the codebase if its all the same base ui component
We could make a factory to search for the id in the events and user meetings repositories

@TODO Create repository for this feature in core
@TODO Create ViewModel for this screen in this folder
@TODO Replace with actual screen
*/
@Composable
fun EventScreen(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Event coming soon!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
