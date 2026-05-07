package com.ekhonavigator.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun FriendPickerCard(
    markerLabel: String,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    friends: List<FriendInfo>,
    onFriendSelected: (FriendInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Send marker to friend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = markerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    label = { Text("Search friends") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Type name...") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FriendListSection(
                    friends = friends,
                    onFriendSelected = onFriendSelected
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun FriendListSection(
    friends: List<FriendInfo>,
    onFriendSelected: (FriendInfo) -> Unit
) {
    val listState = rememberLazyListState()

    if (friends.isEmpty()) {
        Text(
            text = "No friends found",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .heightIn(max = 280.dp) // resets friendslist on "sendmarkertofriend" card to 280dp for the 4-5 friend limit
                .drawWithContent {
                    drawContent()

                    val totalItems = listState.layoutInfo.totalItemsCount
                    val visibleItems = listState.layoutInfo.visibleItemsInfo.size

                    if (totalItems > visibleItems) {
                        val viewportHeight = size.height
                        val totalContentHeight = totalItems * 64.dp.toPx()

                        val scrollbarHeight = (viewportHeight / totalContentHeight) * viewportHeight
                        val currentScroll =
                            (listState.firstVisibleItemIndex * 64.dp.toPx()) + listState.firstVisibleItemScrollOffset
                        val scrollbarTop = (currentScroll / totalContentHeight) * viewportHeight

                        drawRect(
                            color = Color.Gray.copy(alpha = 0.5f),
                            topLeft = Offset(x = size.width - 4.dp.toPx(), y = scrollbarTop),
                            size = Size(width = 4.dp.toPx(), height = scrollbarHeight)
                        )
                    }
                }
        ) {
            items(friends) { friend ->
                ListItem(
                    headlineContent = { Text(friend.name) },
                    modifier = Modifier.clickable { onFriendSelected(friend) }
                )
            }
        }
    }
}

data class FriendInfo(val id: String, val name: String)