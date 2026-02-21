package com.ekhonavigator.feature.events

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

/*
This is a screen of the campus events in a list format
as opposed to the calendar view in CalendarScreen

@TODO Create repository for this feature in core
@TODO Create ViewModel for this screen in this folder
@TODO Replace with actual screen
*/
@Composable
fun EventsScreen(onEventClick: (String) -> Unit,
                   modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Events feature coming soon!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
