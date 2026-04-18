package com.ekhonavigator.feature.event

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.designsystem.icon.EkhoIcons

@Composable
fun InvitesActionIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvitesActionViewModel = hiltViewModel(),
) {
    val count by viewModel.pendingCount.collectAsStateWithLifecycle()

    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ) {
                        Text(if (count > 9) "9+" else count.toString())
                    }
                }
            },
        ) {
            Icon(
                imageVector = EkhoIcons.Notifications,
                contentDescription = "Invites",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
