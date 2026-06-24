package at.aau.se2.skyjo.ui.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.FriendRequestDto
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.RelationshipStatus
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.haptic.LocalHaptic
import at.aau.se2.skyjo.ui.components.AvatarBadge
import at.aau.se2.skyjo.ui.components.SkyjoCard
import at.aau.se2.skyjo.ui.components.SkyjoDrawerScaffold
import at.aau.se2.skyjo.ui.components.screenHorizontalPadding
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.OnlineGreen
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme

@Composable
fun FriendsScreen(
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    friends: List<FriendDto> = emptyList(),
    incomingRequests: List<FriendRequestDto> = emptyList(),
    lobbyInvites: List<LobbyInviteDto> = emptyList(),
    searchResults: List<SocialUserDto> = emptyList(),
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onSendRequest: (String) -> Unit = {},
    onAcceptRequest: (String) -> Unit = {},
    onDeclineRequest: (String) -> Unit = {},
    onInviteFriend: (String) -> Unit = {},
    onAcceptInvite: (String) -> Unit = {},
    onDeclineInvite: (String) -> Unit = {},
) {
    SkyjoDrawerScaffold(
        currentDestination = AppDestination.Friends,
        onNavigate = onNavigate,
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = screenHorizontalPadding(), vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("Friends", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search users") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (searchResults.isNotEmpty()) {
                    SectionCard(title = "Search") {
                        searchResults.forEach { user ->
                            SearchRow(user = user, onSendRequest = onSendRequest)
                        }
                    }
                }

                if (incomingRequests.isNotEmpty()) {
                    SectionCard(title = "Requests") {
                        incomingRequests.forEach { request ->
                            RequestRow(request, onAcceptRequest, onDeclineRequest)
                        }
                    }
                }

                if (lobbyInvites.isNotEmpty()) {
                    SectionCard(title = "Lobby Invites") {
                        lobbyInvites.forEach { invite ->
                            InviteRow(invite, onAcceptInvite, onDeclineInvite)
                        }
                    }
                }

                SectionCard(title = "Your Friends") {
                    if (friends.isEmpty()) {
                        Text("No friends yet", color = MutedText)
                    } else {
                        friends.forEach { friend ->
                            FriendRow(friend, showInvite = friend.online, onInviteFriend = onInviteFriend)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    SkyjoCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun FriendRow(friend: FriendDto, showInvite: Boolean, onInviteFriend: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AvatarBadge(
            initial = friend.username.firstOrNull() ?: '?',
            size = 44,
            showOnlineIndicator = friend.online,
            backgroundColor = MintGreen,
            textColor = PrimaryGreen,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                friend.username,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(if (friend.online) "Online" else "Offline", color = if (friend.online) OnlineGreen else MutedText)
        }
        if (showInvite) {
            ActionPill("Invite") { onInviteFriend(friend.userId) }
        }
    }
}

@Composable
private fun SearchRow(user: SocialUserDto, onSendRequest: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            user.username,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        when (user.relationshipStatus) {
            RelationshipStatus.NONE -> ActionPill("Add") { onSendRequest(user.userId) }
            RelationshipStatus.FRIENDS -> Text("Friend", color = MutedText)
            RelationshipStatus.INCOMING_REQUEST -> Text("Request pending", color = MutedText)
            RelationshipStatus.OUTGOING_REQUEST -> Text("Sent", color = MutedText)
        }
    }
}

@Composable
private fun RequestRow(
    request: FriendRequestDto,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            request.from.username,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ActionPill("Accept") { onAcceptRequest(request.requestId) }
        Spacer(modifier = Modifier.width(6.dp))
        ActionPill("Decline") { onDeclineRequest(request.requestId) }
    }
}

@Composable
private fun InviteRow(
    invite: LobbyInviteDto,
    onAcceptInvite: (String) -> Unit,
    onDeclineInvite: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                invite.from.username,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Code ${invite.joinCode}", color = MutedText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        ActionPill("Join") { onAcceptInvite(invite.inviteId) }
        Spacer(modifier = Modifier.width(6.dp))
        ActionPill("Decline") { onDeclineInvite(invite.inviteId) }
    }
}

@Composable
private fun ActionPill(text: String, onClick: () -> Unit) {
    val haptic = LocalHaptic.current
    Surface(
        onClick = {
            haptic?.tick()
            onClick()
        },
        shape = MaterialTheme.shapes.extraLarge,
        color = PrimaryGreen,
    ) {
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FriendsScreenPreview() {
    SkyjoTheme {
        FriendsScreen(onNavigate = {})
    }
}
