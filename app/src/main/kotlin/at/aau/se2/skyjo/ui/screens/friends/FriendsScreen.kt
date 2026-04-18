package at.aau.se2.skyjo.ui.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.ui.components.AvatarBadge
import at.aau.se2.skyjo.ui.components.BadgeChip
import at.aau.se2.skyjo.ui.components.SkyjoCard
import at.aau.se2.skyjo.ui.components.SkyjoDrawerScaffold
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.GoldSurface
import at.aau.se2.skyjo.ui.theme.GoldYellow
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.OnlineGreen
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme

private data class Friend(
    val name: String,
    val status: String,
    val isOnline: Boolean,
    val level: Int,
)

private val onlineFriends = listOf(
    Friend("KiraBlaze", "In Lobby", isOnline = true, level = 38),
    Friend("MaxStorm", "Playing", isOnline = true, level = 21),
    Friend("RiverDawn", "Available", isOnline = true, level = 55),
)

private val suggestedFriends = listOf(
    Friend("TideWatcher", "Level 14", isOnline = false, level = 14),
    Friend("SkyRunner", "Level 29", isOnline = false, level = 29),
)

@Composable
fun FriendsScreen(
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkyjoDrawerScaffold(
        currentDestination = AppDestination.Friends,
        onNavigate = onNavigate,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Title
            Text(
                text = "Friends",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Search bar
            var query by remember { mutableStateOf("") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search players…", color = MutedText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MutedText,
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = PrimaryGreen,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Online friends
            SectionHeader(
                title = "Online Friends",
                count = onlineFriends.size,
                indicatorColor = OnlineGreen,
            )
            SkyjoCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    onlineFriends.forEachIndexed { index, friend ->
                        FriendRow(friend = friend, showInvite = true)
                        if (index < onlineFriends.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            // Suggested friends
            SectionHeader(title = "Suggested Friends", count = null)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                suggestedFriends.forEach { friend ->
                    SuggestedFriendCard(friend = friend, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int?,
    indicatorColor: androidx.compose.ui.graphics.Color? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indicatorColor != null) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = indicatorColor,
                modifier = Modifier.size(8.dp),
            ) {}
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = if (count != null) "$title ($count)" else title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun FriendRow(friend: Friend, showInvite: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(
            initial = friend.name.first(),
            size = 44,
            showOnlineIndicator = friend.isOnline,
            backgroundColor = MintGreen,
            textColor = PrimaryGreen,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = friend.status,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
            )
        }
        if (showInvite) {
            Surface(
                onClick = {},
                shape = MaterialTheme.shapes.extraLarge,
                color = PrimaryGreen,
            ) {
                Text(
                    text = "Invite",
                    style = MaterialTheme.typography.labelMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun SuggestedFriendCard(friend: Friend, modifier: Modifier = Modifier) {
    androidx.compose.material3.Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarBadge(
                initial = friend.name.first(),
                size = 48,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = PrimaryGreen,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = friend.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Lv. ${friend.level}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = {},
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "+ Add",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FriendsScreenPreview() {
    SkyjoTheme {
        FriendsScreen(onNavigate = {})
    }
}
