package com.ekhonavigator.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
This is the home page that is a crucial first impression to the user
It is incredibly important to have clean and easy to read critical items
Such as upcoming classes and events
This is a feature that the current ci app sorely lacks

@TODO Create repositories for all relevant data, must be in core!!
@TODO Create ViewModel for this screen in this folder
@TODO Replace with actual screen
*/
@Composable
fun HomeScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DummyHomeContent(
        onEventClick = onEventClick,
        modifier = modifier,
    )
}
