package com.ekhonavigator.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkhoTopAppBar(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    secondaryActionIcon: ImageVector? = null,
    secondaryActionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    leadingActions: @Composable RowScope.() -> Unit = {},
) {
    EkhoTopAppBar(
        title = stringResource(id = titleRes),
        modifier = modifier,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationIconContentDescription,
        actionIcon = actionIcon,
        actionIconContentDescription = actionIconContentDescription,
        secondaryActionIcon = secondaryActionIcon,
        secondaryActionIconContentDescription = secondaryActionIconContentDescription,
        colors = colors,
        onNavigationClick = onNavigationClick,
        onActionClick = onActionClick,
        onSecondaryActionClick = onSecondaryActionClick,
        scrollBehavior = scrollBehavior,
        leadingActions = leadingActions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkhoTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    secondaryActionIcon: ImageVector? = null,
    secondaryActionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    leadingActions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = navigationIconContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            leadingActions()
            if (actionIcon != null) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionIconContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        colors = colors,
        modifier = modifier.testTag("ekhoTopAppBar"),
        scrollBehavior = scrollBehavior,
    )
}
