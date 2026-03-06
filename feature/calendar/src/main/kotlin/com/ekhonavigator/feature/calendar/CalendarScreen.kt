package com.ekhonavigator.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

/*
This will be the main calendar view with user assignable schedule info
and clickable CUSCI events. These events open up an EventScreen with a back arrow

@TODO Create repository for this feature in core
@TODO Create ViewModel for this screen in this folder
@TODO Replace with actual screen
*/
@Composable
fun CalendarScreen(onEventClick: (String) -> Unit,
                   modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Calendar feature coming soon!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
