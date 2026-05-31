package at.aau.se2.skyjo.ui.screens.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.model.stats.PlayerStatsDto
import at.aau.se2.skyjo.ui.components.AvatarBadge
import at.aau.se2.skyjo.ui.components.PrimaryButton
import at.aau.se2.skyjo.ui.components.SkyjoCard
import at.aau.se2.skyjo.ui.components.SkyjoDrawerScaffold
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme

@Composable
fun StartScreen(
    onPlayClicked: (playerName: String) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    username: String = "",
    stats: PlayerStatsDto? = null,
    onCreateLobby: () -> Unit = { if (username.isNotBlank()) onPlayClicked(username) },
    onJoinLobby: (String) -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var joinCode by remember { mutableStateOf("") }
    val displayName = username.ifBlank { "Player" }

    SkyjoDrawerScaffold(
        currentDestination = AppDestination.Start,
        onNavigate = onNavigate,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SkyjoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarBadge(
                        initial = displayName.first().uppercaseChar(),
                        size = 52,
                        showOnlineIndicator = true,
                        backgroundColor = MintGreen,
                        textColor = PrimaryGreen,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Ready for the next lobby",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                        )
                    }
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    StatItem(value = (stats?.wins ?: 0).toString(), label = "Wins")
                    VerticalDivider()
                    StatItem(value = (stats?.gamesPlayed ?: 0).toString(), label = "Games")
                    VerticalDivider()
                    StatItem(value = "%.1f".format(stats?.averageScore ?: 0.0), label = "Avg Score")
                }
            }

            SkyjoCard {
                Text(
                    text = "Lobby",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(
                    text = "CREATE LOBBY",
                    onClick = onCreateLobby,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase().take(6) },
                    label = { Text("Join Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                PrimaryButton(
                    text = "JOIN WITH CODE",
                    onClick = { onJoinLobby(joinCode) },
                    enabled = joinCode.isNotBlank(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeActionCard(
                    icon = Icons.Default.Group,
                    title = "Friends",
                    onClick = { onNavigate(AppDestination.Friends) },
                    modifier = Modifier.weight(1f),
                )
                HomeActionCard(
                    icon = Icons.Default.EmojiEvents,
                    title = "Leaderboard",
                    onClick = { onNavigate(AppDestination.Leaderboard) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(icon, contentDescription = title, tint = PrimaryGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryGreen,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VerticalDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color(0xFFE5E7EB))
            .padding(horizontal = 0.dp),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StartScreenPreview() {
    SkyjoTheme {
        StartScreen(
            onPlayClicked = {},
            onNavigate = {},
            username = "Alice",
            stats = PlayerStatsDto("user-a", "Alice", 0, 0, 0, null, 0.0),
        )
    }
}
