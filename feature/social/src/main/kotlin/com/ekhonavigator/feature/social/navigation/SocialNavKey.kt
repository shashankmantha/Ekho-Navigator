package com.ekhonavigator.feature.social.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ekhonavigator.core.model.SharedLocation
import com.ekhonavigator.core.navigation.Navigator
import com.ekhonavigator.feature.social.ChatOptionsScreen
import com.ekhonavigator.feature.social.ChatScreen
import com.ekhonavigator.feature.social.NewChatScreen
import com.ekhonavigator.feature.social.SocialScreen
import com.ekhonavigator.feature.social.UserProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object NewChatNavKey : NavKey

@Serializable
object SocialNavKey : NavKey

@Serializable
data class UserProfileNavKey(
    val userId: String,
) : NavKey

@Serializable
data class ChatOptionsNavKey(
    val conversationId: String,
) : NavKey

@Serializable
data class ChatNavKey(
    val conversationId: String? = null,
    val friendUserId: String = "",
    val friendDisplayName: String = "",
    val friendAvatarId: String = "",
    val chatTitle: String = "",
    val isGroup: Boolean = false,
    val groupParticipantNames: Map<String, String> = emptyMap(),
    val groupParticipantAvatarIds: Map<String, String> = emptyMap(),
    val sharedLocation: SharedLocation? = null,
) : NavKey

fun EntryProviderScope<NavKey>.socialEntry(
    navigator: Navigator,
    onNavigateToMap: () -> Unit,
) {
    entry<SocialNavKey> {
        SocialScreen(
            onProfileClick = { userId ->
                navigator.navigate(
                    UserProfileNavKey(
                        userId = userId,
                    ),
                )
            },
            onMessageClick = { friendUserId, friendDisplayName, friendAvatarId ->
                navigator.navigate(
                    ChatNavKey(
                        conversationId = null,
                        friendUserId = friendUserId,
                        friendDisplayName = friendDisplayName,
                        friendAvatarId = friendAvatarId,
                        chatTitle = friendDisplayName,
                        isGroup = false,
                    ),
                )
            },
            onConversationClick = { conversation ->
                navigator.navigate(
                    ChatNavKey(
                        conversationId = conversation.conversationId,
                        friendUserId = conversation.directFriendUserId,
                        friendDisplayName = conversation.directFriendDisplayName,
                        friendAvatarId = conversation.directFriendAvatarId,
                        chatTitle = conversation.title,
                        isGroup = conversation.isGroup,
                        groupParticipantNames = conversation.participantNames,
                        groupParticipantAvatarIds = conversation.groupParticipantAvatarIds,
                    ),
                )
            },
            onChatOptionsClick = { conversationId ->
                navigator.navigate(
                    ChatOptionsNavKey(
                        conversationId = conversationId,
                    ),
                )
            },
            onNewChatClick = {
                navigator.navigate(NewChatNavKey)
            },
        )
    }

    entry<UserProfileNavKey> { key ->
        UserProfileScreen(
            userId = key.userId,
            onBack = {
                navigator.goBack()
            },
        )
    }

    entry<ChatOptionsNavKey> { key ->
        ChatOptionsScreen(
            conversationId = key.conversationId,
            onBack = {
                navigator.goBack()
            },
            onParticipantClick = { userId ->
                navigator.navigate(
                    UserProfileNavKey(
                        userId = userId,
                    ),
                )
            },
            onLeaveConversation = {
                navigator.goBack()
                navigator.goBack()
            },
        )
    }

    entry<ChatNavKey> { key ->
        ChatScreen(
            conversationId = key.conversationId,
            friendUserId = key.friendUserId,
            friendDisplayName = key.friendDisplayName,
            friendAvatarId = key.friendAvatarId,
            chatTitle = key.chatTitle,
            isGroup = key.isGroup,
            groupParticipantNames = key.groupParticipantNames,
            groupParticipantAvatarIds = key.groupParticipantAvatarIds,
            sharedLocation = key.sharedLocation,
            onNavigateToMap = onNavigateToMap,
            onOpenChatOptions = { conversationId ->
                navigator.navigate(
                    ChatOptionsNavKey(
                        conversationId = conversationId,
                    ),
                )
            },
        )
    }

    entry<NewChatNavKey> {
        NewChatScreen(
            onDirectChatSelected = { friend ->
                navigator.goBack()

                navigator.navigate(
                    ChatNavKey(
                        conversationId = null,
                        friendUserId = friend.uid,
                        friendDisplayName = friend.displayName,
                        friendAvatarId = friend.avatarId,
                        chatTitle = friend.displayName,
                        isGroup = false,
                    ),
                )
            },
            onGroupDraftCreated = { groupTitle, selectedFriends ->
                navigator.goBack()

                navigator.navigate(
                    ChatNavKey(
                        conversationId = null,
                        friendUserId = "",
                        friendDisplayName = "",
                        friendAvatarId = "",
                        chatTitle = groupTitle.trim(),
                        isGroup = true,
                        groupParticipantNames = selectedFriends.associate { friend ->
                            friend.uid to friend.displayName
                        },
                        groupParticipantAvatarIds = selectedFriends.associate { friend ->
                            friend.uid to friend.avatarId
                        },
                    ),
                )
            },
        )
    }
}