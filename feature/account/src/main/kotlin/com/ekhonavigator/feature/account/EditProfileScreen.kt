package com.ekhonavigator.feature.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ekhonavigator.core.designsystem.R as DesignSystemR

private val avatarOptions = listOf(
    "avatar_default",
    "avatar_dolphin",
    "avatar_whale",
    "avatar_turtle",
)

@Composable
fun EditProfileScreen(
    userEmail: String,
    initialDisplayName: String,
    initialMajor: String,
    initialDescription: String,
    initialLinks: String,
    initialMajorVisible: Boolean,
    initialDescriptionVisible: Boolean,
    initialLinksVisible: Boolean,
    avatarId: String,
    onSaveClick: (
        String, String, String, String, Boolean, Boolean, Boolean, String
    ) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by rememberSaveable { mutableStateOf(initialDisplayName) }
    var major by rememberSaveable { mutableStateOf(initialMajor) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var links by rememberSaveable { mutableStateOf(initialLinks) }

    var majorVisible by rememberSaveable { mutableStateOf(initialMajorVisible) }
    var descriptionVisible by rememberSaveable { mutableStateOf(initialDescriptionVisible) }
    var linksVisible by rememberSaveable { mutableStateOf(initialLinksVisible) }

    var selectedAvatarId by rememberSaveable { mutableStateOf(avatarId) }
    var showAvatarDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileHeader(
            email = userEmail,
            avatarId = selectedAvatarId,
            onAvatarClick = { showAvatarDialog = true },
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfileFieldCard(label = stringResource(R.string.account_profile_display_name)) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileFieldCard(
            label = stringResource(R.string.account_profile_major),
            visible = majorVisible,
            onVisibleChange = { majorVisible = it },
        ) {
            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileFieldCard(
            label = stringResource(R.string.account_profile_description),
            visible = descriptionVisible,
            onVisibleChange = { descriptionVisible = it },
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(14.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileFieldCard(
            label = stringResource(R.string.account_profile_links),
            visible = linksVisible,
            onVisibleChange = { linksVisible = it },
        ) {
            OutlinedTextField(
                value = links,
                onValueChange = { links = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                onSaveClick(
                    displayName,
                    major,
                    description,
                    links,
                    majorVisible,
                    descriptionVisible,
                    linksVisible,
                    selectedAvatarId,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.account_profile_save),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.account_sign_out),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text(stringResource(R.string.account_close))
                }
            },
            title = {
                Text(stringResource(R.string.account_choose_avatar))
            },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    avatarOptions.forEach { option ->
                        val avatarRes = avatarIdToRes(option)

                        Image(
                            painter = painterResource(id = avatarRes),
                            contentDescription = option,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable {
                                    selectedAvatarId = option
                                    showAvatarDialog = false
                                },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun ProfileHeader(
    email: String,
    avatarId: String,
    onAvatarClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
        ) {
            Image(
                painter = painterResource(id = avatarIdToRes(avatarId)),
                contentDescription = stringResource(R.string.account_profile_avatar_content_description),
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() },
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.account_edit_profile_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun avatarIdToRes(avatarId: String): Int {
    return when (avatarId) {
        "avatar_dolphin" -> DesignSystemR.drawable.avatar_dolphin
        "avatar_whale" -> DesignSystemR.drawable.avatar_whale
        "avatar_turtle" -> DesignSystemR.drawable.avatar_turtle
        else -> DesignSystemR.drawable.avatar_default
    }
}

@Composable
private fun ProfileFieldCard(
    label: String,
    visible: Boolean? = null,
    onVisibleChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                if (visible != null && onVisibleChange != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.account_profile_visible),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Switch(
                            checked = visible,
                            onCheckedChange = onVisibleChange,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
